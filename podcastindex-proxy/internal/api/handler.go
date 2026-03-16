package api

import (
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"log"
	"net/http"
	"strconv"
	"strings"

	"github.com/mediathekview/podcastindex-proxy/internal/model"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
	"github.com/mediathekview/podcastindex-proxy/internal/search"
)

type Handler struct {
	logger  *log.Logger
	mux     *http.ServeMux
	service *search.Service
}

func NewHandler(service *search.Service, logWriter io.Writer) *Handler {
	handler := &Handler{
		logger:  log.New(logWriter, "podcastindex-proxy ", log.LstdFlags),
		mux:     http.NewServeMux(),
		service: service,
	}

	handler.routes()
	return handler
}

func (h *Handler) ServeHTTP(responseWriter http.ResponseWriter, request *http.Request) {
	h.mux.ServeHTTP(responseWriter, request)
}

func (h *Handler) routes() {
	h.mux.HandleFunc("/health", h.handleHealth)
	h.mux.HandleFunc("/api/audiothek/podcast-search", h.handlePodcastSearch)
}

func (h *Handler) handleHealth(responseWriter http.ResponseWriter, request *http.Request) {
	if request.Method != http.MethodGet {
		h.writeError(responseWriter, http.StatusMethodNotAllowed, "Nur GET ist erlaubt")
		return
	}

	h.writeJSON(responseWriter, http.StatusOK, model.HealthResponse{Status: "ok"})
}

func (h *Handler) handlePodcastSearch(responseWriter http.ResponseWriter, request *http.Request) {
	if request.Method != http.MethodGet {
		h.writeError(responseWriter, http.StatusMethodNotAllowed, "Nur GET ist erlaubt")
		return
	}

	query := strings.TrimSpace(request.URL.Query().Get("q"))
	if query == "" {
		h.writeError(responseWriter, http.StatusBadRequest, "Query-Parameter 'q' fehlt")
		return
	}

	feedLimit, err := optionalInt(request, "feedLimit")
	if err != nil {
		h.writeError(responseWriter, http.StatusBadRequest, err.Error())
		return
	}

	episodeLimit, err := optionalInt(request, "episodeLimit")
	if err != nil {
		h.writeError(responseWriter, http.StatusBadRequest, err.Error())
		return
	}

	results, searchErr := h.service.Search(request.Context(), query, feedLimit, episodeLimit)
	if searchErr != nil {
		h.logger.Printf("Suche fehlgeschlagen: %v", searchErr)
		var statusError *podcastindex.StatusError
		if errors.As(searchErr, &statusError) {
			h.writeError(responseWriter, http.StatusBadGateway, statusError.Message)
			return
		}

		h.writeError(responseWriter, http.StatusInternalServerError, "Interner Serverfehler")
		return
	}

	h.writeJSON(responseWriter, http.StatusOK, model.PodcastSearchResponse{Results: results})
}

func optionalInt(request *http.Request, name string) (*int, error) {
	rawValue := strings.TrimSpace(request.URL.Query().Get(name))
	if rawValue == "" {
		return nil, nil
	}

	parsedValue, err := strconv.Atoi(rawValue)
	if err != nil {
		return nil, fmt.Errorf("Query-Parameter %q ist ungültig", name)
	}

	return &parsedValue, nil
}

func (h *Handler) writeError(responseWriter http.ResponseWriter, statusCode int, message string) {
	h.writeJSON(responseWriter, statusCode, model.ErrorResponse{Error: message})
}

func (h *Handler) writeJSON(responseWriter http.ResponseWriter, statusCode int, payload any) {
	responseWriter.Header().Set("Content-Type", "application/json; charset=utf-8")
	responseWriter.WriteHeader(statusCode)
	if err := json.NewEncoder(responseWriter).Encode(payload); err != nil {
		h.logger.Printf("JSON-Antwort konnte nicht geschrieben werden: %v", err)
	}
}
