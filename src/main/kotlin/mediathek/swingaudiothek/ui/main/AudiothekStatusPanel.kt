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

package mediathek.swingaudiothek.ui.main

import org.jdesktop.swingx.JXStatusBar
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class AudiothekStatusPanel : JXStatusBar() {
    private val sourceLabel = JLabel("Stand: -")
    private val agePanel = AudiothekAgePanel()
    private val activeDownloadsLabel = JLabel("")
    private val countLabel = JLabel("0 Einträge", SwingConstants.RIGHT)
    private val leftPanel = JPanel(FlowLayout(FlowLayout.LEFT, 8, 0)).apply {
        isOpaque = false
        add(sourceLabel)
        add(activeDownloadsLabel)
    }

    init {
        add(leftPanel, Constraint(Constraint.ResizeBehavior.FILL))
        add(agePanel)
        add(countLabel)
        activeDownloadsLabel.isVisible = false
    }

    fun setStand(text: String) {
        sourceLabel.text = text
    }

    fun setStandVisible(visible: Boolean) {
        sourceLabel.isVisible = visible
    }

    fun setAge(text: String) {
        agePanel.setAge(text)
    }

    fun addReloadListener(action: () -> Unit) {
        agePanel.addReloadListener(action)
    }

    fun setReloadEnabled(enabled: Boolean) {
        agePanel.setReloadEnabled(enabled)
    }

    fun setCount(text: String) {
        countLabel.text = text
    }

    fun setActiveDownloads(count: Int) {
        if (count <= 0) {
            activeDownloadsLabel.text = ""
            activeDownloadsLabel.isVisible = false
            return
        }

        activeDownloadsLabel.text = if (count == 1) {
            "1 aktiver Download"
        } else {
            "$count aktive Downloads"
        }
        activeDownloadsLabel.isVisible = true
    }
}
