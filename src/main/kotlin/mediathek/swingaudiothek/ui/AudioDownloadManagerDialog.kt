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
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.nio.file.Path
import javax.swing.*

class AudioDownloadManagerDialog(parent: Frame) : JDialog(parent, "Audio-Downloads", false) {
    private val listModel = DefaultListModel<AudioDownloadItem>()
    private val list = JList(listModel)

    init {
        defaultCloseOperation = HIDE_ON_CLOSE
        list.cellRenderer = AudioDownloadListRenderer()
        list.selectionMode = ListSelectionModel.SINGLE_SELECTION
        list.fixedCellHeight = -1
        list.visibleRowCount = 8
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                handleClick(event)
            }
        })

        add(JScrollPane(list).apply {
            preferredSize = Dimension(620, 320)
            border = BorderFactory.createEmptyBorder()
        }, BorderLayout.CENTER)
        pack()
        setLocationRelativeTo(parent)
    }

    fun addDownload(audioName: String, saveTarget: Path, onCancelRequested: () -> Unit): AudioDownloadHandle {
        val item = AudioDownloadItem(
            audioName = audioName,
            saveTarget = saveTarget,
            onCancelRequested = onCancelRequested,
            onRemoveRequested = { removeItem(it) }
        )
        listModel.addElement(item)
        if (!isVisible) {
            isVisible = true
        }
        toFront()
        list.ensureIndexIsVisible(listModel.size() - 1)
        return AudioDownloadHandleImpl(item) { repaintItem(it) }
    }

    private fun handleClick(event: MouseEvent) {
        val index = list.locationToIndex(event.point)
        if (index < 0) {
            return
        }

        val bounds = list.getCellBounds(index, index) ?: return
        if (!bounds.contains(event.point)) {
            return
        }

        val item = listModel.get(index)
        val relativePoint = Point(event.x - bounds.x, event.y - bounds.y)
        when {
            item.cancelButtonBounds.contains(relativePoint) && item.cancelEnabled -> item.onCancelRequested()
            item.removeButtonBounds.contains(relativePoint) && item.removeEnabled -> item.onRemoveRequested(item)
        }
    }

    private fun removeItem(item: AudioDownloadItem) {
        listModel.removeElement(item)
    }

    private fun repaintItem(item: AudioDownloadItem) {
        val index = listModel.indexOf(item)
        if (index >= 0) {
            list.repaint(list.getCellBounds(index, index))
        }
    }
}

interface AudioDownloadHandle {
    fun setProgress(downloadedBytes: Long, totalBytes: Long?)
    fun markCancelling()
    fun markCompleted()
    fun markCancelled()
    fun markFailed(message: String)
}

private class AudioDownloadHandleImpl(
    private val item: AudioDownloadItem,
    private val onStateChanged: (AudioDownloadItem) -> Unit
) : AudioDownloadHandle {
    override fun setProgress(downloadedBytes: Long, totalBytes: Long?) {
        SwingUtilities.invokeLater {
            item.totalBytes = totalBytes
            item.downloadedBytes = downloadedBytes
            item.status = "Download läuft ..."
            item.progressIndeterminate = totalBytes == null || totalBytes <= 0L
            if (totalBytes != null && totalBytes > 0L) {
                item.progressPercent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
                item.progressText = "${item.progressPercent} % (${formatSize(downloadedBytes)} / ${formatSize(totalBytes)})"
            } else {
                item.progressPercent = 0
                item.progressText = formatSize(downloadedBytes)
            }
            onStateChanged(item)
        }
    }

    override fun markCancelling() {
        SwingUtilities.invokeLater {
            item.status = "Wird abgebrochen ..."
            item.progressIndeterminate = true
            item.progressText = "Abbruch läuft ..."
            item.cancelEnabled = false
            onStateChanged(item)
        }
    }

    override fun markCompleted() {
        SwingUtilities.invokeLater {
            item.status = "Abgeschlossen"
            item.progressIndeterminate = false
            item.progressPercent = 100
            item.progressText = "100 %"
            item.cancelEnabled = false
            item.removeEnabled = true
            onStateChanged(item)
        }
    }

    override fun markCancelled() {
        SwingUtilities.invokeLater {
            item.status = "Abgebrochen"
            item.progressIndeterminate = false
            item.progressText = "Abgebrochen"
            item.cancelEnabled = false
            item.removeEnabled = true
            onStateChanged(item)
        }
    }

    override fun markFailed(message: String) {
        SwingUtilities.invokeLater {
            item.status = message
            item.progressIndeterminate = false
            item.progressText = "Fehlgeschlagen"
            item.cancelEnabled = false
            item.removeEnabled = true
            onStateChanged(item)
        }
    }

    private fun formatSize(bytes: Long): String {
        if (bytes < 1024L) {
            return "$bytes B"
        }
        val units = listOf("KB", "MB", "GB")
        var value = bytes.toDouble() / 1024.0
        var unitIndex = 0
        while (value >= 1024.0 && unitIndex < units.lastIndex) {
            value /= 1024.0
            unitIndex++
        }
        return String.format("%.1f %s", value, units[unitIndex])
    }
}

