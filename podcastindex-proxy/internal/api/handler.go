package api

import (
	"context"
	"errors"
	"io"
	"log/slog"
	"net/http"
	"strings"

	"github.com/mediathekview/podcastindex-proxy/internal/model"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
	"github.com/mediathekview/podcastindex-proxy/internal/search"
)

type searchService interface {
	Search(ctx context.Context, query string, options search.Options) ([]model.AudioEntry, error)
}

type Handler struct {
	mux     *http.ServeMux
	service searchService
}

func NewHandler(service searchService, _ io.Writer) *Handler {
	handler := &Handler{
		mux:     http.NewServeMux(),
		service: service,
	}

	handler.registerRoutes()
	return handler
}

func (h *Handler) ServeHTTP(responseWriter http.ResponseWriter, request *http.Request) {
	h.mux.ServeHTTP(responseWriter, request)
}

func (h *Handler) registerRoutes() {
	h.mux.HandleFunc("GET /health", h.handleHealth)
	h.mux.HandleFunc("GET /api/audiothek/podcast-search", h.handlePodcastSearch)
}

func (h *Handler) handleHealth(responseWriter http.ResponseWriter, _ *http.Request) {
	writeJSON(responseWriter, http.StatusOK, model.HealthResponse{Status: "ok"})
}

func (h *Handler) handlePodcastSearch(responseWriter http.ResponseWriter, request *http.Request) {
	query := strings.TrimSpace(request.URL.Query().Get("q"))
	if query == "" {
		writeError(responseWriter, http.StatusBadRequest, "Query-Parameter 'q' fehlt")
		return
	}

	options, err := parseSearchOptions(request)
	if err != nil {
		writeError(responseWriter, http.StatusBadRequest, err.Error())
		return
	}

	slog.Info(
		"podcast search request",
		"query", query,
		"feedLimit", options.FeedLimit,
		"episodeLimit", options.EpisodeLimit,
		"remoteAddr", request.RemoteAddr,
	)

	results, err := h.service.Search(request.Context(), query, options)
	if err != nil {
		h.handleSearchError(responseWriter, query, err)
		return
	}

	slog.Info("podcast search completed", "query", query, "results", len(results))
	writeJSON(responseWriter, http.StatusOK, model.PodcastSearchResponse{Results: results})
}

func (h *Handler) handleSearchError(responseWriter http.ResponseWriter, query string, err error) {
	slog.Warn("podcast search failed", "query", query, "error", err)

	var statusError *podcastindex.StatusError
	if errors.As(err, &statusError) {
		writeError(responseWriter, http.StatusBadGateway, statusError.Message)
		return
	}

	writeError(responseWriter, http.StatusInternalServerError, "Interner Serverfehler")
}
