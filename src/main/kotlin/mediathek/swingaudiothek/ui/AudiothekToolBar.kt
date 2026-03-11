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

import java.awt.Dimension
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class AudiothekToolBar : JToolBar() {
    private val searchField = JTextField(28)
    private val clearSearchButton = JButton("x")
    private val downloadManagerButton = JButton("dlm")
    private val downloadProgressIcon = CircularProgressIcon()

    init {
        isFloatable = false
        searchField.maximumSize = Dimension(500, searchField.preferredSize.height)
        clearSearchButton.isFocusable = false
        clearSearchButton.toolTipText = "Filter löschen"
        clearSearchButton.isEnabled = false
        downloadManagerButton.isFocusable = false
        downloadManagerButton.horizontalTextPosition = SwingConstants.RIGHT
        downloadManagerButton.iconTextGap = 6
        searchField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) = updateClearButtonState()
            override fun removeUpdate(e: DocumentEvent?) = updateClearButtonState()
            override fun changedUpdate(e: DocumentEvent?) = updateClearButtonState()
        })

        add(JLabel("Filter"))
        addSeparator()
        add(searchField)
        add(clearSearchButton)
        addSeparator()
        add(downloadManagerButton)
    }

    fun addFilterSubmitListener(action: (String) -> Unit) {
        searchField.addActionListener { action(searchField.text) }
    }

    fun addDownloadManagerListener(action: () -> Unit) {
        downloadManagerButton.addActionListener { action() }
    }

    fun addClearSearchListener(action: () -> Unit) {
        clearSearchButton.action = object : AbstractAction("x") {
            override fun actionPerformed(e: java.awt.event.ActionEvent?) {
                searchField.text = ""
                action()
            }
        }
        clearSearchButton.toolTipText = "Filter löschen"
        clearSearchButton.isFocusable = false
        updateClearButtonState()
    }

    fun downloadManagerAnchor(): JButton = downloadManagerButton

    fun setDownloadProgress(summary: DownloadSummary) {
        if (summary.activeCount <= 0) {
            downloadManagerButton.icon = null
            downloadManagerButton.text = "dlm"
            downloadManagerButton.toolTipText = null
            return
        }

        downloadProgressIcon.progress = summary.progress
        downloadManagerButton.icon = downloadProgressIcon
        downloadManagerButton.text = "dlm"
        downloadManagerButton.toolTipText = "${(summary.progress * 100).toInt()} %"
        downloadManagerButton.repaint()
    }

    fun setLoading(loading: Boolean) {
        searchField.isEnabled = !loading
        clearSearchButton.isEnabled = !loading && searchField.text.isNotBlank()
    }

    fun currentQuery(): String = searchField.text

    private fun updateClearButtonState() {
        clearSearchButton.isEnabled = searchField.isEnabled && searchField.text.isNotBlank()
    }
}
