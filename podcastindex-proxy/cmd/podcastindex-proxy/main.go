package main

import (
	"log/slog"
	"net/http"
	"os"
	"time"

	"github.com/mediathekview/podcastindex-proxy/internal/api"
	"github.com/mediathekview/podcastindex-proxy/internal/config"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
	"github.com/mediathekview/podcastindex-proxy/internal/search"
)

func main() {
	logger := slog.New(slog.NewTextHandler(os.Stdout, &slog.HandlerOptions{
		Level: slog.LevelDebug,
	}))
	slog.SetDefault(logger)

	cfg, err := config.FromEnvironment()
	if err != nil {
		logger.Error("failed to load configuration", "error", err)
		os.Exit(1)
	}

	server := newServer(cfg, api.NewHandler(
		search.NewService(
			cfg,
			podcastindex.NewClient(cfg, &http.Client{Timeout: 10 * time.Second}),
		),
		os.Stdout,
	))

	logger.Info(
		"Podcastindex-Proxy lauscht",
		"address", "http://"+cfg.ListenAddress(),
		"userAgent", cfg.UserAgent,
		"feedLimit", cfg.DefaultFeedLimit,
		"episodeLimit", cfg.DefaultEpisodeLimit,
		"configFile", cfg.ConfigFile,
	)
	if err := server.ListenAndServe(); err != nil && err != http.ErrServerClosed {
		logger.Error("server stopped", "error", err)
		os.Exit(1)
	}
}

func newServer(cfg config.Config, handler http.Handler) *http.Server {
	return &http.Server{
		Addr:              cfg.ListenAddress(),
		Handler:           handler,
		ReadHeaderTimeout: 5 * time.Second,
	}
}
