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

import java.awt.*
import java.nio.file.Path
import javax.swing.*
import kotlin.math.roundToInt

private const val DOWNLOAD_ROW_PREFERRED_WIDTH = 600
private const val DOWNLOAD_PANEL_INSET = 5
private const val DOWNLOAD_PANEL_PREFERRED_WIDTH = DOWNLOAD_ROW_PREFERRED_WIDTH + (DOWNLOAD_PANEL_INSET * 2)

class AudioDownloadManagerPanel : JPanel(BorderLayout()) {
    private val contentPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        isOpaque = false
        border = BorderFactory.createEmptyBorder(
            DOWNLOAD_PANEL_INSET,
            DOWNLOAD_PANEL_INSET,
            DOWNLOAD_PANEL_INSET,
            DOWNLOAD_PANEL_INSET
        )
    }
    private val scrollPane = JScrollPane(contentPanel)
    private var emptyListener: (() -> Unit)? = null
    private var progressListener: ((DownloadSummary) -> Unit)? = null
    private var primaryActionListener: ((String) -> Unit)? = null
    private var secondaryActionListener: ((String) -> Unit)? = null
    private var previousTaskIds: List<String> = emptyList()
    private var currentItems: List<AudioDownloadItem> = emptyList()
    private val rowPanels = LinkedHashMap<String, AudioDownloadRowPanel>()

    init {
        preferredSize = Dimension(DOWNLOAD_PANEL_PREFERRED_WIDTH, 320)
        minimumSize = Dimension(DOWNLOAD_PANEL_PREFERRED_WIDTH, 160)
        val popoverBackground = UIManager.getColor("Panel.background") ?: background
        background = popoverBackground
        isOpaque = true
        contentPanel.background = popoverBackground

        scrollPane.border = BorderFactory.createEmptyBorder()
        scrollPane.viewport.background = popoverBackground
        scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_NEVER

        add(scrollPane, BorderLayout.CENTER)
    }

    fun setTasks(tasks: List<AudioDownloadTaskSnapshot>) {
        val taskIds = tasks.map { it.id }
        val shouldScrollToEnd = taskIds.size > previousTaskIds.size || taskIds.lastOrNull() != previousTaskIds.lastOrNull()
        previousTaskIds = taskIds
        currentItems = tasks.map { it.toListItem() }

        val removedIds = rowPanels.keys - taskIds.toSet()
        removedIds.forEach(rowPanels::remove)

        currentItems.forEach { item ->
            val rowPanel = rowPanels[item.id]
            if (rowPanel == null) {
                rowPanels[item.id] = AudioDownloadRowPanel(
                    item,
                    primaryActionListener,
                    secondaryActionListener
                )
            } else {
                rowPanel.update(item)
            }
        }

        contentPanel.removeAll()
        currentItems.forEachIndexed { index, item ->
            contentPanel.add(rowPanels.getValue(item.id))
            if (index < currentItems.lastIndex) {
                contentPanel.add(Box.createVerticalStrut(8))
            }
        }
        contentPanel.add(Box.createVerticalGlue())
        contentPanel.revalidate()
        contentPanel.repaint()

        notifyProgressChanged()

        if (shouldScrollToEnd) {
            SwingUtilities.invokeLater {
                scrollPane.verticalScrollBar.value = scrollPane.verticalScrollBar.maximum
            }
        }
        if (currentItems.isEmpty()) {
            emptyListener?.invoke()
        }
    }

    fun addEmptyListener(listener: () -> Unit) {
        emptyListener = listener
    }

    fun addProgressListener(listener: (DownloadSummary) -> Unit) {
        progressListener = listener
        notifyProgressChanged()
    }

    fun addPrimaryActionListener(listener: (String) -> Unit) {
        primaryActionListener = listener
    }

    fun addSecondaryActionListener(listener: (String) -> Unit) {
        secondaryActionListener = listener
    }

    private fun notifyProgressChanged() {
        progressListener?.invoke(
            DownloadSummary(
                activeCount = currentItems.count { it.state == AudioDownloadTaskState.DOWNLOADING },
                progress = calculateAggregateProgress(currentItems)
            )
        )
    }

    private fun calculateAggregateProgress(items: List<AudioDownloadItem>): Double {
        val activeItems = items.filter {
            it.state == AudioDownloadTaskState.DOWNLOADING &&
                !it.progressIndeterminate &&
                (it.totalBytes ?: 0L) > 0L
        }
        if (activeItems.isEmpty()) {
            return 0.0
        }

        val totalBytes = activeItems.sumOf { it.totalBytes ?: 0L }
        if (totalBytes <= 0L) {
            return 0.0
        }

        val downloadedBytes = activeItems.sumOf { it.downloadedBytes.coerceAtMost(it.totalBytes ?: 0L) }
        return downloadedBytes.toDouble() / totalBytes.toDouble()
    }
}

