/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.swingaudiothek.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mediathek.swingaudiothek.model.AudioEntry
import mediathek.tool.ApplicationConfiguration
import mediathek.tool.http.MVHttpClient
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import tools.jackson.core.JsonParser
import tools.jackson.core.JsonToken
import tools.jackson.core.ObjectReadContext
import tools.jackson.core.json.JsonFactory
import java.io.InputStream
import java.net.URI
import java.security.MessageDigest
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.concurrent.TimeUnit

class PodcastIndexSearchRepository(
    private val client: OkHttpClient = MVHttpClient.getInstance().httpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build(),
    private val credentialsProvider: PodcastIndexCredentialsProvider = PodcastIndexCredentialsProvider()
) {
    private val jsonFactory = JsonFactory()

    suspend fun search(query: String): List<AudioEntry> = withContext(Dispatchers.IO) {
        val normalizedQuery = query.trim()
        if (normalizedQuery.isEmpty()) {
            return@withContext emptyList()
        }

        val credentials = credentialsProvider.read() ?: return@withContext emptyList()
        val feeds = executeAuthenticatedRequest(
            credentials = credentials,
            url = SEARCH_URL.toHttpUrl().newBuilder()
                .addQueryParameter("q", normalizedQuery)
                .addQueryParameter("max", SEARCH_LIMIT.toString())
                .build()
        ) { body ->
            body.byteStream().use(::parseFeeds)
        }

        feeds.flatMap { feed ->
            loadEpisodesForFeed(credentials, feed)
        }.distinctBy(::entryKey)
    }

    private fun loadEpisodesForFeed(credentials: PodcastIndexCredentials, feed: FeedSearchResult): List<AudioEntry> {
        val urlBuilder = when {
            feed.id != null -> EPISODES_BY_FEED_ID_URL.toHttpUrl().newBuilder()
                .addQueryParameter("id", feed.id.toString())
            feed.feedUrl.isNotBlank() -> EPISODES_BY_FEED_URL.toHttpUrl().newBuilder()
                .addQueryParameter("url", feed.feedUrl)
            else -> return emptyList()
        }

        return executeAuthenticatedRequest(credentials, urlBuilder.build()) { body ->
            body.byteStream().use { parseEpisodes(it, feed) }
        }
    }

    private fun parseFeeds(inputStream: InputStream): List<FeedSearchResult> {
        val entries = mutableListOf<FeedSearchResult>()
        jsonFactory.createParser(ObjectReadContext.empty(), inputStream).use { parser ->
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return emptyList()
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = parser.currentName() ?: continue
                val token = parser.nextToken()
                if (fieldName == "feeds" && token == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        parseFeed(parser)?.let(entries::add)
                    }
                } else {
                    parser.skipChildren()
                }
            }
        }
        return entries
    }

    private fun parseFeed(parser: JsonParser): FeedSearchResult? {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            parser.skipChildren()
            return null
        }

        var id: Long? = null
        var title = ""
        var author = ""
        var ownerName = ""
        var description = ""
        var link = ""
        var feedUrl = ""
        var updatedEpochSeconds: Long? = null
        var newestItemEpochSeconds: Long? = null
        var categories = emptyList<String>()

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = parser.currentName() ?: continue
            val token = parser.nextToken()
            when (fieldName) {
                "id" -> id = parser.getValueAsLong(0L).takeIf { it > 0L }
                "title" -> title = parser.getValueAsString("").orEmpty()
                "author" -> author = parser.getValueAsString("").orEmpty()
                "ownerName" -> ownerName = parser.getValueAsString("").orEmpty()
                "description" -> description = parser.getValueAsString("").orEmpty()
                "link" -> link = parser.getValueAsString("").orEmpty()
                "url" -> feedUrl = parser.getValueAsString("").orEmpty()
                "lastUpdateTime" -> updatedEpochSeconds = parser.getValueAsLong(0L).takeIf { it > 0L }
                "newestItemPubdate" -> newestItemEpochSeconds = parser.getValueAsLong(0L).takeIf { it > 0L }
                "categories" -> if (token == JsonToken.START_OBJECT) {
                    categories = parseCategories(parser)
                } else {
                    parser.skipChildren()
                }
                else -> parser.skipChildren()
            }
        }

        val resolvedTitle = title.ifBlank { link.ifBlank { feedUrl } }
        val websiteUrl = parseUri(link).orElseParse(feedUrl)
        if (resolvedTitle.isBlank() && websiteUrl == null) {
            return null
        }

        return FeedSearchResult(
            id = id,
            title = resolvedTitle,
            author = author.ifBlank { ownerName },
            description = sanitizeDescription(description),
            websiteUrl = websiteUrl,
            feedUrl = feedUrl,
            genre = categories.joinToString(", ").ifBlank { "Podcast" },
            publishedAt = epochSecondsToLocalDateTime(newestItemEpochSeconds ?: updatedEpochSeconds)
        )
    }

    private fun parseEpisodes(inputStream: InputStream, feed: FeedSearchResult): List<AudioEntry> {
        val entries = mutableListOf<AudioEntry>()
        jsonFactory.createParser(ObjectReadContext.empty(), inputStream).use { parser ->
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return emptyList()
            }

            while (parser.nextToken() != JsonToken.END_OBJECT) {
                val fieldName = parser.currentName() ?: continue
                val token = parser.nextToken()
                if ((fieldName == "items" || fieldName == "episodes") && token == JsonToken.START_ARRAY) {
                    while (parser.nextToken() != JsonToken.END_ARRAY) {
                        parseEpisode(parser, feed)?.let(entries::add)
                    }
                } else {
                    parser.skipChildren()
                }
            }
        }
        return entries
    }

    private fun parseEpisode(parser: JsonParser, feed: FeedSearchResult): AudioEntry? {
        if (parser.currentToken() != JsonToken.START_OBJECT) {
            parser.skipChildren()
            return null
        }

        var title = ""
        var description = ""
        var enclosureUrl = ""
        var link = ""
        var durationSeconds: Int? = null
        var enclosureLength: Long? = null
        var datePublished: Long? = null
        var feedTitle = ""

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            val fieldName = parser.currentName() ?: continue
            val token = parser.nextToken()
            when (fieldName) {
                "title" -> title = parser.getValueAsString("").orEmpty()
                "description", "contentText" -> if (description.isBlank()) {
                    description = parser.getValueAsString("").orEmpty()
                } else {
                    parser.skipChildren()
                }
                "enclosureUrl" -> enclosureUrl = parser.getValueAsString("").orEmpty()
                "link" -> link = parser.getValueAsString("").orEmpty()
                "duration" -> durationSeconds = parseDurationSeconds(parser.getValueAsString("").orEmpty())
                "enclosureLength" -> enclosureLength = parser.getValueAsLong(0L).takeIf { it > 0L }
                "datePublished" -> datePublished = parser.getValueAsLong(0L).takeIf { it > 0L }
                "feedTitle" -> feedTitle = parser.getValueAsString("").orEmpty()
                else -> parser.skipChildren()
            }
        }

        val resolvedTitle = title.ifBlank { link.ifBlank { enclosureUrl } }
        if (resolvedTitle.isBlank()) {
            return null
        }

        return AudioEntry(
            channel = "Podcastindex",
            genre = feed.genre,
            theme = feedTitle.ifBlank { feed.title.ifBlank { feed.author.ifBlank { "Podcast" } } },
            title = resolvedTitle,
            durationMinutes = durationSeconds?.let(::secondsToMinutes),
            sizeMb = enclosureLength?.let(::bytesToMegabytes),
            description = sanitizeDescription(description),
            audioUrl = parseUri(enclosureUrl),
            websiteUrl = parseUri(link).orElse(feed.websiteUrl),
            isNew = false,
            isPodcast = true,
            isDuplicate = false,
            publishedAt = epochSecondsToLocalDateTime(datePublished) ?: feed.publishedAt
        )
    }

    private fun parseCategories(parser: JsonParser): List<String> {
        val categories = linkedSetOf<String>()
        while (parser.nextToken() != JsonToken.END_OBJECT) {
            parser.currentName()
            parser.nextToken()
            parser.getValueAsString("")
                ?.trim()
                ?.takeIf(String::isNotEmpty)
                ?.let(categories::add)
        }
        return categories.toList()
    }

    private fun readUserAgent(): String {
        return ApplicationConfiguration.getConfiguration()
            .getString(ApplicationConfiguration.APPLICATION_USER_AGENT, "MediathekView")
            .ifBlank { "MediathekView" }
    }

    private fun buildAuthorization(credentials: PodcastIndexCredentials, authDate: String): String {
        val payload = credentials.key + credentials.secret + authDate
        return MessageDigest.getInstance("SHA-1")
            .digest(payload.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }

    private fun <T> executeAuthenticatedRequest(
        credentials: PodcastIndexCredentials,
        url: okhttp3.HttpUrl,
        parse: (okhttp3.ResponseBody) -> T
    ): T {
        val authDate = Instant.now().epochSecond.toString()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", readUserAgent())
            .header("X-Auth-Key", credentials.key)
            .header("X-Auth-Date", authDate)
            .header("Authorization", buildAuthorization(credentials, authDate))
            .get()
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("Podcastindex-Anfrage fehlgeschlagen: HTTP ${response.code}")
            }
            val body = response.body ?: error("Podcastindex-Antwort ohne Body")
            return parse(body)
        }
    }

    private fun epochSecondsToLocalDateTime(epochSeconds: Long?): LocalDateTime? {
        epochSeconds ?: return null
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault())
    }

    private fun parseDurationSeconds(value: String): Int? {
        if (value.isBlank()) {
            return null
        }
        return value.toIntOrNull()
            ?: value.split(':')
                .map(String::trim)
                .takeIf { it.isNotEmpty() && it.all(String::isNotEmpty) }
                ?.fold(0) { acc, part -> (acc * 60) + (part.toIntOrNull() ?: return null) }
    }

    private fun secondsToMinutes(totalSeconds: Int): Int {
        if (totalSeconds <= 0) {
            return 0
        }
        return maxOf(1, totalSeconds / 60)
    }

    private fun bytesToMegabytes(totalBytes: Long): Int? {
        if (totalBytes <= 0L) {
            return null
        }
        return maxOf(1L, totalBytes / (1024L * 1024L)).toInt()
    }

    private fun parseUri(value: String): URI? {
        if (value.isBlank()) {
            return null
        }
        return runCatching { URI(value) }.getOrNull()
    }

    private fun sanitizeDescription(value: String): String {
        if (value.isBlank()) {
            return ""
        }
        return Jsoup.parse(value)
            .wholeText()
            .lines()
            .map(String::trim)
            .filter(String::isNotEmpty)
            .joinToString("\n")
    }

    private fun URI?.orElse(value: URI?): URI? {
        return this ?: value
    }

    private fun URI?.orElseParse(value: String): URI? {
        return this ?: parseUri(value)
    }

    private fun entryKey(entry: AudioEntry): String {
        return listOf(
            entry.title,
            entry.audioUrl?.toString().orEmpty(),
            entry.websiteUrl?.toString().orEmpty(),
            entry.publishedAt?.toString().orEmpty()
        ).joinToString("\u0000")
    }

    private data class FeedSearchResult(
        val id: Long?,
        val title: String,
        val author: String,
        val description: String,
        val websiteUrl: URI?,
        val feedUrl: String,
        val genre: String,
        val publishedAt: LocalDateTime?
    )

    companion object {
        private const val SEARCH_URL = "https://api.podcastindex.org/api/1.0/search/byterm"
        private const val EPISODES_BY_FEED_ID_URL = "https://api.podcastindex.org/api/1.0/episodes/byfeedid"
        private const val EPISODES_BY_FEED_URL = "https://api.podcastindex.org/api/1.0/episodes/byfeedurl"
        private const val SEARCH_LIMIT = 25
    }
}
