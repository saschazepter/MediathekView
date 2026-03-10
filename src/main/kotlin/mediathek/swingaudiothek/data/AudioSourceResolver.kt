package mediathek.swingaudiothek.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.time.Duration
import java.util.concurrent.ThreadLocalRandom
import javax.xml.parsers.DocumentBuilderFactory

class AudioSourceResolver(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .connectTimeout(Duration.ofSeconds(20))
        .build()
) {
    suspend fun resolveSourceUrl(): String = withContext(Dispatchers.IO) {
        val storedUrls = loadStoredUrls()
        if (storedUrls.isNotEmpty()) {
            storedUrls.random()
        } else {
            FALLBACK_URLS[ThreadLocalRandom.current().nextInt(FALLBACK_URLS.size)]
        }
    }

    private fun loadStoredUrls(): List<String> {
        return runCatching {
            val request = Request.Builder()
                .url(STORED_AUDIO_LIST_URL)
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return emptyList()
                }

                val body = response.body.string()
                val builderFactory = DocumentBuilderFactory.newInstance()
                val builder = builderFactory.newDocumentBuilder()
                val document = builder.parse(InputSource(StringReader(body)))
                val nodes = document.getElementsByTagName("*")
                buildList {
                    for (index in 0 until nodes.length) {
                        val node = nodes.item(index)
                        if (node is Element && node.tagName.equals("url", ignoreCase = true)) {
                            val value = node.textContent?.trim().orEmpty()
                            if (value.isNotEmpty()) {
                                add(value)
                            }
                        }
                    }
                }.distinct()
            }
        }.getOrDefault(emptyList())
    }

    companion object {
        private const val STORED_AUDIO_LIST_URL = "https://p2tools.de/download/storedAtList.xml"
        private val FALLBACK_URLS = listOf(
            "https://atlist.de/audios.xz",
            "https://p2atlist.de/audios.xz",
            "https://atlist.eu/audios.xz"
        )
    }
}
