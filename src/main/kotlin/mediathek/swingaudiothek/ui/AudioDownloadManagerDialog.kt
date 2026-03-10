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

class AudioDownloadManagerDialog(parent: Frame) : JDialog(parent, "Audio-Downloads", false) {
    private val downloadsPanel = JPanel().apply {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
    }
    private val rowSpacing = 6

    init {
        defaultCloseOperation = HIDE_ON_CLOSE
        add(JScrollPane(downloadsPanel).apply {
            preferredSize = Dimension(620, 320)
            border = BorderFactory.createEmptyBorder()
        }, BorderLayout.CENTER)
        pack()
        setLocationRelativeTo(parent)
    }

    fun addDownload(audioName: String, saveTarget: Path, onCancelRequested: () -> Unit): AudioDownloadHandle {
        val rowPanel = AudioDownloadRowPanel(audioName, saveTarget, onCancelRequested) { removeDownloadRow(it) }
        downloadsPanel.add(rowPanel)
        downloadsPanel.add(Box.createVerticalStrut(rowSpacing))
        downloadsPanel.revalidate()
        downloadsPanel.repaint()
        if (!isVisible) {
            isVisible = true
        }
        toFront()
        return rowPanel
    }

    private fun removeDownloadRow(rowPanel: AudioDownloadRowPanel) {
        val components = downloadsPanel.components.toList()
        val rowIndex = components.indexOf(rowPanel)
        if (rowIndex < 0) {
            return
        }

        downloadsPanel.remove(rowPanel)
        val spacerIndex = rowIndex.coerceAtMost(downloadsPanel.componentCount - 1)
        if (spacerIndex >= 0) {
            val spacer = downloadsPanel.getComponent(spacerIndex)
            if (spacer is Box.Filler) {
                downloadsPanel.remove(spacerIndex)
            }
        }
        downloadsPanel.revalidate()
        downloadsPanel.repaint()
    }
}

interface AudioDownloadHandle {
    fun setProgress(downloadedBytes: Long, totalBytes: Long?)
    fun markCancelling()
    fun markCompleted()
    fun markCancelled()
    fun markFailed(message: String)
}

private class AudioDownloadRowPanel(
    audioName: String,
    saveTarget: Path,
    private val onCancelRequested: () -> Unit,
    private val onRemoveRequested: (AudioDownloadRowPanel) -> Unit
) : JPanel(BorderLayout(8, 8)), AudioDownloadHandle {
    private val statusLabel = JLabel("Wartet auf Start ...")
    private val progressBar = JProgressBar().apply {
        minimum = 0
        maximum = 100
        isStringPainted = true
        string = "0 %"
    }
    private val cancelButton = JButton("Abbrechen").apply {
        addActionListener {
            isEnabled = false
            markCancelling()
            onCancelRequested()
        }
    }
    private val removeButton = JButton("Entfernen").apply {
        isEnabled = false
        addActionListener { onRemoveRequested(this@AudioDownloadRowPanel) }
    }

    init {
        border = BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIManager.getColor("Component.borderColor")),
            BorderFactory.createEmptyBorder(8, 8, 8, 8)
        )

        val content = JPanel(GridBagLayout())
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
        content.add(JLabel(audioName), gbc)

        gbc.gridx = 0
        gbc.gridy = 1
        gbc.weightx = 0.0
        content.add(JLabel("Speicherort:"), gbc)

        gbc.gridx = 1
        gbc.weightx = 1.0
        content.add(JLabel(saveTarget.toAbsolutePath().toString()), gbc)

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
        add(JPanel().apply {
            layout = BoxLayout(this, BoxLayout.Y_AXIS)
            add(cancelButton)
            add(Box.createVerticalStrut(6))
            add(removeButton)
        }, BorderLayout.EAST)
    }

    override fun setProgress(downloadedBytes: Long, totalBytes: Long?) {
        SwingUtilities.invokeLater {
            if (totalBytes == null || totalBytes <= 0L) {
                progressBar.isIndeterminate = true
                progressBar.string = formatSize(downloadedBytes)
                statusLabel.text = "Download läuft ..."
                return@invokeLater
            }

            progressBar.isIndeterminate = false
            val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
            progressBar.value = percent
            progressBar.string = "$percent % (${formatSize(downloadedBytes)} / ${formatSize(totalBytes)})"
            statusLabel.text = "Download läuft ..."
        }
    }

    override fun markCancelling() {
        SwingUtilities.invokeLater {
            progressBar.isIndeterminate = true
            progressBar.string = "Abbruch läuft ..."
            statusLabel.text = "Wird abgebrochen ..."
        }
    }

    override fun markCompleted() {
        SwingUtilities.invokeLater {
            progressBar.isIndeterminate = false
            progressBar.value = 100
            progressBar.string = "100 %"
            statusLabel.text = "Abgeschlossen"
            showRemoveButton()
        }
    }

    override fun markCancelled() {
        SwingUtilities.invokeLater {
            progressBar.isIndeterminate = false
            progressBar.string = "Abgebrochen"
            statusLabel.text = "Abgebrochen"
            showRemoveButton()
        }
    }

    override fun markFailed(message: String) {
        SwingUtilities.invokeLater {
            progressBar.isIndeterminate = false
            progressBar.string = "Fehlgeschlagen"
            statusLabel.text = message
            showRemoveButton()
        }
    }

    private fun showRemoveButton() {
        cancelButton.isEnabled = false
        removeButton.isEnabled = true
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
