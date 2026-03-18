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

import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import java.awt.Color
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.UIManager

class AudiothekAgePanel : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {
    private val ageLabel = JLabel("")
    private val reloadLabel = JLabel()
    private val reloadEnabledIcon = FontIcon.of(FontAwesomeSolid.RECYCLE, 14)
    private val reloadDisabledIcon = FontIcon.of(
        FontAwesomeSolid.RECYCLE,
        14,
        UIManager.getColor("Button.disabledText") ?: Color.GRAY
    )
    private var reloadAction: (() -> Unit)? = null

    init {
        isOpaque = false

        reloadLabel.icon = reloadEnabledIcon
        reloadLabel.disabledIcon = reloadDisabledIcon
        reloadLabel.toolTipText = "Neu laden"
        reloadLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        reloadLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (reloadLabel.isEnabled) {
                    reloadAction?.invoke()
                }
            }
        })

        add(ageLabel)
        add(reloadLabel)
    }

    fun setAge(text: String) {
        ageLabel.text = text
    }

    fun addReloadListener(action: () -> Unit) {
        reloadAction = action
    }

    fun setReloadEnabled(enabled: Boolean) {
        reloadLabel.isEnabled = enabled
        reloadLabel.cursor = if (enabled) {
            Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        } else {
            Cursor.getDefaultCursor()
        }
    }
}
