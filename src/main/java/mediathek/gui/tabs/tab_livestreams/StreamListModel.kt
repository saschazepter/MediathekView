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

import javax.swing.AbstractListModel

class StreamListModel : AbstractListModel<StreamInfo>() {

    private val streams = mutableListOf<StreamInfo>()

    override fun getSize(): Int = streams.size

    override fun getElementAt(index: Int): StreamInfo = streams[index]

    fun setData(newStreams: List<StreamInfo>) {
        streams.clear()
        streams.addAll(newStreams)
        fireContentsChanged(this, 0, streams.size - 1)
    }

    fun clear() {
        streams.clear()
        fireContentsChanged(this, 0, 0)
    }
}
