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

package mediathek.gui.tabs.tab_film.selection

import mediathek.config.Daten
import mediathek.daten.DatenPset
import mediathek.gui.tabs.tab_film.actions.FilmActionHostAdapter
import mediathek.gui.tabs.tab_film.actions.FilmActionHost
import mediathek.gui.tabs.tab_film.bookmark.FilmBookmarkController
import mediathek.gui.tabs.tab_film.bookmark.FilmBookmarkHostAdapter
import mediathek.mainwindow.MediathekGui
import mediathek.tool.table.MVFilmTable
import java.awt.Component

class FilmControllerSetup(
    val selectionController: FilmSelectionController,
    val bookmarkController: FilmBookmarkController,
    val filmActionHost: FilmActionHost,
    val saveSelectedFilm: (DatenPset?) -> Unit,
) {
    companion object {
        fun create(
            table: () -> MVFilmTable,
            tableOrNull: () -> MVFilmTable?,
            parentComponent: Component,
            mediathekGui: MediathekGui,
            daten: () -> Daten,
            showHighQualityOnly: () -> Boolean,
            saveLock: Any,
            repaintOwner: () -> Unit,
            toggleFilterDialogVisibility: () -> Unit,
        ): FilmControllerSetup {
            val selectionHost = FilmSelectionHostAdapter(
                table,
                tableOrNull,
                parentComponent,
                mediathekGui,
                daten,
                showHighQualityOnly,
            )
            val selectionController = FilmSelectionController(selectionHost)
            val bookmarkHost = FilmBookmarkHostAdapter(mediathekGui, repaintOwner)
            val bookmarkController = FilmBookmarkController(bookmarkHost)
            val saveSelectedFilm = { pset: DatenPset? ->
                synchronized(saveLock) {
                    selectionController.saveFilm(pset)
                }
            }
            val filmActionHost = FilmActionHostAdapter(
                saveSelectedFilm,
                selectionController::getSelectedFilms,
                bookmarkController::updateBookmarkListAndRefresh,
                selectionController::getCurrentlySelectedFilm,
                toggleFilterDialogVisibility,
            )

            return FilmControllerSetup(
                selectionController,
                bookmarkController,
                filmActionHost,
                saveSelectedFilm,
            )
        }
    }
}
