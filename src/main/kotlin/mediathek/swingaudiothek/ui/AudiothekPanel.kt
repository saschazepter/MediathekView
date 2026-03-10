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

package mediathek.swingaudiothek.ui

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import mediathek.config.Konstanten
import mediathek.gui.actions.UrlHyperlinkAction
import mediathek.gui.tabs.tab_film.FilmDescriptionPanel
import mediathek.mac.MacMultimediaPlayerLocator
import mediathek.mac.SingleIinaPlayer
import mediathek.swing.OverlayPanel
import mediathek.swingaudiothek.data.AudioDownloadStatus
import mediathek.swingaudiothek.data.AudioLoadResult
import mediathek.swingaudiothek.data.AudioRepository
import mediathek.swingaudiothek.model.AudioEntry
import mediathek.tool.GuiFunktionenProgramme
import org.apache.commons.lang3.SystemUtils
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.*

class AudiothekPanel(
    private val repository: AudioRepository
) : JPanel(BorderLayout(10, 10)) {
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    private var loadJob: Job? = null
    private var ageTickerJob: Job? = null

    private val table = AudiothekTable(
        onOpenAudio = ::openAudioEntry,
        onDownload = ::showDownloadDialog
    )

    private val statusPanel = AudiothekStatusPanel()
    private val detailsPanel = FilmDescriptionPanel()
    private val toolBar = AudiothekToolBar()
    private val tableScrollPane = JScrollPane(table)
    private val errorOverlay = OverlayPanel("Audiothek konnte nicht geladen werden")
    private val tableContainer = JLayeredPane().apply {
        layout = OverlayLayout(this)
        add(errorOverlay)
        add(tableScrollPane)
    }
    private val southPanel = JPanel(VerticalLayout()).apply {
        add(statusPanel)
        add(detailsPanel)
    }

    private var datasetTimestamp: LocalDateTime? = null
    private val iinaPlayer = SingleIinaPlayer()

    init {
        add(toolBar, BorderLayout.NORTH)
        add(tableContainer, BorderLayout.CENTER)
        add(southPanel, BorderLayout.SOUTH)
        errorOverlay.isVisible = false
        setupListeners()
        triggerLoad(isManualReload = false)
    }

    fun disposePanel() {
        uiScope.coroutineContext[Job]?.cancel()
    }

    private fun setupListeners() {
        statusPanel.addReloadListener { triggerLoad(isManualReload = true) }
        table.addEntrySelectionListener {
            if (!it.valueIsAdjusting) {
                detailsPanel.setCurrentAudioEntry(table.selectedEntry())
            }
        }
        toolBar.addFilterSubmitListener(::applyFilterNow)
        table.addComponentListener(object : ComponentAdapter() {
            override fun componentResized(event: ComponentEvent?) {
                errorOverlay.setSize(table.width, table.height)
            }
        })
    }

    private fun triggerLoad(isManualReload: Boolean) {
        loadJob?.cancel()
        loadJob = uiScope.launch {
            setLoadingState(true)
            hideErrorOverlay()
            if (isManualReload) {
                statusPanel.setReloadEnabled(false)
            }

            try {
                runCatching { repository.loadAudiothek() }
                    .onSuccess { handleLoadSuccess(it, isManualReload) }
                    .onFailure { handleLoadFailure(it, isManualReload) }
            } finally {
                if (isManualReload) {
                    statusPanel.setReloadEnabled(true)
                }
                setLoadingState(false)
            }
        }
    }

    private fun handleLoadSuccess(result: AudioLoadResult, isManualReload: Boolean) {
        if (shouldSkipTableRefresh(result, isManualReload)) {
            showReloadMessage(result.downloadStatus)
            return
        }

        datasetTimestamp = result.dataset.metaLocal
        table.setRows(result.dataset.entries)
        applyFilterNow(toolBar.currentQuery())
        statusPanel.setStand("Stand: ${result.dataset.metaLocal?.format(DATASET_TIMESTAMP_FORMAT) ?: "-"}")
        startAgeTicker()
        refreshSelectionState()
        if (isManualReload) {
            showReloadMessage(result.downloadStatus)
        }
    }

    private fun shouldSkipTableRefresh(result: AudioLoadResult, isManualReload: Boolean): Boolean {
        if (!isManualReload) {
            return false
        }
        if (result.downloadStatus == AudioDownloadStatus.DOWNLOADED) {
            return false
        }
        return datasetTimestamp != null
    }

    private fun handleLoadFailure(error: Throwable, isManualReload: Boolean) {
        if (isManualReload && table.rowCount > 0) {
            JOptionPane.showMessageDialog(
                this,
                buildLoadFailureMessage(error),
                Konstanten.PROGRAMMNAME,
                JOptionPane.ERROR_MESSAGE
            )
            return
        }

        table.setRows(emptyList())
        datasetTimestamp = null
        stopAgeTicker()
        showErrorOverlay()
        detailsPanel.setCurrentAudioEntry(null)
        statusPanel.setStand(error.message ?: error::class.java.simpleName)
        statusPanel.setAge("")
        statusPanel.setCount("0 Einträge")
    }

    private fun startAgeTicker() {
        stopAgeTicker()
        updateAgeLabel()
        ageTickerJob = uiScope.launch {
            while (true) {
                delay(1_000)
                updateAgeLabel()
            }
        }
    }

    private fun stopAgeTicker() {
        ageTickerJob?.cancel()
        ageTickerJob = null
    }

    private fun updateAgeLabel() {
        val timestamp = datasetTimestamp
        val age = timestamp?.let { calculateDatasetAge(it) }
        statusPanel.setAge(age?.let { "Alter: ${formatAge(it)}" }.orEmpty())
    }

    private fun calculateDatasetAge(timestamp: LocalDateTime): Duration {
        return Duration.between(timestamp, LocalDateTime.now()).coerceAtLeast(Duration.ZERO)
    }

    private fun formatAge(duration: Duration): String {
        val totalSeconds = duration.seconds.coerceAtLeast(0)
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return "%02d:%02d:%02d".format(hours, minutes, seconds)
    }

    private fun setLoadingState(loading: Boolean) {
        toolBar.setLoading(loading)
    }

    private fun buildLoadFailureMessage(error: Throwable): String {
        val errorMessage = error.message?.takeIf(String::isNotBlank)
        return buildString {
            append("<html>Das Laden ist fehlgeschlagen")
            append(if (errorMessage == null) "." else ":")
            errorMessage?.let {
                append("<br/>").append("<i>").append(it).append("</i>")
            }
            append("</html>")
        }
    }

    private fun showErrorOverlay() {
        errorOverlay.isVisible = true
        tableContainer.repaint()
    }

    private fun hideErrorOverlay() {
        errorOverlay.isVisible = false
        tableContainer.repaint()
    }

    private fun applyFilterNow(query: String) {
        table.applyFilter(query)
        statusPanel.setCount("${table.rowCount} Treffer")
        refreshSelectionState()
    }

    private fun refreshSelectionState() {
        if (table.rowCount > 0) {
            table.selectFirstRow()
            return
        }
        detailsPanel.setCurrentAudioEntry(null)
    }

    private fun openAudioEntry(entry: AudioEntry) {
        entry.audioUrl?.let(::openExternal)
    }

    private fun showDownloadDialog(entry: AudioEntry) {
        val title = entry.title.ifBlank { "(ohne Titel)" }
        JOptionPane.showMessageDialog(
            this,
            "Download:\n$title",
            "Download",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    private fun openExternal(url: URI) {
        runCatching {
            if (!SystemUtils.IS_OS_MAC_OSX) {
                try {
                    val vlcPath = GuiFunktionenProgramme.findExecutableOnPath("vlc")
                    ProcessBuilder(vlcPath.toAbsolutePath().toString(), url.toString()).start()
                } catch (_: IllegalStateException) {
                    JOptionPane.showMessageDialog(
                        this,
                        "<html>Es konnte kein VLC auf dem System gefunden werden.<br/>" +
                            "Es wird versucht, die Datei über den Browser zu öffnen.</html>",
                        Konstanten.PROGRAMMNAME,
                        JOptionPane.INFORMATION_MESSAGE
                    )
                    UrlHyperlinkAction.openURI(url)
                }
            } else {
                MacMultimediaPlayerLocator.findIinaPlayer().ifPresentOrElse({
                    iinaPlayer.play(url.toString())
                }, {
                    MacMultimediaPlayerLocator.findVlcPlayer().ifPresentOrElse({
                        ProcessBuilder("open", "-a", "VLC", url.toString()).start()
                    }, {
                        Desktop.getDesktop().browse(url)
                    })
                })
            }
        }.onFailure {
            JOptionPane.showMessageDialog(
                this,
                "URL konnte nicht geöffnet werden:\n$url",
                Konstanten.PROGRAMMNAME,
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    private fun showReloadMessage(downloadStatus: AudioDownloadStatus) {
        val message = when (downloadStatus) {
            AudioDownloadStatus.DOWNLOADED -> return
            AudioDownloadStatus.NOT_MODIFIED -> "Es konnte keine neue Datei geladen werden.\nDie vorhandene ist bereits aktuell."
            AudioDownloadStatus.USED_CACHE_AFTER_FAILURE -> "Es konnte keine neue Datei geladen werden.\nDie zwischengespeicherte wird weiter verwendet."
        }
        JOptionPane.showMessageDialog(
            this,
            message,
            "Audiothek",
            JOptionPane.INFORMATION_MESSAGE
        )
    }

    companion object {
        private val DATASET_TIMESTAMP_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss")
    }
}
