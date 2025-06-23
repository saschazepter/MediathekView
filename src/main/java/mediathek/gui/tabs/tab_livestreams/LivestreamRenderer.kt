/*
 * Copyright (c) 2025 derreisende77.
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

package mediathek.gui.tabs.tab_livestreams

import java.awt.BorderLayout
import java.awt.Component
import java.time.Instant
import javax.swing.*

class LivestreamRenderer : JPanel(), ListCellRenderer<LivestreamEntry> {

    private val nameLabel = JLabel()
    private val showLabel = JLabel()
    private val progressBar = JProgressBar()

    init {
        layout = BorderLayout(5, 5)
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)

        add(nameLabel, BorderLayout.NORTH)
        add(showLabel, BorderLayout.CENTER)
        add(progressBar, BorderLayout.SOUTH)
    }

    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[\\u00AD\\p{Cf}]"), "")
    }

    override fun getListCellRendererComponent(
        list: JList<out LivestreamEntry>,
        value: LivestreamEntry,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {

        //Parlamentsfernsehen contains soft hyphens...
        nameLabel.text = sanitizeName(value.streamName)
        val show = value.show

        if (show != null && show.startTime.isBefore(Instant.now()) && show.endTime.isAfter(Instant.now())) {
            showLabel.text = "${show.title} - ${show.subtitle}"
            val total = show.endTime.epochSecond - show.startTime.epochSecond
            val elapsed = Instant.now().epochSecond - show.startTime.epochSecond
            progressBar.maximum = total.toInt()
            progressBar.value = elapsed.toInt()
        } else {
            showLabel.text = "Keine Sendung oder außerhalb des Zeitraums"
            progressBar.maximum = 100
            progressBar.value = 0
        }

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground

        return this
    }
}
