package podcastindex

import (
	"context"
	"crypto/sha1"
	"encoding/hex"
	"encoding/json"
	"fmt"
	"net/http"
	"net/url"
	"strconv"
	"time"

	"github.com/mediathekview/podcastindex-proxy/internal/config"
)

const (
	searchByTermURL     = "https://api.podcastindex.org/api/1.0/search/byterm"
	episodesByFeedIDURL = "https://api.podcastindex.org/api/1.0/episodes/byfeedid"
	episodesByFeedURL   = "https://api.podcastindex.org/api/1.0/episodes/byfeedurl"
)

type Client struct {
	config     config.Config
	httpClient *http.Client
}

type StatusError struct {
	Code    int
	Message string
}

func (error *StatusError) Error() string {
	return error.Message
}

func NewClient(cfg config.Config, httpClient *http.Client) *Client {
	return &Client{config: cfg, httpClient: httpClient}
}

func (client *Client) SearchFeeds(ctx context.Context, query string, limit int) ([]Feed, error) {
	endpoint, err := url.Parse(searchByTermURL)
	if err != nil {
		return nil, err
	}

	params := endpoint.Query()
	params.Set("q", query)
	params.Set("max", strconv.Itoa(limit))
	endpoint.RawQuery = params.Encode()

	var response SearchResponse
	if err := client.doRequest(ctx, endpoint.String(), &response); err != nil {
		return nil, err
	}

	return response.Feeds, nil
}

func (client *Client) LoadEpisodes(ctx context.Context, feed Feed, limit int) ([]Episode, error) {
	endpointURL, parameterName, parameterValue, err := client.resolveEpisodeRequest(feed)
	if err != nil {
		return nil, err
	}

	endpoint, err := url.Parse(endpointURL)
	if err != nil {
		return nil, err
	}

	params := endpoint.Query()
	params.Set(parameterName, parameterValue)
	params.Set("max", strconv.Itoa(limit))
	endpoint.RawQuery = params.Encode()

	var response EpisodesResponse
	if err := client.doRequest(ctx, endpoint.String(), &response); err != nil {
		return nil, err
	}

	return response.MergedItems(), nil
}

func (client *Client) doRequest(ctx context.Context, endpoint string, target any) error {
	timestamp := strconv.FormatInt(time.Now().Unix(), 10)
	request, err := http.NewRequestWithContext(ctx, http.MethodGet, endpoint, nil)
	if err != nil {
		return err
	}

	request.Header.Set("User-Agent", client.config.UserAgent)
	request.Header.Set("X-Auth-Key", client.config.APIKey)
	request.Header.Set("X-Auth-Date", timestamp)
	request.Header.Set("Authorization", client.authorization(timestamp))

	response, err := client.httpClient.Do(request)
	if err != nil {
		return err
	}
	defer response.Body.Close()

	if response.StatusCode < http.StatusOK || response.StatusCode >= http.StatusMultipleChoices {
		return &StatusError{
			Code:    response.StatusCode,
			Message: fmt.Sprintf("Podcastindex antwortete mit HTTP %d", response.StatusCode),
		}
	}

	return json.NewDecoder(response.Body).Decode(target)
}

func (client *Client) authorization(timestamp string) string {
	hash := sha1.Sum([]byte(client.config.APIKey + client.config.APISecret + timestamp))
	return hex.EncodeToString(hash[:])
}

func (client *Client) resolveEpisodeRequest(feed Feed) (string, string, string, error) {
	if feed.ID != nil {
		return episodesByFeedIDURL, "id", strconv.FormatInt(*feed.ID, 10), nil
	}

	if feed.URL != "" {
		return episodesByFeedURL, "url", feed.URL, nil
	}

	return "", "", "", fmt.Errorf("Feed ohne id und ohne url kann nicht geladen werden")
}
