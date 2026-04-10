package mediathek.gui.tabs.tab_downloads

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import mediathek.daten.DatenDownload
import org.apache.logging.log4j.LogManager
import java.lang.Runnable
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class DownloadSizeLookupService(
    private val reloadTable: Runnable
) {
    private val logger = LogManager.getLogger(DownloadSizeLookupService::class.java)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO.limitedParallelism(1))
    private val inFlight: MutableSet<DatenDownload> = Collections.newSetFromMap(ConcurrentHashMap())

    fun updateFilmSizes(downloads: List<DatenDownload>) {
        if (downloads.isEmpty()) {
            return
        }

        scope.launch {
            var updateNeeded = false

            for (download in downloads) {
                if (!download.needsLiveSizeLookup() || !inFlight.add(download)) {
                    continue
                }

                try {
                    val oldSize = download.mVFilmSize.size
                    download.queryLiveSize()
                    if (download.mVFilmSize.size != oldSize) {
                        updateNeeded = true
                    }
                } catch (ex: RuntimeException) {
                    logger.debug("Could not update live size for download {}", download.arr[DatenDownload.DOWNLOAD_TITEL], ex)
                } finally {
                    inFlight.remove(download)
                }
            }

            if (updateNeeded) {
                withContext(Dispatchers.Swing) {
                    reloadTable.run()
                }
            }
        }
    }

    private fun DatenDownload.needsLiveSizeLookup(): Boolean =
        film != null && mVFilmSize.size == 0L
}
