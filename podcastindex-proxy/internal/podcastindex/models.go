package podcastindex

import (
	"bytes"
	"encoding/json"
	"fmt"
	"strconv"
)

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
	Title           string      `json:"title"`
	Description     string      `json:"description"`
	ContentText     string      `json:"contentText"`
	EnclosureURL    string      `json:"enclosureUrl"`
	EnclosureLength *int64      `json:"enclosureLength"`
	Link            string      `json:"link"`
	Duration        StringValue `json:"duration"`
	FeedTitle       string      `json:"feedTitle"`
	DatePublished   *int64      `json:"datePublished"`
}

type StringValue string

func (value *StringValue) UnmarshalJSON(data []byte) error {
	trimmed := bytes.TrimSpace(data)
	if bytes.Equal(trimmed, []byte("null")) {
		*value = ""
		return nil
	}

	var stringValue string
	if err := json.Unmarshal(trimmed, &stringValue); err == nil {
		*value = StringValue(stringValue)
		return nil
	}

	var intValue int64
	if err := json.Unmarshal(trimmed, &intValue); err == nil {
		*value = StringValue(strconv.FormatInt(intValue, 10))
		return nil
	}

	var floatValue float64
	if err := json.Unmarshal(trimmed, &floatValue); err == nil {
		*value = StringValue(strconv.FormatFloat(floatValue, 'f', -1, 64))
		return nil
	}

	return fmt.Errorf("unsupported JSON value for string field: %s", string(trimmed))
}
