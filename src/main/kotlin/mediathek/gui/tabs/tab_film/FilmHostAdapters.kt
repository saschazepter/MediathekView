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
import mediathek.daten.DatenFilm
import mediathek.daten.DatenPset
import mediathek.mainwindow.MediathekGui
import mediathek.tool.table.MVFilmTable
import java.awt.Component
import java.util.Optional
import java.util.function.Consumer
import java.util.function.Supplier
import javax.swing.JCheckBoxMenuItem
import javax.swing.JTabbedPane

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

class FilmBookmarkHostAdapter(
    private val mediathekGui: MediathekGui,
    private val repaintOwner: Runnable,
) : FilmBookmarkController.Host {
    override fun mediathekGui(): MediathekGui = mediathekGui

    override fun repaintOwner() {
        repaintOwner.run()
    }
}

class SearchFieldHostAdapter(
    private val mediathekGui: MediathekGui,
    private val loadTable: Runnable,
    private val loadTableFromSearchField: Consumer<Boolean>,
) : SearchField.Host {
    override fun mediathekGui(): MediathekGui = mediathekGui

    override fun loadTable() {
        loadTable.run()
    }

    override fun loadTable(fromSearchField: Boolean) {
        loadTableFromSearchField.accept(fromSearchField)
    }
}

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

class FilmViewHostAdapter(
    private val guiFilme: GuiFilme,
    private val psetButtonsTab: JTabbedPane,
    private val psetButtonsPanel: Supplier<PsetButtonsPanel?>,
    private val setPsetButtonsPanel: Consumer<PsetButtonsPanel>,
    private val showButtonsMenuItem: Supplier<JCheckBoxMenuItem>,
    private val showDescriptionMenuItem: Supplier<JCheckBoxMenuItem>,
    private val actions: Supplier<FilmUiActions>,
    private val makeDescriptionTabVisible: Consumer<Boolean>,
) : FilmViewController.Host {
    override fun guiFilme(): GuiFilme = guiFilme

    override fun psetButtonsTab(): JTabbedPane = psetButtonsTab

    override fun psetButtonsPanel(): PsetButtonsPanel? = psetButtonsPanel.get()

    override fun setPsetButtonsPanel(panel: PsetButtonsPanel) {
        setPsetButtonsPanel.accept(panel)
    }

    override fun showButtonsMenuItem(): JCheckBoxMenuItem = showButtonsMenuItem.get()

    override fun showDescriptionMenuItem(): JCheckBoxMenuItem = showDescriptionMenuItem.get()

    override fun actions(): FilmUiActions = actions.get()

    override fun makeDescriptionTabVisible(visible: Boolean) {
        makeDescriptionTabVisible.accept(visible)
    }
}
