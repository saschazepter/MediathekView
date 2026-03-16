package model

type HealthResponse struct {
	Status string `json:"status"`
}

type ErrorResponse struct {
	Error string `json:"error"`
}

type PodcastSearchResponse struct {
	Results []AudioEntry `json:"results"`
}

type AudioEntry struct {
	Channel         string `json:"channel"`
	Genre           string `json:"genre"`
	Theme           string `json:"theme"`
	Title           string `json:"title"`
	Description     string `json:"description"`
	AudioURL        string `json:"audioUrl,omitempty"`
	WebsiteURL      string `json:"websiteUrl,omitempty"`
	PublishedAt     string `json:"publishedAt,omitempty"`
	DurationMinutes *int   `json:"durationMinutes,omitempty"`
	SizeMB          *int   `json:"sizeMb,omitempty"`
	IsPodcast       bool   `json:"isPodcast"`
}
