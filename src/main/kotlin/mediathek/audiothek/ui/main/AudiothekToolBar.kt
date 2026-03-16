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

package mediathek.audiothek.ui.main

import com.formdev.flatlaf.FlatClientProperties
import mediathek.audiothek.ui.download.CircularProgressIcon
import mediathek.audiothek.ui.download.DownloadSummary
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.materialdesign2.MaterialDesignT
import org.kordamp.ikonli.swing.FontIcon
import java.awt.Dimension
import java.awt.event.KeyEvent
import javax.swing.*
import javax.swing.text.JTextComponent

class AudiothekToolBar : JToolBar() {
    private val searchField = JTextField(28)
    private val helpButton = JButton()
    private val onlineSearchCheckBox = JCheckBox("Online-Suche", true)
    private val settingsButton = JButton()
    private val downloadManagerButton = JButton()
    private val downloadManagerIdleIcon = FontIcon.of(MaterialDesignT.TRAY_ARROW_DOWN, 16)
    private val settingsIcon = FontIcon.of(FontAwesomeSolid.COG, 16)
    private val downloadProgressIcon = CircularProgressIcon()

    init {
        isFloatable = false
        searchField.maximumSize = Dimension(500, searchField.preferredSize.height)
        searchField.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, "Audiothek-Suche")
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_SHOW_CLEAR_BUTTON, true)
        helpButton.isFocusable = false
        configureEmbeddedSearchActions()
        onlineSearchCheckBox.isFocusable = false
        settingsButton.isFocusable = false
        settingsButton.icon = settingsIcon
        downloadManagerButton.isFocusable = false
        downloadManagerButton.icon = downloadManagerIdleIcon
        downloadManagerButton.horizontalTextPosition = SwingConstants.RIGHT
        downloadManagerButton.iconTextGap = 6
        settingsButton.toolTipText = "Audiothek-Einstellungen"

        add(JLabel("Filter"))
        addSeparator()
        add(searchField)
        addSeparator()
        add(onlineSearchCheckBox)
        addSeparator()
        add(settingsButton)
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
        searchField.putClientProperty("JTextField.clearCallback", java.util.function.Consumer<JTextComponent> {
            searchField.text = ""
            action()
        })
        searchField.inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "clearSearchField")
        searchField.actionMap.put("clearSearchField", object : AbstractAction() {
            override fun actionPerformed(event: java.awt.event.ActionEvent?) {
                if (searchField.text.isNotEmpty()) {
                    searchField.text = ""
                    action()
                }
            }
        })
    }

    fun addOnlineSearchToggleListener(action: (Boolean) -> Unit) {
        onlineSearchCheckBox.addActionListener { action(onlineSearchCheckBox.isSelected) }
    }

    fun addSettingsListener(action: () -> Unit) {
        settingsButton.addActionListener { action() }
    }

    fun setHelpAction(action: Action) {
        helpButton.action = action
        helpButton.text = null
        helpButton.toolTipText = action.getValue(Action.SHORT_DESCRIPTION) as? String
        helpButton.isFocusable = false
    }

    fun downloadManagerAnchor(): JButton = downloadManagerButton

    fun isOnlineSearchEnabled(): Boolean = onlineSearchCheckBox.isSelected

    fun setOnlineSearchEnabled(enabled: Boolean) {
        onlineSearchCheckBox.isSelected = enabled
    }

    fun setOnlineSearchAvailable(available: Boolean) {
        onlineSearchCheckBox.isEnabled = available
        onlineSearchCheckBox.toolTipText = if (available) {
            null
        } else {
            "Es ist kein Proxy für die Online-Suche konfiguriert"
        }
    }

    fun setDownloadProgress(summary: DownloadSummary) {
        if (summary.activeCount <= 0) {
            downloadManagerButton.icon = downloadManagerIdleIcon
            downloadManagerButton.toolTipText = null
            return
        }

        downloadProgressIcon.progress = summary.progress
        downloadManagerButton.icon = downloadProgressIcon
        downloadManagerButton.toolTipText = "${(summary.progress * 100).toInt()} %"
        downloadManagerButton.repaint()
    }

    fun setLoading(loading: Boolean) {
        searchField.isEnabled = !loading
        helpButton.isEnabled = !loading
        settingsButton.isEnabled = !loading
    }

    fun currentQuery(): String = searchField.text

    private fun configureEmbeddedSearchActions() {
        val trailingToolbar = JToolBar().apply {
            isFloatable = false
            isOpaque = false
            border = null
            add(helpButton)
        }
        searchField.putClientProperty(FlatClientProperties.TEXT_FIELD_TRAILING_COMPONENT, trailingToolbar)
    }
}
