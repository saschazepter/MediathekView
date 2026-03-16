package config

import (
	"bufio"
	"fmt"
	"net"
	"os"
	"path/filepath"
	"strconv"
	"strings"
)

type Config struct {
	Host                string
	Port                int
	APIKey              string
	APISecret           string
	UserAgent           string
	DefaultFeedLimit    int
	DefaultEpisodeLimit int
	ConfigFile          string
}

func FromEnvironment() (Config, error) {
	fileValues, configFile, err := loadConfigFile()
	if err != nil {
		return Config{}, err
	}

	apiKey, err := requireSetting("PODCASTINDEX_API_KEY", fileValues)
	if err != nil {
		return Config{}, err
	}

	apiSecret, err := requireSetting("PODCASTINDEX_API_SECRET", fileValues)
	if err != nil {
		return Config{}, err
	}

	return Config{
		Host:                stringSetting("PODCASTINDEX_PROXY_HOST", fileValues, "0.0.0.0"),
		Port:                intSetting("PODCASTINDEX_PROXY_PORT", fileValues, 8080, 1, 65535),
		APIKey:              apiKey,
		APISecret:           apiSecret,
		UserAgent:           stringSetting("PODCASTINDEX_PROXY_USER_AGENT", fileValues, "MediathekView-Podcastindex-Proxy"),
		DefaultFeedLimit:    intSetting("PODCASTINDEX_FEED_LIMIT", fileValues, 10, 1, 50),
		DefaultEpisodeLimit: intSetting("PODCASTINDEX_EPISODE_LIMIT", fileValues, 100, 1, 200),
		ConfigFile:          configFile,
	}, nil
}

func (c Config) ListenAddress() string {
	return net.JoinHostPort(c.Host, strconv.Itoa(c.Port))
}

func requireSetting(name string, fileValues map[string]string) (string, error) {
	if value := strings.TrimSpace(os.Getenv(name)); value != "" {
		return value, nil
	}

	if value := strings.TrimSpace(fileValues[name]); value != "" {
		return value, nil
	}

	return "", fmt.Errorf("%q fehlt in Umgebung und Konfigurationsdatei", name)
}

func loadConfigFile() (map[string]string, string, error) {
	configFile := strings.TrimSpace(os.Getenv("PODCASTINDEX_PROXY_CONFIG_FILE"))
	if configFile == "" {
		configFile = "/etc/podcastindex-proxy.conf"
	}
	fileInfo, err := os.Stat(configFile)
	if err != nil {
		if os.IsNotExist(err) {
			return map[string]string{}, configFile, nil
		}

		return nil, "", fmt.Errorf("Konfigurationsdatei %q kann nicht gelesen werden: %w", configFile, err)
	}

	if fileInfo.IsDir() {
		return nil, "", fmt.Errorf("Konfigurationsdatei %q ist ein Verzeichnis", configFile)
	}

	file, err := os.Open(filepath.Clean(configFile))
	if err != nil {
		return nil, "", fmt.Errorf("Konfigurationsdatei %q kann nicht geöffnet werden: %w", configFile, err)
	}
	defer file.Close()

	values := make(map[string]string)
	scanner := bufio.NewScanner(file)
	for scanner.Scan() {
		line := strings.TrimSpace(scanner.Text())
		if line == "" || strings.HasPrefix(line, "#") {
			continue
		}

		key, value, found := strings.Cut(line, "=")
		if !found {
			return nil, "", fmt.Errorf("Ungültige Zeile in %q: %q", configFile, line)
		}

		values[strings.TrimSpace(key)] = strings.TrimSpace(value)
	}

	if err := scanner.Err(); err != nil {
		return nil, "", fmt.Errorf("Konfigurationsdatei %q kann nicht gelesen werden: %w", configFile, err)
	}

	return values, configFile, nil
}

func stringSetting(name string, fileValues map[string]string, fallback string) string {
	if value := strings.TrimSpace(os.Getenv(name)); value != "" {
		return value
	}

	if value := strings.TrimSpace(fileValues[name]); value != "" {
		return value
	}

	return fallback
}

func intSetting(name string, fileValues map[string]string, fallback, min, max int) int {
	return clampInt(parseIntOrFallback(stringSetting(name, fileValues, ""), fallback), min, max)
}

func parseIntOrFallback(value string, fallback int) int {
	if value == "" {
		return fallback
	}

	parsed, err := strconv.Atoi(value)
	if err != nil {
		return fallback
	}

	return parsed
}

func clampInt(value, min, max int) int {
	if value < min {
		return min
	}

	if value > max {
		return max
	}

	return value
}
