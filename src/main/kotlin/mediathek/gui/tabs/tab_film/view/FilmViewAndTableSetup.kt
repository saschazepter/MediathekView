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
    val tableInstaller: FilmTableInstaller,
    val viewController: FilmViewController,
) {
    data class ViewDependencies(
        val psetButtonsTab: JTabbedPane,
        val psetButtonsPanel: Supplier<PsetButtonsPanel?>,
        val setPsetButtonsPanel: Consumer<PsetButtonsPanel>,
        val showButtonsMenuItem: Supplier<JCheckBoxMenuItem>,
        val showDescriptionMenuItem: Supplier<JCheckBoxMenuItem>,
        val descriptionTabController: DescriptionTabController,
    )

    data class TableDependencies(
        val filmListScrollPane: JScrollPane,
        val ownerComponent: Component,
        val table: Supplier<MVFilmTable>,
        val tableOrNull: Supplier<MVFilmTable?>,
        val setTable: Consumer<MVFilmTable>,
    )

    data class ActionDependencies(
        val filmActionHost: FilmActionHost,
        val filmUiActions: Supplier<FilmUiActions>,
        val selectedFilm: Supplier<Optional<DatenFilm>>,
        val filmAtRow: IntFunction<Optional<DatenFilm>>,
        val playSelectedFilm: Runnable,
        val saveSelectedFilm: Runnable,
        val startFilmWithPset: Consumer<DatenPset>,
        val setSelectionUpdatesSuspended: Consumer<Boolean>,
    )

    data class RuntimeHooks(
        val mediathekGui: MediathekGui,
        val updateSelectedListItemsCount: Runnable,
        val onComponentShown: Runnable,
        val updateFilmData: Runnable,
        val selectionUpdatesSuspended: Supplier<Boolean>,
    )

    companion object {
        @JvmStatic
        fun create(
            view: ViewDependencies,
            tableDependencies: TableDependencies,
            actions: ActionDependencies,
            runtimeHooks: RuntimeHooks,
        ): FilmViewAndTableSetup {
            val viewHost = FilmViewHostAdapter(
                view.psetButtonsTab,
                view.psetButtonsPanel,
                view.setPsetButtonsPanel,
                view.showButtonsMenuItem,
                view.showDescriptionMenuItem,
                actions.filmUiActions,
                view.descriptionTabController::setVisible,
                actions.startFilmWithPset,
            )
            val tableContextMenuHost = TableContextMenuHostAdapter(
                tableDependencies.table,
                actions.selectedFilm,
                actions.filmAtRow,
                actions.playSelectedFilm,
                actions.saveSelectedFilm,
                actions.startFilmWithPset,
                actions.setSelectionUpdatesSuspended,
                runtimeHooks.mediathekGui,
                actions.filmUiActions,
            )
            val tableInstallerHost = FilmTableInstallerHostAdapter(
                tableDependencies.table,
                tableDependencies.tableOrNull,
                tableDependencies.setTable,
                tableDependencies.filmListScrollPane,
                tableDependencies.ownerComponent,
                Supplier { tableContextMenuHost },
                actions.filmActionHost,
                actions.filmUiActions,
                runtimeHooks.updateSelectedListItemsCount,
                runtimeHooks.onComponentShown,
                runtimeHooks.updateFilmData,
                runtimeHooks.selectionUpdatesSuspended::get,
            )

            return FilmViewAndTableSetup(
                FilmTableInstaller(tableInstallerHost),
                FilmViewController(viewHost),
            )
        }
    }
}
