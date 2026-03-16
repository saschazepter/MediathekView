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

package mediathek.audiothek.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mediathek.audiothek.model.AudioDataset
import mediathek.tool.http.MVHttpClient
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.logging.log4j.LogManager
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.*
import java.util.concurrent.TimeUnit

class AudioRepository(
    private val client: OkHttpClient = MVHttpClient.getInstance().httpClient.newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            val requestWithoutUserAgent = chain.request().newBuilder()
                .removeHeader("User-Agent")
                .build()
            chain.proceed(requestWithoutUserAgent)
        }
        .build(),
    private val resolver: AudioSourceResolver = AudioSourceResolver(client),
    private val parser: AudioParser = AudioParser(),
    private val cache: AudioDownloadCache = AudioDownloadCache()
) {
    private val logger = LogManager.getLogger(AudioRepository::class.java)

    suspend fun loadAudiothek(useCachedOnDownloadFailure: Boolean = false): AudioLoadResult = withContext(Dispatchers.IO) {
        val cachedMetadata = cache.readMetadata()
        val sourceUrl = resolver.resolveSourceUrl(cachedMetadata?.sourceUrl)
        val request = Request.Builder()
            .url(sourceUrl)
            .apply {
                cachedMetadata?.eTag?.let { header("If-None-Match", it) }
                cachedMetadata?.lastModified?.let { header("If-Modified-Since", it) }
            }
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                when (response.code) {
                    304 -> {
                        if (!cache.hasCachedAudio()) {
                            error("Audiothek cache miss after HTTP 304 for $sourceUrl")
                        }
                        logger.info("Audiothek unchanged, no new audio file downloaded for {}", sourceUrl)
                        return@withContext cachedResult(sourceUrl, AudioDownloadStatus.NOT_MODIFIED)
                    }

                    !in 200..299 -> {
                        if (cache.hasCachedAudio()) {
                            logger.warn(
                                "Audiothek download skipped after HTTP {} for {}, using cached audio file",
                                response.code,
                                sourceUrl
                            )
                            return@withContext cachedResult(sourceUrl, AudioDownloadStatus.USED_CACHE_AFTER_FAILURE)
                        }
                    }
                }

                if (!response.isSuccessful) {
                    error("Failed to load audiothek data from $sourceUrl: HTTP ${response.code}")
                }

                val bodyBytes = response.body.bytes()
                val cachedBodyBytes = cache.readCachedAudioBytes()
                if (cachedBodyBytes != null && Arrays.equals(cachedBodyBytes, bodyBytes)) {
                    logger.info("Audiothek unchanged, downloaded content matches cached audio file for {}", sourceUrl)
                    return@withContext cachedResult(sourceUrl, AudioDownloadStatus.NOT_MODIFIED)
                }

                cache.write(
                    sourceUrl = sourceUrl,
                    eTag = response.header("ETag"),
                    lastModified = response.header("Last-Modified"),
                    body = bodyBytes
                )

                AudioLoadResult(
                    dataset = parseAudioDataset(sourceUrl, ByteArrayInputStream(bodyBytes)),
                    downloadStatus = AudioDownloadStatus.DOWNLOADED
                )
            }
        } catch (error: Exception) {
            if (useCachedOnDownloadFailure && cache.hasCachedAudio()) {
                logger.warn("Audiothek download failed for {}, using cached audio file", sourceUrl, error)
                return@withContext cachedResult(sourceUrl, AudioDownloadStatus.USED_CACHE_AFTER_FAILURE)
            }
            throw error
        }
    }

    private fun cachedResult(sourceUrl: String, status: AudioDownloadStatus) = AudioLoadResult(
        dataset = loadCachedDataset(sourceUrl),
        downloadStatus = status
    )

    private fun loadCachedDataset(sourceUrl: String): AudioDataset =
        cache.openCachedAudio().use { parseAudioDataset(sourceUrl, it) }

    private fun parseAudioDataset(sourceUrl: String, body: InputStream): AudioDataset =
        openPayloadStream(sourceUrl, body).use { parser.parse(it, sourceUrl) }

    private fun openPayloadStream(sourceUrl: String, body: InputStream): InputStream =
        if (sourceUrl.endsWith(".xz")) XZInputStream(body) else body
}

data class AudioLoadResult(
    val dataset: AudioDataset,
    val downloadStatus: AudioDownloadStatus
)

enum class AudioDownloadStatus {
    DOWNLOADED,
    NOT_MODIFIED,
    USED_CACHE_AFTER_FAILURE
}
