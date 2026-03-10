package mediathek.swingaudiothek.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import mediathek.swingaudiothek.model.AudioDataset
import okhttp3.OkHttpClient
import okhttp3.Request
import org.tukaani.xz.XZInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.time.Duration

class AudioRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(Duration.ofSeconds(20))
        .build(),
    private val resolver: AudioSourceResolver = AudioSourceResolver(client),
    private val parser: AudioParser = AudioParser(),
    private val cache: AudioDownloadCache = AudioDownloadCache()
) {
    suspend fun loadAudiothek(): AudioDataset = withContext(Dispatchers.IO) {
        val sourceUrl = resolver.resolveSourceUrl()
        val cachedMetadata = cache.readMetadata()
        val requestBuilder = Request.Builder().url(sourceUrl)
        if (cachedMetadata?.sourceUrl == sourceUrl) {
            cachedMetadata.eTag?.let { requestBuilder.header("If-None-Match", it) }
            cachedMetadata.lastModified?.let { requestBuilder.header("If-Modified-Since", it) }
        }
        val request = requestBuilder.get().build()

        client.newCall(request).execute().use { response ->
            when (response.code) {
                304 -> {
                    if (!cache.hasCachedAudio()) {
                        error("Audiothek cache miss after HTTP 304 for $sourceUrl")
                    }
                    return@withContext loadCachedDataset(sourceUrl)
                }

                !in 200..299 -> {
                    if (cache.hasCachedAudio()) {
                        return@withContext loadCachedDataset(sourceUrl)
                    }
                }
            }

            if (!response.isSuccessful) {
                error("Failed to load audiothek data from $sourceUrl: HTTP ${response.code}")
            }

            val bodyBytes = response.body.bytes()
            cache.write(
                sourceUrl = sourceUrl,
                eTag = response.header("ETag"),
                lastModified = response.header("Last-Modified"),
                body = bodyBytes
            )

            parseAudioDataset(sourceUrl, ByteArrayInputStream(bodyBytes))
        }
    }

    private fun loadCachedDataset(sourceUrl: String): AudioDataset =
        cache.openCachedAudio().use { parseAudioDataset(sourceUrl, it) }

    private fun parseAudioDataset(sourceUrl: String, body: InputStream): AudioDataset =
        openPayloadStream(sourceUrl, body).use { parser.parse(it, sourceUrl) }

    private fun openPayloadStream(sourceUrl: String, body: InputStream): InputStream =
        if (sourceUrl.endsWith(".xz")) XZInputStream(body) else body
}
