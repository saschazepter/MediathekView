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
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.mainwindow.MediathekGui
import mediathek.tool.table.MVFilmTable
import java.awt.Component
import java.util.Optional
import java.util.concurrent.Executor
import java.util.function.BooleanSupplier
import java.util.function.Consumer
import java.util.function.IntFunction
import java.util.function.Supplier
import javax.swing.JScrollPane

class FilmTableReloadHostAdapter(
    private val table: Supplier<MVFilmTable>,
    private val searchFieldData: Supplier<SearchFieldData>,
    private val filterController: FilmFilterController,
    private val tableModelExecutor: Supplier<Executor>,
    private val setSelectionUpdatesSuspended: Consumer<Boolean>,
    private val updateStartInfoProperty: Runnable,
    private val updateFilmData: Runnable,
) : FilmTableReloader.Host {
    override fun table(): MVFilmTable = table.get()

    override fun searchFieldData(): SearchFieldData = searchFieldData.get()

    override fun filterController(): FilmFilterController = filterController

    override fun tableModelExecutor(): Executor = tableModelExecutor.get()

    override fun setSelectionUpdatesSuspended(suspended: Boolean) {
        setSelectionUpdatesSuspended.accept(suspended)
    }

    override fun updateStartInfoProperty() {
        updateStartInfoProperty.run()
    }

    override fun updateFilmData() {
        updateFilmData.run()
    }
}

class TableContextMenuHostAdapter(
    private val table: Supplier<MVFilmTable>,
    private val currentlySelectedFilm: Supplier<Optional<DatenFilm>>,
    private val filmAtRow: IntFunction<Optional<DatenFilm>>,
    private val playSelectedFilm: Runnable,
    private val saveSelectedFilm: Runnable,
    private val startFilmWithPset: Consumer<DatenPset>,
    private val setSelectionUpdatesSuspended: Consumer<Boolean>,
    private val gui: MediathekGui,
    private val actions: Supplier<FilmUiActions>,
) : TableContextMenuHandler.Host {
    override fun table(): MVFilmTable = table.get()

    override fun getCurrentlySelectedFilm(): Optional<DatenFilm> = currentlySelectedFilm.get()

    override fun getFilm(row: Int): Optional<DatenFilm> = filmAtRow.apply(row)

    override fun playSelectedFilm() {
        playSelectedFilm.run()
    }

    override fun saveSelectedFilm() {
        saveSelectedFilm.run()
    }

    override fun startFilmWithPset(pSet: DatenPset) {
        startFilmWithPset.accept(pSet)
    }

    override fun setSelectionUpdatesSuspended(suspended: Boolean) {
        setSelectionUpdatesSuspended.accept(suspended)
    }

    override fun gui(): MediathekGui = gui

    override fun actions(): FilmUiActions = actions.get()
}

class FilmTableInstallerHostAdapter(
    private val table: Supplier<MVFilmTable>,
    private val tableOrNull: Supplier<MVFilmTable?>,
    private val setTable: Consumer<MVFilmTable>,
    private val filmListScrollPane: JScrollPane,
    private val ownerComponent: Component,
    private val tableContextMenuHost: Supplier<TableContextMenuHandler.Host>,
    private val filmActionHost: FilmActionHost,
    private val actions: Supplier<FilmUiActions>,
    private val updateSelectedListItemsCount: Runnable,
    private val onComponentShown: Runnable,
    private val updateFilmData: Runnable,
    private val selectionUpdatesSuspended: BooleanSupplier,
) : FilmTableInstaller.Host {
    override fun table(): MVFilmTable = table.get()

    override fun tableOrNull(): MVFilmTable? = tableOrNull.get()

    override fun setTable(table: MVFilmTable) {
        setTable.accept(table)
    }

    override fun filmListScrollPane(): JScrollPane = filmListScrollPane

    override fun ownerComponent(): Component = ownerComponent

    override fun tableContextMenuHost(): TableContextMenuHandler.Host = tableContextMenuHost.get()

    override fun filmActionHost(): FilmActionHost = filmActionHost

    override fun actions(): FilmUiActions = actions.get()

    override fun updateSelectedListItemsCount() {
        updateSelectedListItemsCount.run()
    }

    override fun onComponentShown() {
        onComponentShown.run()
    }

    override fun updateFilmData() {
        updateFilmData.run()
    }

    override fun selectionUpdatesSuspended(): Boolean = selectionUpdatesSuspended.asBoolean
}
