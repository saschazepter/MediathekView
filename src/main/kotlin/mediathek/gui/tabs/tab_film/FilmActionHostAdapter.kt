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

import mediathek.daten.DatenFilm
import mediathek.daten.DatenPset
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Supplier

class FilmActionHostAdapter(
    private val saveFilm: Consumer<DatenPset?>,
    private val selectedFilms: Supplier<List<DatenFilm>>,
    private val updateBookmarkListAndRefresh: Consumer<List<DatenFilm>>,
    private val currentlySelectedFilm: Supplier<Optional<DatenFilm>>,
    private val toggleFilterDialogVisibility: Runnable,
) : FilmActionHost {
    override fun saveFilm(pSet: DatenPset?) {
        saveFilm.accept(pSet)
    }

    override fun selectedFilms(): List<DatenFilm> = selectedFilms.get()

    override fun updateBookmarkListAndRefresh(films: List<DatenFilm>) {
        updateBookmarkListAndRefresh.accept(films)
    }

    override fun currentlySelectedFilm(): Optional<DatenFilm> = currentlySelectedFilm.get()

    override fun toggleFilterDialogVisibility() {
        toggleFilterDialogVisibility.run()
    }
}
