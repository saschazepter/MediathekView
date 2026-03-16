package podcastindex

type SearchResponse struct {
	Feeds []Feed `json:"feeds"`
}

type EpisodesResponse struct {
	Items    []Episode `json:"items"`
	Episodes []Episode `json:"episodes"`
}

func (response EpisodesResponse) MergedItems() []Episode {
	if len(response.Items) > 0 {
		return response.Items
	}

	return response.Episodes
}

type Feed struct {
	ID         *int64            `json:"id"`
	Title      string            `json:"title"`
	Author     string            `json:"author"`
	OwnerName  string            `json:"ownerName"`
	URL        string            `json:"url"`
	Link       string            `json:"link"`
	Categories map[string]string `json:"categories"`
}

type Episode struct {
	Title           string `json:"title"`
	Description     string `json:"description"`
	ContentText     string `json:"contentText"`
	EnclosureURL    string `json:"enclosureUrl"`
	EnclosureLength *int64 `json:"enclosureLength"`
	Link            string `json:"link"`
	Duration        string `json:"duration"`
	FeedTitle       string `json:"feedTitle"`
	DatePublished   *int64 `json:"datePublished"`
}
