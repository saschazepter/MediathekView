package api

import (
	"bytes"
	"context"
	"encoding/json"
	"errors"
	"net/http"
	"net/http/httptest"
	"testing"

	"github.com/mediathekview/podcastindex-proxy/internal/model"
	"github.com/mediathekview/podcastindex-proxy/internal/podcastindex"
	"github.com/mediathekview/podcastindex-proxy/internal/search"
)

type stubSearchService struct {
	searchFn func(ctx context.Context, query string, options search.Options) ([]model.AudioEntry, error)
}

func (stub stubSearchService) Search(ctx context.Context, query string, options search.Options) ([]model.AudioEntry, error) {
	return stub.searchFn(ctx, query, options)
}

func TestHandlePodcastSearchRejectsMissingQuery(t *testing.T) {
	t.Parallel()

	handler := NewHandler(stubSearchService{
		searchFn: func(_ context.Context, _ string, _ search.Options) ([]model.AudioEntry, error) {
			t.Fatal("search service should not be called")
			return nil, nil
		},
	}, &bytes.Buffer{})

	request := httptest.NewRequest(http.MethodGet, "/api/audiothek/podcast-search", nil)
	recorder := httptest.NewRecorder()

	handler.ServeHTTP(recorder, request)

	if recorder.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusBadRequest)
	}
}

func TestHandlePodcastSearchPassesParsedOptions(t *testing.T) {
	t.Parallel()

	handler := NewHandler(stubSearchService{
		searchFn: func(_ context.Context, query string, options search.Options) ([]model.AudioEntry, error) {
			if query != "hörspiel" {
				t.Fatalf("query = %q, want %q", query, "hörspiel")
			}
			if options.FeedLimit != 5 || options.EpisodeLimit != 12 {
				t.Fatalf("options = %+v, want feedLimit=5 episodeLimit=12", options)
			}

			return []model.AudioEntry{{Title: "Test", IsPodcast: true}}, nil
		},
	}, &bytes.Buffer{})

	request := httptest.NewRequest(http.MethodGet, "/api/audiothek/podcast-search?q=hörspiel&feedLimit=5&episodeLimit=12", nil)
	recorder := httptest.NewRecorder()

	handler.ServeHTTP(recorder, request)

	if recorder.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusOK)
	}

	var response model.PodcastSearchResponse
	if err := json.Unmarshal(recorder.Body.Bytes(), &response); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}

	if len(response.Results) != 1 || response.Results[0].Title != "Test" {
		t.Fatalf("unexpected response: %+v", response.Results)
	}
}

func TestHandlePodcastSearchMapsUpstreamStatusErrors(t *testing.T) {
	t.Parallel()

	handler := NewHandler(stubSearchService{
		searchFn: func(_ context.Context, _ string, _ search.Options) ([]model.AudioEntry, error) {
			return nil, &podcastindex.StatusError{Code: http.StatusTooManyRequests, Message: "upstream busy"}
		},
	}, &bytes.Buffer{})

	request := httptest.NewRequest(http.MethodGet, "/api/audiothek/podcast-search?q=test", nil)
	recorder := httptest.NewRecorder()

	handler.ServeHTTP(recorder, request)

	if recorder.Code != http.StatusBadGateway {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusBadGateway)
	}
}

func TestHandlePodcastSearchMapsInternalErrors(t *testing.T) {
	t.Parallel()

	handler := NewHandler(stubSearchService{
		searchFn: func(_ context.Context, _ string, _ search.Options) ([]model.AudioEntry, error) {
			return nil, errors.New("boom")
		},
	}, &bytes.Buffer{})

	request := httptest.NewRequest(http.MethodGet, "/api/audiothek/podcast-search?q=test", nil)
	recorder := httptest.NewRecorder()

	handler.ServeHTTP(recorder, request)

	if recorder.Code != http.StatusInternalServerError {
		t.Fatalf("status = %d, want %d", recorder.Code, http.StatusInternalServerError)
	}
}
