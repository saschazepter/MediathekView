package api

import (
	"fmt"
	"net/http"
	"strconv"
	"strings"

	"github.com/mediathekview/podcastindex-proxy/internal/search"
)

func parseSearchOptions(request *http.Request) (search.Options, error) {
	feedLimit, err := optionalInt(request, "feedLimit")
	if err != nil {
		return search.Options{}, err
	}

	episodeLimit, err := optionalInt(request, "episodeLimit")
	if err != nil {
		return search.Options{}, err
	}

	return search.Options{
		FeedLimit:    feedLimit,
		EpisodeLimit: episodeLimit,
	}, nil
}

func optionalInt(request *http.Request, name string) (int, error) {
	rawValue := strings.TrimSpace(request.URL.Query().Get(name))
	if rawValue == "" {
		return 0, nil
	}

	parsedValue, err := strconv.Atoi(rawValue)
	if err != nil {
		return 0, fmt.Errorf("Query-Parameter %q ist ungültig", name)
	}

	return parsedValue, nil
}
