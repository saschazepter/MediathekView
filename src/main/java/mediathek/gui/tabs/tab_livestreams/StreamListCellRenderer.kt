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

import org.jdesktop.swingx.HorizontalLayout
import org.jdesktop.swingx.VerticalLayout
import java.awt.Component
import javax.swing.*

class StreamListCellRenderer : JPanel(), ListCellRenderer<StreamInfo> {

    private val nameLabel = JLabel()
    private val streamUrlLabel = JLabel()

    init {
        isOpaque = true
        layout = HorizontalLayout(5)

        val panel = JPanel()
        panel.isOpaque = false
        panel.layout = VerticalLayout()
        panel.add(nameLabel)
        panel.add(streamUrlLabel)

        add(JLabel("Logo"))
        add(panel)

        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
    }

    override fun getListCellRendererComponent(
        list: JList<out StreamInfo>,
        value: StreamInfo,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {
        nameLabel.text = value.name
        streamUrlLabel.text = value.streamUrl

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground
        nameLabel.foreground = foreground
        streamUrlLabel.foreground = foreground

        return this
    }
}
