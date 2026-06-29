package mediathek.controller.starter

import mediathek.config.Daten
import mediathek.daten.DatenDownload
import mediathek.daten.DatenFilm
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
    fun removesFinishedButtonDownloads() {
        val finishedButtonDownload = buttonDownload(
            filmUrl = "https://example.invalid/finished",
            downloadUrl = "https://example.invalid/finished.mp4",
            status = StartStatus.FINISHED,
        )
        val runningButtonDownload = buttonDownload(
            filmUrl = "https://example.invalid/running",
            downloadUrl = "https://example.invalid/running.mp4",
            status = StartStatus.RUNNING,
        )
        val finishedManualDownload = download(DownloadRunState().apply { status = StartStatus.FINISHED })
        daten.downloads.buttonQueue.add(finishedButtonDownload)
        daten.downloads.buttonQueue.add(runningButtonDownload)
        daten.downloads.buttonQueue.add(finishedManualDownload)

        assertTrue(daten.downloads.cleanupFinishedButtonDownloads())

        assertEquals(listOf(runningButtonDownload, finishedManualDownload), daten.downloads.buttonQueue)
    }

    @Test
    fun reportsNoFinishedButtonDownloads() {
        val runningButtonDownload = buttonDownload(
            filmUrl = "https://example.invalid/running",
            downloadUrl = "https://example.invalid/running.mp4",
            status = StartStatus.RUNNING,
        )
        daten.downloads.buttonQueue.add(runningButtonDownload)

        assertFalse(daten.downloads.cleanupFinishedButtonDownloads())

        assertEquals(listOf(runningButtonDownload), daten.downloads.buttonQueue)
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

    @Test
    fun buildsDownloadStartInfo() {
        val initialized = download(DownloadRunState().apply { status = StartStatus.INITIALIZED })
        val runningAbo = download(DownloadRunState().apply { status = StartStatus.RUNNING }).apply {
            quelle = DownloadSource.ABO
            aboName = "Abo"
        }
        val finishedDeferred = download(DownloadRunState().apply { status = StartStatus.FINISHED }).apply {
            isDeferred = true
        }
        val buttonDownload = download(DownloadRunState().apply { status = StartStatus.RUNNING }).apply {
            quelle = DownloadSource.BUTTON
        }
        daten.downloads.queue.add(initialized)
        daten.downloads.queue.add(runningAbo)
        daten.downloads.queue.add(finishedDeferred)
        daten.downloads.queue.add(buttonDownload)

        val info = daten.downloads.startInfo()

        assertEquals(4, info.total_num_download_list_entries)
        assertEquals(3, info.total_starts)
        assertEquals(1, info.num_abos)
        assertEquals(3, info.num_downloads)
        assertEquals(1, info.initialized)
        assertEquals(1, info.running)
        assertEquals(1, info.finished)
        assertEquals(0, info.error)
    }

    @Test
    fun reconnectsDownloadsToLoadedFilms() {
        val film = DatenFilm().apply {
            urlNormalQuality = "https://example.invalid/video.mp4"
        }
        val matchingDownload = DatenDownload().apply {
            downloadUrl = film.urlNormalQuality
        }
        val existingFilm = DatenFilm().apply {
            urlNormalQuality = "https://example.invalid/existing.mp4"
        }
        val alreadyConnectedDownload = DatenDownload().apply {
            this.film = existingFilm
            downloadUrl = film.urlNormalQuality
        }
        daten.filmCatalog.allFilms.add(film)
        daten.downloads.queue.add(matchingDownload)
        daten.downloads.queue.add(alreadyConnectedDownload)

        daten.downloads.reconnectFilms()

        assertSame(film, matchingDownload.film)
        assertSame(existingFilm, alreadyConnectedDownload.film)
    }

    @Test
    fun refreshAboDownloadsRemovesResetsAndClearsDeferredEntries() {
        val unstartedAbo = aboDownload(null)
        val erroredAbo = aboDownload(DownloadRunState().apply { status = StartStatus.ERROR })
        val runningAbo = aboDownload(DownloadRunState().apply { status = StartStatus.RUNNING }).apply {
            isDeferred = true
        }
        val interruptedAbo = aboDownload(DownloadRunState().apply { status = StartStatus.RUNNING }).apply {
            isInterruptedFlag = true
        }
        val manualDownload = download(DownloadRunState().apply { status = StartStatus.INITIALIZED }).apply {
            isDeferred = true
        }
        daten.downloads.queue.add(unstartedAbo)
        daten.downloads.queue.add(erroredAbo)
        daten.downloads.queue.add(runningAbo)
        daten.downloads.queue.add(interruptedAbo)
        daten.downloads.queue.add(manualDownload)

        daten.downloads.refreshAboDownloads()

        assertEquals(listOf(erroredAbo, runningAbo, interruptedAbo, manualDownload), daten.downloads.queue)
        assertNull(erroredAbo.runtime.runState)
        assertFalse(runningAbo.isDeferred)
        assertTrue(interruptedAbo.isInterrupted)
        assertFalse(manualDownload.isDeferred)
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

    private fun aboDownload(runState: DownloadRunState?): DatenDownload =
        DatenDownload().apply {
            quelle = DownloadSource.ABO
            aboName = "Abo"
            runtime.runState = runState
        }
}