data class DownloadSummary(
    val activeCount: Int,
    val progress: Double
)

private data class AudioDownloadItem(
    val id: String,
    val state: AudioDownloadTaskState,
    val audioName: String,
    val saveTarget: Path,
    val status: String,
    val progressText: String,
    val progressPercent: Int,
    val progressIndeterminate: Boolean,
    val primaryLabel: String,
    val primaryEnabled: Boolean,
    val secondaryLabel: String,
    val secondaryEnabled: Boolean,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long? = null
)

private class AudioDownloadRowPanel(
    item: AudioDownloadItem,
    primaryActionListener: ((String) -> Unit)?,
    secondaryActionListener: ((String) -> Unit)?
) : JPanel(GridBagLayout()) {
    private var currentItem: AudioDownloadItem = item
    private val nameValueLabel = JLabel()
    private val statusValueLabel = JLabel()
    private val progressBar = JProgressBar(0, 100)
    private val primaryButton = JButton()
    private val secondaryButton = JButton()

    init {
        val borderColor = UIManager.getColor("Component.borderColor") ?: Color.LIGHT_GRAY
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(borderColor, 1, true),
            BorderFactory.createEmptyBorder(10, 12, 10, 12)
        )
        background = UIManager.getColor("Panel.background") ?: background
        preferredSize = Dimension(DOWNLOAD_ROW_PREFERRED_WIDTH, 96)
        maximumSize = Dimension(Int.MAX_VALUE, 96)
        alignmentX = LEFT_ALIGNMENT

        progressBar.isStringPainted = true
        primaryButton.addActionListener { primaryActionListener?.invoke(currentItem.id) }
        secondaryButton.addActionListener { secondaryActionListener?.invoke(currentItem.id) }

        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 2, 2, 2)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
        }

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        add(JLabel("Name:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        add(nameValueLabel, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        add(JLabel("Status:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        add(statusValueLabel, gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        add(progressBar, gbc)

        val buttonPanel = JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            isOpaque = false
            add(primaryButton)
            add(Box.createVerticalStrut(8))
            add(secondaryButton)
        }

        gbc.gridx = 2
        gbc.gridy = 0
        gbc.gridheight = 3
        gbc.gridwidth = 1
        gbc.weightx = 0.0
        gbc.fill = GridBagConstraints.NONE
        gbc.anchor = GridBagConstraints.NORTHEAST
        gbc.insets = Insets(2, 12, 2, 2)
        add(buttonPanel, gbc)

        update(item)
    }

    fun update(item: AudioDownloadItem) {
        currentItem = item
        nameValueLabel.text = ellipsize(item.audioName, 90)
        nameValueLabel.toolTipText = item.audioName
        statusValueLabel.text = ellipsize(item.status, 80)
        statusValueLabel.toolTipText = item.status
        progressBar.isIndeterminate = item.progressIndeterminate
        progressBar.value = item.progressPercent
        progressBar.string = item.progressText

        primaryButton.text = item.primaryLabel
        primaryButton.isEnabled = item.primaryEnabled
        primaryButton.isVisible = item.primaryLabel.isNotBlank()

        secondaryButton.text = item.secondaryLabel
        secondaryButton.isEnabled = item.secondaryEnabled
        secondaryButton.isVisible = item.secondaryLabel.isNotBlank()

        revalidate()
        repaint()
    }

    private fun ellipsize(text: String, maxChars: Int): String {
        if (text.length <= maxChars) {
            return text
        }
        return text.take(maxChars - 3) + "..."
    }
}

