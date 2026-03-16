package main

import (
	"log"
	"net/http"
	"os"
	"time"

	"github.com/mediathekview/podcastindex-proxy/internal/api"
	"github.com/mediathekview/podcastindex-proxy/internal/config"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
	"github.com/mediathekview/podcastindex-proxy/internal/search"
)

func main() {
	cfg, err := config.FromEnvironment()
	if err != nil {
		log.Fatal(err)
	}

	httpClient := &http.Client{Timeout: 10 * time.Second}
	podcastClient := podcastindex.NewClient(cfg, httpClient)
	searchService := search.NewService(cfg, podcastClient)
	handler := api.NewHandler(searchService, os.Stdout)

	log.Printf("Podcastindex-Proxy lauscht auf http://%s:%d", cfg.Host, cfg.Port)
	log.Fatal(http.ListenAndServe(cfg.ListenAddress(), handler))
}
