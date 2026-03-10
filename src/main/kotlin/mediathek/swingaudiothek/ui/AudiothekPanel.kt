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
import mediathek.swingaudiothek.data.AudioDownloadStatus
import mediathek.swingaudiothek.data.AudioLoadResult
import mediathek.swingaudiothek.data.AudioRepository
import mediathek.swingaudiothek.model.AudioDataset
import mediathek.swingaudiothek.model.AudioEntry
import java.awt.BorderLayout
import java.awt.Desktop
import java.net.URI
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JSplitPane

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
    private val detailsPanel = AudiothekDetailsPanel()
    private val toolBar = AudiothekToolBar()
    private val tableScrollPane = JScrollPane(table)
    private val tableMessagePanel = AudiothekTableMessagePanel()

    private var datasetTimestamp: LocalDateTime? = null
    private var manualReloadRunning = false

    init {
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT, tableScrollPane, detailsPanel)
        splitPane.resizeWeight = 0.72

        add(toolBar, BorderLayout.NORTH)
        add(splitPane, BorderLayout.CENTER)
        add(statusPanel, BorderLayout.SOUTH)

        setupListeners()
        triggerLoad(isManualReload = false)
    }

    fun disposePanel() {
        uiScope.coroutineContext[Job]?.cancel()
    }

    private fun showTable() {
        if (tableScrollPane.viewport.view !== table) {
            tableScrollPane.setViewportView(table)
        }
    }

    private fun showTableMessage(message: String) {
        tableMessagePanel.setMessage(message)
        if (tableScrollPane.viewport.view !== tableMessagePanel) {
            tableScrollPane.setViewportView(tableMessagePanel)
        }
    }

    private fun setupListeners() {
        statusPanel.addReloadListener { triggerLoad(isManualReload = true) }
        table.addEntrySelectionListener {
            if (!it.valueIsAdjusting) {
                detailsPanel.showEntry(table.selectedEntry())
            }
        }
        toolBar.addFilterSubmitListener(::applyFilterNow)
    }

    private fun triggerLoad(isManualReload: Boolean) {
        loadJob?.cancel()
        loadJob = uiScope.launch {
            setLoadingState(true)
            if (shouldShowLoadingMessage(isManualReload)) {
                showTableMessage("Audiothek wird geladen ...")
            }
            if (isManualReload) {
                manualReloadRunning = true
                statusPanel.setReloadEnabled(false)
            }

            try {
                runCatching { repository.loadAudiothek() }
                    .onSuccess { handleLoadSuccess(it, isManualReload) }
                    .onFailure(::handleLoadFailure)
            } finally {
                if (isManualReload) {
                    manualReloadRunning = false
                    statusPanel.setReloadEnabled(true)
                }
                setLoadingState(false)
            }
        }
    }

    private fun handleLoadSuccess(result: AudioLoadResult, isManualReload: Boolean) {
        if (shouldSkipTableRefresh(result, isManualReload)) {
            showTable()
            showReloadMessage(result.downloadStatus)
            return
        }

        val dataset: AudioDataset = result.dataset
        datasetTimestamp = dataset.metaLocal
        table.setRows(dataset.entries)
        showTable()
        applyFilterNow(toolBar.currentQuery())
        statusPanel.setStand("Stand: ${dataset.metaLocal?.format(DATASET_TIMESTAMP_FORMAT) ?: "-"}")
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

    private fun shouldShowLoadingMessage(isManualReload: Boolean): Boolean {
        if (!isManualReload) {
            return true
        }
        return datasetTimestamp == null || table.rowCount == 0
    }

    private fun handleLoadFailure(error: Throwable) {
        table.setRows(emptyList())
        datasetTimestamp = null
        stopAgeTicker()
        showTableMessage(error.message ?: "Laden fehlgeschlagen")
        detailsPanel.showEntry(null)
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
        if (!manualReloadRunning) {
            statusPanel.setReloadEnabled(true)
        }
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
        detailsPanel.showEntry(null)
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
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(url)
            } else {
                JOptionPane.showMessageDialog(
                    this,
                    "Desktop-Integration ist nicht verfugbar.",
                    Konstanten.PROGRAMMNAME,
                    JOptionPane.ERROR_MESSAGE
                )
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