private data class AudioDownloadItem(
    val audioName: String,
    val saveTarget: Path,
    val onCancelRequested: () -> Unit,
    val onRemoveRequested: (AudioDownloadItem) -> Unit,
    var status: String = "Wartet auf Start ...",
    var progressText: String = "0 %",
    var progressPercent: Int = 0,
    var progressIndeterminate: Boolean = false,
    var cancelEnabled: Boolean = true,
    var removeEnabled: Boolean = false,
    var downloadedBytes: Long = 0L,
    var totalBytes: Long? = null,
    var cancelButtonBounds: Rectangle = Rectangle(),
    var removeButtonBounds: Rectangle = Rectangle()
)

private class AudioDownloadListRenderer : JPanel(), ListCellRenderer<AudioDownloadItem> {
    private val borderColor = UIManager.getColor("Component.borderColor") ?: Color.LIGHT_GRAY
    private val titleLabel = JLabel()
    private val pathLabel = JLabel()
    private val statusLabel = JLabel()
    private val progressBar = JProgressBar(0, 100).apply {
        isStringPainted = true
    }
    private var item: AudioDownloadItem? = null

    init {
        layout = BorderLayout(8, 8)
        border = BorderFactory.createEmptyBorder(6, 6, 6, 6)

        val content = JPanel(GridBagLayout()).apply {
            isOpaque = false
        }
        val gbc = GridBagConstraints().apply {
            insets = Insets(2, 2, 2, 2)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
        }

        gbc.gridx = 0
        gbc.gridy = 0
        gbc.weightx = 0.0
        content.add(JLabel("Name:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        content.add(titleLabel, gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        content.add(JLabel("Speicherort:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        content.add(pathLabel, gbc)

        gbc.gridx = 0
        gbc.gridy = 2
        gbc.weightx = 0.0
        content.add(JLabel("Status:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        content.add(statusLabel, gbc)

        gbc.gridx = 0
        gbc.gridy = 3
        gbc.gridwidth = 2
        gbc.weightx = 1.0
        content.add(progressBar, gbc)

        add(content, BorderLayout.CENTER)
    }

    override fun getListCellRendererComponent(
        list: JList<out AudioDownloadItem>,
        value: AudioDownloadItem,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        item = value
        titleLabel.text = value.audioName
        pathLabel.text = value.saveTarget.toAbsolutePath().toString()
        statusLabel.text = value.status
        progressBar.isIndeterminate = value.progressIndeterminate
        progressBar.value = value.progressPercent
        progressBar.string = value.progressText

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground
        titleLabel.foreground = foreground
        pathLabel.foreground = foreground
        statusLabel.foreground = foreground
        progressBar.foreground = UIManager.getColor("ProgressBar.foreground")
        isOpaque = true
        return this
    }

    override fun paintComponent(g: Graphics) {
        super.paintComponent(g)
        val currentItem = item ?: return
        val graphics = g.create() as Graphics2D
        try {
            graphics.color = borderColor
            graphics.drawRoundRect(0, 0, width - 1, height - 1, 10, 10)
            paintButtons(graphics, currentItem)
        } finally {
            graphics.dispose()
        }
    }

    private fun paintButtons(graphics: Graphics2D, item: AudioDownloadItem) {
        val buttonWidth = 90
        val buttonHeight = 28
        val gap = 8
        val x = width - buttonWidth - 12
        val cancelY = 18
        val removeY = cancelY + buttonHeight + gap

        item.cancelButtonBounds = Rectangle(x, cancelY, buttonWidth, buttonHeight)
        item.removeButtonBounds = Rectangle(x, removeY, buttonWidth, buttonHeight)

        paintButton(graphics, item.cancelButtonBounds, "Abbrechen", item.cancelEnabled)
        paintButton(graphics, item.removeButtonBounds, "Entfernen", item.removeEnabled)
    }

    private fun paintButton(graphics: Graphics2D, bounds: Rectangle, text: String, enabled: Boolean) {
        val bg = when {
            enabled -> UIManager.getColor("Button.background") ?: Color(230, 230, 230)
            else -> UIManager.getColor("Button.disabledBackground") ?: Color(245, 245, 245)
        }
        val fg = when {
            enabled -> UIManager.getColor("Button.foreground") ?: Color.BLACK
            else -> UIManager.getColor("Button.disabledText") ?: Color.GRAY
        }
        graphics.color = bg
        graphics.fillRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8)
        graphics.color = borderColor
        graphics.drawRoundRect(bounds.x, bounds.y, bounds.width, bounds.height, 8, 8)
        graphics.color = fg
        val fm = graphics.fontMetrics
        val textX = bounds.x + (bounds.width - fm.stringWidth(text)) / 2
        val textY = bounds.y + (bounds.height - fm.height) / 2 + fm.ascent
        graphics.drawString(text, textX, textY)
    }

    override fun getPreferredSize(): Dimension {
        return Dimension(super.getPreferredSize().width, 118)
    }

    override fun doLayout() {
        super.doLayout()
        getComponent(0).setBounds(8, 8, width - 124, height - 16)
    }
}
