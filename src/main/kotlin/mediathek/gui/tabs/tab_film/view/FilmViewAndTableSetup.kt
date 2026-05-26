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

package mediathek.gui.tabs.tab_film.view

import mediathek.daten.DatenFilm
import mediathek.daten.DatenPset
import mediathek.gui.tabs.DescriptionTabController
import mediathek.gui.tabs.tab_film.PsetButtonsPanel
import mediathek.gui.tabs.tab_film.actions.FilmActionHost
import mediathek.gui.tabs.tab_film.actions.FilmUiActions
import mediathek.gui.tabs.tab_film.table.FilmTableInstallerHostAdapter
import mediathek.gui.tabs.tab_film.table.FilmTableInstaller
import mediathek.gui.tabs.tab_film.table.TableContextMenuHostAdapter
import mediathek.mainwindow.MediathekGui
import mediathek.tool.table.MVFilmTable
import java.awt.Component
import java.util.*
import java.util.function.Consumer
import java.util.function.IntFunction
import java.util.function.Supplier
import javax.swing.JCheckBoxMenuItem
import javax.swing.JScrollPane
import javax.swing.JTabbedPane

class FilmViewAndTableSetup(
    private val tableInstaller: FilmTableInstaller,
    private val viewController: FilmViewController,
) {
    fun tableInstaller(): FilmTableInstaller = tableInstaller
    fun viewController(): FilmViewController = viewController

    companion object {
        @JvmStatic
        fun create(
            psetButtonsTab: JTabbedPane,
            psetButtonsPanel: Supplier<PsetButtonsPanel?>,
            setPsetButtonsPanel: Consumer<PsetButtonsPanel>,
            showButtonsMenuItem: Supplier<JCheckBoxMenuItem>,
            showDescriptionMenuItem: Supplier<JCheckBoxMenuItem>,
            filmListScrollPane: JScrollPane,
            ownerComponent: Component,
            table: Supplier<MVFilmTable>,
            tableOrNull: Supplier<MVFilmTable?>,
            setTable: Consumer<MVFilmTable>,
            filmActionHost: FilmActionHost,
            filmUiActions: Supplier<FilmUiActions>,
            selectedFilm: Supplier<Optional<DatenFilm>>,
            filmAtRow: IntFunction<Optional<DatenFilm>>,
            playSelectedFilm: Runnable,
            saveSelectedFilm: Runnable,
            startFilmWithPset: Consumer<DatenPset>,
            setSelectionUpdatesSuspended: Consumer<Boolean>,
            mediathekGui: MediathekGui,
            updateSelectedListItemsCount: Runnable,
            onComponentShown: Runnable,
            updateFilmData: Runnable,
            selectionUpdatesSuspended: Supplier<Boolean>,
            descriptionTabController: DescriptionTabController,
        ): FilmViewAndTableSetup {
            val viewHost = FilmViewHostAdapter(
                psetButtonsTab,
                psetButtonsPanel,
                setPsetButtonsPanel,
                showButtonsMenuItem,
                showDescriptionMenuItem,
                filmUiActions,
                descriptionTabController::setVisible,
                startFilmWithPset,
            )
            val tableContextMenuHost = TableContextMenuHostAdapter(
                table,
                selectedFilm,
                filmAtRow,
                playSelectedFilm,
                saveSelectedFilm,
                startFilmWithPset,
                setSelectionUpdatesSuspended,
                mediathekGui,
                filmUiActions,
            )
            val tableInstallerHost = FilmTableInstallerHostAdapter(
                table,
                tableOrNull,
                setTable,
                filmListScrollPane,
                ownerComponent,
                Supplier { tableContextMenuHost },
                filmActionHost,
                filmUiActions,
                updateSelectedListItemsCount,
                onComponentShown,
                updateFilmData,
                selectionUpdatesSuspended::get,
            )

            return FilmViewAndTableSetup(
                FilmTableInstaller(tableInstallerHost),
                FilmViewController(viewHost),
            )
        }
    }
}
