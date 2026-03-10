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

class AudioDownloadProgressDialog(
    parent: Frame,
    audioName: String,
    saveTarget: Path,
    private val onCancelRequested: () -> Unit
) : JDialog(parent, "Audio-Download", false) {
    private val progressBar = JProgressBar().apply {
        isStringPainted = true
        minimum = 0
        maximum = 100
        value = 0
        string = "0 %"
        preferredSize = Dimension(360, preferredSize.height)
    }
    private val cancelButton = JButton("Abbrechen").apply {
        addActionListener { requestCancel() }
    }

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        addWindowListener(object : java.awt.event.WindowAdapter() {
            override fun windowClosing(e: java.awt.event.WindowEvent?) {
                requestCancel()
            }
        })

        val content = JPanel(GridBagLayout())
        val gbc = GridBagConstraints().apply {
            insets = Insets(6, 8, 6, 8)
            fill = GridBagConstraints.HORIZONTAL
            anchor = GridBagConstraints.WEST
            weightx = 1.0
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
        gbc.gridwidth = 2
        content.add(progressBar, gbc)

        val buttonPanel = JPanel().apply {
            add(cancelButton)
        }

        add(content, BorderLayout.CENTER)
        add(buttonPanel, BorderLayout.SOUTH)
        pack()
        setLocationRelativeTo(parent)
    }

    fun setProgress(downloadedBytes: Long, totalBytes: Long?) {
        if (totalBytes == null || totalBytes <= 0L) {
            progressBar.isIndeterminate = true
            progressBar.string = formatSize(downloadedBytes)
            return
        }

        progressBar.isIndeterminate = false
        val percent = ((downloadedBytes * 100) / totalBytes).toInt().coerceIn(0, 100)
        progressBar.value = percent
        progressBar.string = "$percent % (${formatSize(downloadedBytes)} / ${formatSize(totalBytes)})"
    }

    fun markCancelling() {
        cancelButton.isEnabled = false
        progressBar.isIndeterminate = true
        progressBar.string = "Abbruch läuft ..."
    }

    private fun requestCancel() {
        markCancelling()
        onCancelRequested()
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
