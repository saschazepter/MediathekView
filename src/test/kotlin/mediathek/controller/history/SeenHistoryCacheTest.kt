package mediathek.controller.history

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SeenHistoryCacheTest {
    @AfterEach
    fun tearDown() {
        SeenHistoryCache.clear()
    }

    @Test
    fun bloomLoadDoesNotTreatMatchesAsVerifiedUntilExactLookupConfirmsThem() {
        val url = "https://example.org/seen.mp4"

        SeenHistoryCache.load(SeenHistorySource.FILM, setOf(url))

        assertTrue(SeenHistoryCache.isPrepared(SeenHistorySource.FILM))
        assertTrue(SeenHistoryCache.mightContain(SeenHistorySource.FILM, url))
        assertFalse(SeenHistoryCache.containsVerified(SeenHistorySource.FILM, url))

        SeenHistoryCache.recordLookup(SeenHistorySource.FILM, url, seen = true)

        assertTrue(SeenHistoryCache.containsVerified(SeenHistorySource.FILM, url))
    }

    @Test
    fun removingUrlClearsOnlyExactVerificationBecauseBloomFiltersCannotDelete() {
        val url = "https://example.org/removed.mp4"
        SeenHistoryCache.load(SeenHistorySource.FILM, setOf(url))
        SeenHistoryCache.recordLookup(SeenHistorySource.FILM, url, seen = true)

        SeenHistoryCache.remove(SeenHistorySource.FILM, url)

        assertFalse(SeenHistoryCache.containsVerified(SeenHistorySource.FILM, url))
        assertTrue(SeenHistoryCache.mightContain(SeenHistorySource.FILM, url))
    }

    @Test
    fun markSeenUpdatesPreparedBloomAndExactVerification() {
        val url = "https://example.org/newly-seen.mp4"
        SeenHistoryCache.load(SeenHistorySource.FILM, emptySet())

        SeenHistoryCache.add(SeenHistorySource.FILM, url)

        assertTrue(SeenHistoryCache.mightContain(SeenHistorySource.FILM, url))
        assertTrue(SeenHistoryCache.containsVerified(SeenHistorySource.FILM, url))
    }
}
