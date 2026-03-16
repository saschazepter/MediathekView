package api

import (
	"encoding/json"
	"log/slog"
	"net/http"

	"github.com/mediathekview/podcastindex-proxy/internal/model"
)

func writeError(responseWriter http.ResponseWriter, statusCode int, message string) {
	writeJSON(responseWriter, statusCode, model.ErrorResponse{Error: message})
}

func writeJSON(responseWriter http.ResponseWriter, statusCode int, payload any) {
	responseWriter.Header().Set("Content-Type", "application/json; charset=utf-8")
	responseWriter.WriteHeader(statusCode)
	if err := json.NewEncoder(responseWriter).Encode(payload); err != nil {
		slog.Warn("failed to write json response", "error", err)
	}
}