private fun AudioDownloadTaskSnapshot.toListItem(): AudioDownloadItem {
    val progressPercent = totalBytes
        ?.takeIf { it > 0L }
        ?.let { ((downloadedBytes.coerceAtMost(it).toDouble() / it.toDouble()) * 100.0).roundToInt().coerceIn(0, 100) }
        ?: if (state == AudioDownloadTaskState.COMPLETED) 100 else 0
    val progressText = when (state) {
        AudioDownloadTaskState.COMPLETED -> "100 %"
        AudioDownloadTaskState.DOWNLOADING,
        AudioDownloadTaskState.PAUSED -> totalBytes
            ?.takeIf { it > 0L }
            ?.let { "$progressPercent %" }
            ?: formatByteSize(downloadedBytes)
        AudioDownloadTaskState.CANCELLED -> "Abgebrochen"
        AudioDownloadTaskState.FAILED -> "Fehler"
    }

    val statusText = when (state) {
        AudioDownloadTaskState.DOWNLOADING -> buildTransferStatus("Laeuft", downloadedBytes, totalBytes)
        AudioDownloadTaskState.PAUSED -> buildTransferStatus("Pausiert", downloadedBytes, totalBytes)
        AudioDownloadTaskState.COMPLETED -> "Abgeschlossen"
        AudioDownloadTaskState.CANCELLED -> "Abgebrochen"
        AudioDownloadTaskState.FAILED -> sanitizeFailureMessage(errorMessage)
    }

    val primary = when (state) {
        AudioDownloadTaskState.DOWNLOADING -> "Pause" to true
        AudioDownloadTaskState.PAUSED -> "Fortsetzen" to true
        AudioDownloadTaskState.FAILED,
        AudioDownloadTaskState.CANCELLED -> "Neu starten" to true
        AudioDownloadTaskState.COMPLETED -> "" to false
    }
    val secondary = when (state) {
        AudioDownloadTaskState.DOWNLOADING,
        AudioDownloadTaskState.PAUSED -> "Abbrechen" to true
        AudioDownloadTaskState.FAILED,
        AudioDownloadTaskState.CANCELLED,
        AudioDownloadTaskState.COMPLETED -> "Entfernen" to true
    }

    return AudioDownloadItem(
        id = id,
        state = state,
        audioName = audioName,
        saveTarget = Path.of(targetFile),
        status = statusText,
        progressText = progressText,
        progressPercent = progressPercent,
        progressIndeterminate = state == AudioDownloadTaskState.DOWNLOADING && (totalBytes ?: 0L) <= 0L,
        primaryLabel = primary.first,
        primaryEnabled = primary.second,
        secondaryLabel = secondary.first,
        secondaryEnabled = secondary.second,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes
    )
}

private fun sanitizeFailureMessage(errorMessage: String?): String {
    val message = errorMessage?.trim().orEmpty()
    if (message.isBlank()) {
        return "Fehlgeschlagen"
    }
    if (looksLikePath(message)) {
        return "Fehlgeschlagen: Datei konnte nicht gespeichert werden"
    }
    return "Fehlgeschlagen: $message"
}

private fun looksLikePath(message: String): Boolean {
    return message.startsWith("/") ||
        message.startsWith("\\") ||
        Regex("^[A-Za-z]:[\\\\/]").containsMatchIn(message) ||
        (message.contains("/") && !message.startsWith("http")) ||
        message.contains("\\")
}

private fun buildTransferStatus(prefix: String, downloadedBytes: Long, totalBytes: Long?): String {
    val downloaded = formatByteSize(downloadedBytes)
    val total = totalBytes?.takeIf { it > 0L }?.let(::formatByteSize)
    return if (total == null) {
        "$prefix ($downloaded)"
    } else {
        "$prefix ($downloaded / $total)"
    }
}

private fun formatByteSize(bytes: Long): String {
    if (bytes < 1024L) {
        return "$bytes B"
    }
    val units = listOf("KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = -1
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return String.format("%.1f %s", value, units[unitIndex])
}
