package mediathek.swingaudiothek.data

import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.util.*

class AudioDownloadCache(
    private val cacheDir: Path = Path.of("example-audio-data")
) {
    private val audioFile: Path = cacheDir.resolve("audios.xz")
    private val metadataFile: Path = cacheDir.resolve("audiolist-cache.properties")

    fun readMetadata(): AudioCacheMetadata? {
        if (!Files.exists(metadataFile)) {
            return null
        }

        val properties = Properties()
        Files.newInputStream(metadataFile).use(properties::load)
        val sourceUrl = properties.getProperty(KEY_SOURCE_URL)?.takeIf(String::isNotBlank) ?: return null
        return AudioCacheMetadata(
            sourceUrl = sourceUrl,
            eTag = properties.getProperty(KEY_ETAG)?.takeIf(String::isNotBlank),
            lastModified = properties.getProperty(KEY_LAST_MODIFIED)?.takeIf(String::isNotBlank)
        )
    }

    fun write(sourceUrl: String, eTag: String?, lastModified: String?, body: ByteArray) {
        Files.createDirectories(cacheDir)
        Files.write(audioFile, body)

        val properties = Properties().apply {
            setProperty(KEY_SOURCE_URL, sourceUrl)
            eTag?.let { setProperty(KEY_ETAG, it) }
            lastModified?.let { setProperty(KEY_LAST_MODIFIED, it) }
        }
        Files.newOutputStream(metadataFile).use { properties.store(it, "Audio download cache") }
    }

    fun hasCachedAudio(): Boolean = Files.exists(audioFile)

    fun openCachedAudio(): InputStream = Files.newInputStream(audioFile)

    companion object {
        private const val KEY_SOURCE_URL = "sourceUrl"
        private const val KEY_ETAG = "etag"
        private const val KEY_LAST_MODIFIED = "lastModified"
    }
}

data class AudioCacheMetadata(
    val sourceUrl: String,
    val eTag: String?,
    val lastModified: String?
)
