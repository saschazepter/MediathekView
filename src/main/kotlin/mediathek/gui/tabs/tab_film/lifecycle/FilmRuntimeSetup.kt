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

package mediathek.gui.tabs.tab_film.lifecycle

import mediathek.config.Daten
import mediathek.gui.tabs.tab_film.FilmToolBar
import mediathek.gui.tabs.tab_film.actions.FilmUiActions
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.gui.tabs.tab_film.search.SearchField
import mediathek.gui.tabs.tab_film.search.SearchFieldData
import mediathek.gui.tabs.tab_film.table.FilmTableReloadHostAdapter
import mediathek.gui.tabs.tab_film.table.FilmTableReloader
import mediathek.tool.FilterConfiguration
import mediathek.tool.table.MVFilmTable
import java.awt.event.ActionListener
import java.util.function.Consumer
import java.util.function.Supplier
import javax.swing.Timer

class FilmRuntimeSetup(
    private val tableReloader: FilmTableReloader,
    private val reloadTableDataTimer: Timer,
    private val lifecycleController: FilmLifecycleController,
) {
    fun tableReloader(): FilmTableReloader = tableReloader
    fun reloadTableDataTimer(): Timer = reloadTableDataTimer
    fun lifecycleController(): FilmLifecycleController = lifecycleController

    companion object {
        @JvmStatic
        fun create(
            messageBusSubscriber: Any,
            daten: Daten,
            table: Supplier<MVFilmTable>,
            filterConfiguration: FilterConfiguration,
            bookmarkStartupReloadCoordinator: BookmarkStartupReloadCoordinator,
            swingFilterDialog: Supplier<mediathek.gui.tabs.tab_film.filter.SwingFilterDialog>,
            filmToolBar: Supplier<FilmToolBar>,
            searchField: Supplier<SearchField>,
            filmUiActions: Supplier<FilmUiActions>,
            filterController: FilmFilterController,
            setSelectionUpdatesSuspended: Consumer<Boolean>,
            updateStartInfoProperty: Runnable,
            updateFilmData: Runnable,
            requestTableReload: Runnable,
            saveTableConfiguration: Runnable,
            closeFilterSelectionModel: Runnable,
            timerFactory: java.util.function.Function<ActionListener, Timer>,
        ): FilmRuntimeSetup {
            val tableReloadHost = FilmTableReloadHostAdapter(
                table,
                Supplier {
                    val field = searchField.get()
                    SearchFieldData(field.text, field.getSearchMode())
                },
                filterController,
                daten::getDecoratedPool,
                setSelectionUpdatesSuspended,
                updateStartInfoProperty,
                updateFilmData,
            )
            val tableReloader = FilmTableReloader(tableReloadHost)
            val reloadTableDataTimer = timerFactory.apply(ActionListener { tableReloader.loadTable() })
            val lifecycleHost = FilmLifecycleHostAdapter(
                messageBusSubscriber,
                daten,
                table,
                filterConfiguration,
                bookmarkStartupReloadCoordinator,
                swingFilterDialog,
                filmToolBar,
                searchField,
                filmUiActions,
                requestTableReload,
                updateStartInfoProperty,
                saveTableConfiguration,
                closeFilterSelectionModel,
            )
            val lifecycleController = FilmLifecycleController(lifecycleHost)

            return FilmRuntimeSetup(tableReloader, reloadTableDataTimer, lifecycleController)
        }
    }
}
