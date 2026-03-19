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

import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JPanel

class AudiothekAgePanel : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {
    private val ageLabel = JLabel("")

    init {
        isOpaque = false
        add(ageLabel)
    }

    fun setAge(text: String) {
        ageLabel.text = text
    }
}
