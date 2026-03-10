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

import javax.swing.JLabel
import javax.swing.JTextField
import javax.swing.JToolBar

class AudiothekToolBar : JToolBar() {
    private val searchField = JTextField(28)

    init {
        isFloatable = false

        add(JLabel("Filter"))
        addSeparator()
        add(searchField)
    }

    fun addFilterSubmitListener(action: (String) -> Unit) {
        searchField.addActionListener { action(searchField.text) }
    }

    fun setLoading(loading: Boolean) {
        searchField.isEnabled = !loading
    }

    fun currentQuery(): String = searchField.text
}
