package search

import "testing"

func TestSanitizeDescriptionRemovesHTMLAndNormalizesWhitespace(t *testing.T) {
	t.Parallel()

	input := "<p> Hallo&nbsp;Welt </p><br><div> noch   mehr </div>"
	got := sanitizeDescription(input)
	want := "Hallo Welt\nnoch mehr"

	if got != want {
		t.Fatalf("sanitizeDescription() = %q, want %q", got, want)
	}
}

func TestDurationMinutesSupportsClockFormat(t *testing.T) {
	t.Parallel()

	got := durationMinutes("01:30:00")
	if got == nil || *got != 90 {
		t.Fatalf("durationMinutes() = %v, want 90", got)
	}
}

func TestDurationMinutesRoundsShortDurationUpToOneMinute(t *testing.T) {
	t.Parallel()

	got := durationMinutes("59")
	if got == nil || *got != 1 {
		t.Fatalf("durationMinutes() = %v, want 1", got)
	}
}
