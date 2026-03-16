package podcastindex

import (
	"encoding/json"
	"testing"
)

func TestEpisodeDurationUnmarshalString(t *testing.T) {
	t.Parallel()

	var episode Episode
	if err := json.Unmarshal([]byte(`{"duration":"01:23:45"}`), &episode); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}

	if got, want := string(episode.Duration), "01:23:45"; got != want {
		t.Fatalf("duration = %q, want %q", got, want)
	}
}

func TestEpisodeDurationUnmarshalNumber(t *testing.T) {
	t.Parallel()

	var episode Episode
	if err := json.Unmarshal([]byte(`{"duration":3600}`), &episode); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}

	if got, want := string(episode.Duration), "3600"; got != want {
		t.Fatalf("duration = %q, want %q", got, want)
	}
}

func TestEpisodeDurationUnmarshalNull(t *testing.T) {
	t.Parallel()

	var episode Episode
	if err := json.Unmarshal([]byte(`{"duration":null}`), &episode); err != nil {
		t.Fatalf("unmarshal failed: %v", err)
	}

	if got := string(episode.Duration); got != "" {
		t.Fatalf("duration = %q, want empty string", got)
	}
}
