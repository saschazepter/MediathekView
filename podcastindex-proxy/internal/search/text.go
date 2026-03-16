package search

import (
	"html"
	"net/url"
	"regexp"
	"strconv"
	"strings"
)

var (
	htmlTagPattern   = regexp.MustCompile(`(?s)<[^>]*>`)
	paragraphPattern = regexp.MustCompile(`(?i)</p\s*>|<br\s*/?>`)
)

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
		if normalized := strings.Join(strings.Fields(line), " "); normalized != "" {
			cleanedLines = append(cleanedLines, normalized)
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

	totalSeconds := 0
	for _, part := range strings.Split(value, ":") {
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

func firstNonBlank(values ...string) string {
	for _, value := range values {
		if trimmed := strings.TrimSpace(value); trimmed != "" {
			return trimmed
		}
	}

	return ""
}
