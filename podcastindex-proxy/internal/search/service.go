package search

import (
	"context"
	"html"
	"net/url"
	"regexp"
	"slices"
	"strconv"
	"strings"
	"time"

	"github.com/mediathekview/podcastindex-proxy/internal/config"
	"github.com/mediathekview/podcastindex-proxy/internal/model"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
)

var (
	htmlTagPattern   = regexp.MustCompile(`(?s)<[^>]*>`)
	paragraphPattern = regexp.MustCompile(`(?i)</p\s*>|<br\s*/?>`)
)

type feedClient interface {
	SearchFeeds(ctx context.Context, query string, limit int) ([]podcastindex.Feed, error)
	LoadEpisodes(ctx context.Context, feed podcastindex.Feed, limit int) ([]podcastindex.Episode, error)
}

type Service struct {
	config config.Config
	client feedClient
}

func NewService(cfg config.Config, client feedClient) *Service {
	return &Service{config: cfg, client: client}
}

func (service *Service) Search(ctx context.Context, query string, feedLimit, episodeLimit *int) ([]model.AudioEntry, error) {
	effectiveFeedLimit := clamp(valueOrDefault(feedLimit, service.config.DefaultFeedLimit), 1, 50)
	effectiveEpisodeLimit := clamp(valueOrDefault(episodeLimit, service.config.DefaultEpisodeLimit), 1, 200)

	feeds, err := service.client.SearchFeeds(ctx, query, effectiveFeedLimit)
	if err != nil {
		return nil, err
	}

	results := make([]model.AudioEntry, 0, effectiveFeedLimit*effectiveEpisodeLimit)
	seenKeys := make(map[string]struct{})

	for _, feed := range feeds {
		episodes, loadErr := service.client.LoadEpisodes(ctx, feed, effectiveEpisodeLimit)
		if loadErr != nil {
			return nil, loadErr
		}

		for _, episode := range episodes {
			entry, ok := service.toAudioEntry(feed, episode)
			if !ok {
				continue
			}

			key := service.entryKey(entry)
			if _, exists := seenKeys[key]; exists {
				continue
			}

			seenKeys[key] = struct{}{}
			results = append(results, entry)
		}
	}

	return results, nil
}

func (service *Service) toAudioEntry(feed podcastindex.Feed, episode podcastindex.Episode) (model.AudioEntry, bool) {
	title := firstNonBlank(episode.Title, episode.Link, episode.EnclosureURL)
	if title == "" {
		return model.AudioEntry{}, false
	}

	entry := model.AudioEntry{
		Channel:     "Podcastindex",
		Genre:       service.genre(feed),
		Theme:       firstNonBlank(episode.FeedTitle, feed.Title, feed.Author, feed.OwnerName, "Podcast"),
		Title:       title,
		Description: sanitizeDescription(firstNonBlank(episode.Description, episode.ContentText)),
		AudioURL:    validURL(episode.EnclosureURL),
		WebsiteURL:  firstNonBlank(validURL(episode.Link), validURL(feed.Link), validURL(feed.URL)),
		IsPodcast:   true,
	}

	if episode.DatePublished != nil {
		publishedAt := time.Unix(*episode.DatePublished, 0).Local().Format("2006-01-02T15:04:05")
		entry.PublishedAt = publishedAt
	}

	if duration := durationMinutes(string(episode.Duration)); duration != nil {
		entry.DurationMinutes = duration
	}

	if size := sizeMB(episode.EnclosureLength); size != nil {
		entry.SizeMB = size
	}

	return entry, true
}

func (service *Service) genre(feed podcastindex.Feed) string {
	if len(feed.Categories) == 0 {
		return "Podcast"
	}

	values := make([]string, 0, len(feed.Categories))
	for _, value := range feed.Categories {
		trimmedValue := strings.TrimSpace(value)
		if trimmedValue != "" {
			values = append(values, trimmedValue)
		}
	}

	if len(values) == 0 {
		return "Podcast"
	}

	slices.Sort(values)
	return strings.Join(values, ", ")
}

func sanitizeDescription(value string) string {
	trimmedValue := strings.TrimSpace(value)
	if trimmedValue == "" {
		return ""
	}

	valueWithBreaks := paragraphPattern.ReplaceAllString(trimmedValue, "\n")
	withoutTags := htmlTagPattern.ReplaceAllString(valueWithBreaks, "")
	unescaped := html.UnescapeString(withoutTags)

	lines := strings.Split(unescaped, "\n")
	cleanedLines := make([]string, 0, len(lines))
	for _, line := range lines {
		normalizedLine := strings.Join(strings.Fields(line), " ")
		if normalizedLine != "" {
			cleanedLines = append(cleanedLines, normalizedLine)
		}
	}

	return strings.Join(cleanedLines, "\n")
}

func durationMinutes(value string) *int {
	trimmedValue := strings.TrimSpace(value)
	if trimmedValue == "" {
		return nil
	}

	totalSeconds, ok := parseDurationSeconds(trimmedValue)
	if !ok {
		return nil
	}

	if totalSeconds <= 0 {
		minutes := 0
		return &minutes
	}

	minutes := max(totalSeconds/60, 1)
	return &minutes
}

func parseDurationSeconds(value string) (int, bool) {
	if seconds, err := strconv.Atoi(value); err == nil {
		return seconds, true
	}

	parts := strings.Split(value, ":")
	if len(parts) == 0 {
		return 0, false
	}

	totalSeconds := 0
	for _, part := range parts {
		parsedPart, err := strconv.Atoi(strings.TrimSpace(part))
		if err != nil {
			return 0, false
		}

		totalSeconds = (totalSeconds * 60) + parsedPart
	}

	return totalSeconds, true
}

func sizeMB(value *int64) *int {
	if value == nil || *value <= 0 {
		return nil
	}

	size := max(int(*value/(1024*1024)), 1)
	return &size
}

func validURL(value string) string {
	trimmedValue := strings.TrimSpace(value)
	if trimmedValue == "" {
		return ""
	}

	parsedURL, err := url.Parse(trimmedValue)
	if err != nil || parsedURL.Scheme == "" || parsedURL.Host == "" {
		return ""
	}

	return trimmedValue
}

func (service *Service) entryKey(entry model.AudioEntry) string {
	return strings.Join([]string{
		entry.Title,
		entry.AudioURL,
		entry.WebsiteURL,
		entry.PublishedAt,
	}, "\x00")
}

func firstNonBlank(values ...string) string {
	for _, value := range values {
		trimmedValue := strings.TrimSpace(value)
		if trimmedValue != "" {
			return trimmedValue
		}
	}

	return ""
}

func valueOrDefault(value *int, fallback int) int {
	if value == nil {
		return fallback
	}

	return *value
}

func clamp(value, minValue, maxValue int) int {
	if value < minValue {
		return minValue
	}

	if value > maxValue {
		return maxValue
	}

	return value
}
