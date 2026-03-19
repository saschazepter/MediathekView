package mediathek.tool

import mediathek.tool.http.MVHttpClient
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Request
import okhttp3.Response
import org.apache.logging.log4j.LogManager
import java.io.IOException

object FileSize {
    const val ONE_MiB = 1_000_000
    const val INVALID_SIZE: Byte = -1
    private val logger = LogManager.getLogger()

    @JvmStatic
    fun getFileLengthFromUrl(url: String, forceFetch: Boolean = false): String {
        val okUrl = url.toHttpUrlOrNull() ?: return ""
        return convertSize(getFileSizeFromUrl(okUrl, forceFetch))
    }

    @JvmStatic
    fun convertSize(byteLength: Long): String {
        return when {
            byteLength > ONE_MiB -> (byteLength / ONE_MiB).toString()
            byteLength > 0 -> "1"
            else -> ""
        }
    }

    @JvmStatic
    fun getContentLength(response: Response): Long {
        val sizeStr = response.headers["Content-Length"] ?: return INVALID_SIZE.toLong()
        return sizeStr.toLongOrNull() ?: INVALID_SIZE.toLong()
    }

    @JvmStatic
    fun getFileSizeFromUrl(url: HttpUrl, forceFetch: Boolean = false): Long {
        if (!url.scheme.startsWith("http") || url.encodedPath.endsWith(".m3u8")) {
            return INVALID_SIZE.toLong()
        }

        val request = Request.Builder().url(url).head().build()
        var respLength = INVALID_SIZE.toLong()

        val fetchSize = forceFetch || ApplicationConfiguration.getConfiguration()
            .getBoolean(ApplicationConfiguration.DOWNLOAD_FETCH_FILE_SIZE, true)
        if (fetchSize) {
            logger.info("Requesting file size for: {}", url)
            try {
                MVHttpClient.getInstance().httpClient.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        respLength = getContentLength(response)
                    }
                }
            } catch (_: IOException) {
            }
        } else {
            logger.info("Skipping file size request due to user setting")
        }

        if (respLength < ONE_MiB) {
            respLength = INVALID_SIZE.toLong()
        }
        return respLength
    }
}
