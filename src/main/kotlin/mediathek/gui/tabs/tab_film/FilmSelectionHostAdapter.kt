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

package mediathek.gui.tabs.tab_film

import mediathek.config.Daten
import mediathek.mainwindow.MediathekGui
import mediathek.tool.table.MVFilmTable
import java.awt.Component
import java.util.function.Supplier

class FilmSelectionHostAdapter(
    private val table: Supplier<MVFilmTable>,
    private val tableOrNull: Supplier<MVFilmTable?>,
    private val parentComponent: Component,
    private val mediathekGui: MediathekGui,
    private val daten: Supplier<Daten>,
    private val showHighQualityOnly: Supplier<Boolean>,
) : FilmSelectionController.Host {
    override fun table(): MVFilmTable = table.get()

    override fun tableOrNull(): MVFilmTable? = tableOrNull.get()

    override fun parentComponent(): Component = parentComponent

    override fun mediathekGui(): MediathekGui = mediathekGui

    override fun daten(): Daten = daten.get()

    override fun showHighQualityOnly(): Boolean = showHighQualityOnly.get()
}
