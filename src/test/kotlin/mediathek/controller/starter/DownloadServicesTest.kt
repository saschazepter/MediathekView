package mediathek.controller.starter

import mediathek.config.Daten
import mediathek.daten.DatenDownload
import mediathek.daten.DownloadSource
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class DownloadServicesTest {
    private lateinit var daten: Daten

    @BeforeEach
    fun setUp() {
        daten = Daten()
    }

    @AfterEach
    fun tearDown() {
        daten.downloads.shutdown()
    }

    @Test
    fun cancelsRunningButtonDownloadByFilmUrl() {
        val download = buttonDownload(
            filmUrl = "https://example.invalid/film",
            downloadUrl = "https://example.invalid/download.mp4",
            status = StartStatus.RUNNING,
        )
        daten.downloads.buttonQueue.add(download)

        assertTrue(daten.downloads.cancelRunningButtonDownloadByFilmUrl("https://example.invalid/film"))

        assertNull(download.runtime.runState)
    }

    @Test
    fun ignoresButtonDownloadsThatAreNotRunning() {
        val download = buttonDownload(
            filmUrl = "https://example.invalid/film",
            downloadUrl = "https://example.invalid/download.mp4",
            status = StartStatus.INITIALIZED,
        )
        val runState = download.runtime.runState
        daten.downloads.buttonQueue.add(download)

        assertFalse(daten.downloads.cancelRunningButtonDownloadByFilmUrl("https://example.invalid/film"))

        assertSame(runState, download.runtime.runState)
    }

    @Test
    fun requestsStopForQueuedDownloadsDuringShutdown() {
        val runState = DownloadRunState().apply {
            status = StartStatus.RUNNING
        }
        val download = download(runState)
        daten.downloads.queue.add(download)

        daten.downloads.requestStopForShutdown()

        assertTrue(runState.stoppen)
    }

    @Test
    fun countsOnlyUnfinishedDownloads() {
        daten.downloads.queue.add(download(DownloadRunState().apply { status = StartStatus.RUNNING }))
        daten.downloads.queue.add(download(DownloadRunState().apply { status = StartStatus.FINISHED }))

        assertEquals(1L, daten.downloads.unfinishedDownloads())
    }

    @Test
    fun returnsUnfinishedDownloadsForSource() {
        val manualDownload = download(DownloadRunState().apply { status = StartStatus.RUNNING })
        val aboDownload = download(DownloadRunState().apply { status = StartStatus.INITIALIZED }).apply {
            quelle = DownloadSource.ABO
        }
        val finishedManualDownload = download(DownloadRunState().apply { status = StartStatus.FINISHED })
        daten.downloads.queue.add(manualDownload)
        daten.downloads.queue.add(aboDownload)
        daten.downloads.queue.add(finishedManualDownload)

        assertEquals(listOf(manualDownload), daten.downloads.unfinishedDownloads(DownloadSource.DOWNLOAD))
        assertEquals(listOf(manualDownload, aboDownload), daten.downloads.unfinishedDownloads(DownloadSource.ALL))
    }

    private fun buttonDownload(
        filmUrl: String,
        downloadUrl: String,
        status: StartStatus,
    ): DatenDownload =
        DatenDownload().apply {
            this.filmUrl = filmUrl
            this.downloadUrl = downloadUrl
            quelle = DownloadSource.BUTTON
            runtime.runState = DownloadRunState().apply {
                this.status = status
            }
        }

    private fun download(runState: DownloadRunState): DatenDownload =
        DatenDownload().apply {
            quelle = DownloadSource.DOWNLOAD
            runtime.runState = runState
        }
}
