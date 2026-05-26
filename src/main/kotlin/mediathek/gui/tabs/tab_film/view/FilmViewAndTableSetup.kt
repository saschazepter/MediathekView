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
import javax.swing.JCheckBoxMenuItem
import javax.swing.JScrollPane
import javax.swing.JTabbedPane

class FilmViewAndTableSetup(
    val tableInstaller: FilmTableInstaller,
    val viewController: FilmViewController,
) {
    data class ViewDependencies(
        val psetButtonsTab: JTabbedPane,
        val psetButtonsPanel: () -> PsetButtonsPanel?,
        val setPsetButtonsPanel: (PsetButtonsPanel) -> Unit,
        val showButtonsMenuItem: () -> JCheckBoxMenuItem,
        val showDescriptionMenuItem: () -> JCheckBoxMenuItem,
        val descriptionTabController: DescriptionTabController,
    )

    data class TableDependencies(
        val filmListScrollPane: JScrollPane,
        val ownerComponent: Component,
        val table: () -> MVFilmTable,
        val tableOrNull: () -> MVFilmTable?,
        val setTable: (MVFilmTable) -> Unit,
    )

    data class ActionDependencies(
        val filmActionHost: FilmActionHost,
        val filmUiActions: () -> FilmUiActions,
        val selectedFilm: () -> Optional<DatenFilm>,
        val filmAtRow: (Int) -> Optional<DatenFilm>,
        val playSelectedFilm: () -> Unit,
        val saveSelectedFilm: () -> Unit,
        val startFilmWithPset: (DatenPset) -> Unit,
        val setSelectionUpdatesSuspended: (Boolean) -> Unit,
    )

    data class RuntimeHooks(
        val mediathekGui: MediathekGui,
        val updateSelectedListItemsCount: () -> Unit,
        val onComponentShown: () -> Unit,
        val updateFilmData: () -> Unit,
        val selectionUpdatesSuspended: () -> Boolean,
    )

    companion object {
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
                { tableContextMenuHost },
                actions.filmActionHost,
                actions.filmUiActions,
                runtimeHooks.updateSelectedListItemsCount,
                runtimeHooks.onComponentShown,
                runtimeHooks.updateFilmData,
                runtimeHooks.selectionUpdatesSuspended,
            )

            return FilmViewAndTableSetup(
                FilmTableInstaller(tableInstallerHost),
                FilmViewController(viewHost),
            )
        }
    }
}
