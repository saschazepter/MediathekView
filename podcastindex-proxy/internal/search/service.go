package search

import (
	"context"

	"github.com/mediathekview/podcastindex-proxy/internal/config"
	"github.com/mediathekview/podcastindex-proxy/internal/model"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
)

type feedClient interface {
	SearchFeeds(ctx context.Context, query string, limit int) ([]podcastindex.Feed, error)
	LoadEpisodes(ctx context.Context, feed podcastindex.Feed, limit int) ([]podcastindex.Episode, error)
}

type Options struct {
	FeedLimit    int
	EpisodeLimit int
}

type normalizedOptions struct {
	feedLimit    int
	episodeLimit int
}

type Service struct {
	config config.Config
	client feedClient
}

func NewService(cfg config.Config, client feedClient) *Service {
	return &Service{config: cfg, client: client}
}

func (service *Service) Search(ctx context.Context, query string, options Options) ([]model.AudioEntry, error) {
	normalized := service.normalizeOptions(options)

	feeds, err := service.client.SearchFeeds(ctx, query, normalized.feedLimit)
	if err != nil {
		return nil, err
	}

	results := make([]model.AudioEntry, 0, normalized.feedLimit*normalized.episodeLimit)
	seenKeys := make(map[string]struct{}, normalized.feedLimit*normalized.episodeLimit)

	for _, feed := range feeds {
		episodes, err := service.client.LoadEpisodes(ctx, feed, normalized.episodeLimit)
		if err != nil {
			return nil, err
		}

		for _, episode := range episodes {
			entry, ok := toAudioEntry(feed, episode)
			if !ok {
				continue
			}

			key := entryKey(entry)
			if _, exists := seenKeys[key]; exists {
				continue
			}

			seenKeys[key] = struct{}{}
			results = append(results, entry)
		}
	}

	return results, nil
}

func (service *Service) normalizeOptions(options Options) normalizedOptions {
	return normalizedOptions{
		feedLimit:    clampOrDefault(options.FeedLimit, service.config.DefaultFeedLimit, 1, 50),
		episodeLimit: clampOrDefault(options.EpisodeLimit, service.config.DefaultEpisodeLimit, 1, 200),
	}
}

func clampOrDefault(value, fallback, minValue, maxValue int) int {
	if value == 0 {
		value = fallback
	}

	if value < minValue {
		return minValue
	}

	if value > maxValue {
		return maxValue
	}

	return value
}
