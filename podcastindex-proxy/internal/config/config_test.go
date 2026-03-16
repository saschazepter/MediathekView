package config

import (
	"os"
	"path/filepath"
	"testing"
)

func TestFromEnvironmentReadsLimitsFromConfigFile(t *testing.T) {
	tempDir := t.TempDir()
	configFile := filepath.Join(tempDir, "podcastindex-proxy.conf")
	configBody := "" +
		"PODCASTINDEX_API_KEY=file-key\n" +
		"PODCASTINDEX_API_SECRET=file-secret\n" +
		"PODCASTINDEX_FEED_LIMIT=7\n" +
		"PODCASTINDEX_EPISODE_LIMIT=42\n"

	if err := os.WriteFile(configFile, []byte(configBody), 0o600); err != nil {
		t.Fatalf("write config file: %v", err)
	}

	t.Setenv("PODCASTINDEX_PROXY_CONFIG_FILE", configFile)
	t.Setenv("PODCASTINDEX_API_KEY", "")
	t.Setenv("PODCASTINDEX_API_SECRET", "")
	t.Setenv("PODCASTINDEX_FEED_LIMIT", "")
	t.Setenv("PODCASTINDEX_EPISODE_LIMIT", "")

	cfg, err := FromEnvironment()
	if err != nil {
		t.Fatalf("FromEnvironment() error = %v", err)
	}

	if cfg.DefaultFeedLimit != 7 {
		t.Fatalf("DefaultFeedLimit = %d, want 7", cfg.DefaultFeedLimit)
	}

	if cfg.DefaultEpisodeLimit != 42 {
		t.Fatalf("DefaultEpisodeLimit = %d, want 42", cfg.DefaultEpisodeLimit)
	}
}

func TestFromEnvironmentPrefersEnvironmentOverConfigFileForLimits(t *testing.T) {
	tempDir := t.TempDir()
	configFile := filepath.Join(tempDir, "podcastindex-proxy.conf")
	configBody := "" +
		"PODCASTINDEX_API_KEY=file-key\n" +
		"PODCASTINDEX_API_SECRET=file-secret\n" +
		"PODCASTINDEX_FEED_LIMIT=7\n" +
		"PODCASTINDEX_EPISODE_LIMIT=42\n"

	if err := os.WriteFile(configFile, []byte(configBody), 0o600); err != nil {
		t.Fatalf("write config file: %v", err)
	}

	t.Setenv("PODCASTINDEX_PROXY_CONFIG_FILE", configFile)
	t.Setenv("PODCASTINDEX_API_KEY", "")
	t.Setenv("PODCASTINDEX_API_SECRET", "")
	t.Setenv("PODCASTINDEX_FEED_LIMIT", "9")
	t.Setenv("PODCASTINDEX_EPISODE_LIMIT", "11")

	cfg, err := FromEnvironment()
	if err != nil {
		t.Fatalf("FromEnvironment() error = %v", err)
	}

	if cfg.DefaultFeedLimit != 9 {
		t.Fatalf("DefaultFeedLimit = %d, want 9", cfg.DefaultFeedLimit)
	}

	if cfg.DefaultEpisodeLimit != 11 {
		t.Fatalf("DefaultEpisodeLimit = %d, want 11", cfg.DefaultEpisodeLimit)
	}
}
