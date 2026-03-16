package search

import (
	"slices"
	"strings"
	"time"

	"github.com/mediathekview/podcastindex-proxy/internal/model"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
)

func toAudioEntry(feed podcastindex.Feed, episode podcastindex.Episode) (model.AudioEntry, bool) {
	title := firstNonBlank(episode.Title, episode.Link, episode.EnclosureURL)
	if title == "" {
		return model.AudioEntry{}, false
	}

	entry := model.AudioEntry{
		Channel:     "Podcastindex",
		Genre:       genre(feed),
		Theme:       firstNonBlank(episode.FeedTitle, feed.Title, feed.Author, feed.OwnerName, "Podcast"),
		Title:       title,
		Description: sanitizeDescription(firstNonBlank(episode.Description, episode.ContentText)),
		AudioURL:    validURL(episode.EnclosureURL),
		WebsiteURL:  firstNonBlank(validURL(episode.Link), validURL(feed.Link), validURL(feed.URL)),
		IsPodcast:   true,
	}

	if episode.DatePublished != nil {
		entry.PublishedAt = time.Unix(*episode.DatePublished, 0).Local().Format("2006-01-02T15:04:05")
	}

	if duration := durationMinutes(string(episode.Duration)); duration != nil {
		entry.DurationMinutes = duration
	}

	if size := sizeMB(episode.EnclosureLength); size != nil {
		entry.SizeMB = size
	}

	return entry, true
}

func genre(feed podcastindex.Feed) string {
	if len(feed.Categories) == 0 {
		return "Podcast"
	}

	values := make([]string, 0, len(feed.Categories))
	for _, value := range feed.Categories {
		if trimmed := strings.TrimSpace(value); trimmed != "" {
			values = append(values, trimmed)
		}
	}

	if len(values) == 0 {
		return "Podcast"
	}

	slices.Sort(values)
	return strings.Join(values, ", ")
}

func entryKey(entry model.AudioEntry) string {
	return strings.Join([]string{
		entry.Title,
		entry.AudioURL,
		entry.WebsiteURL,
		entry.PublishedAt,
	}, "\x00")
}
