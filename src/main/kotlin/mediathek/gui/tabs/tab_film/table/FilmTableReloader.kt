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

package mediathek.gui.tabs.tab_film.table

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import mediathek.filmlisten.FilmCatalog
import mediathek.daten.DatenFilm
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.gui.tabs.tab_film.helpers.GuiModelHelperFactory
import mediathek.gui.tabs.tab_film.helpers.FilmQueryEngine
import mediathek.gui.tabs.tab_film.search.SearchFieldData
import org.apache.logging.log4j.LogManager
import java.awt.Component

@OptIn(ExperimentalCoroutinesApi::class)
class FilmTableReloader(
    private val host: Host,
    private val queryEngineFactory: (Host) -> FilmQueryEngine = { queryHost ->
        GuiModelHelperFactory.createGuiModelHelper(
            queryHost.filmCatalog(),
            queryHost.owner(),
            queryHost.searchFieldData(),
            queryHost.filterController(),
        )
    },
) {
    interface Host {
        fun tableBinding(): FilmTableModelBinding

        fun filmCatalog(): FilmCatalog

        fun owner(): Component

        fun searchFieldData(): SearchFieldData

        fun filterController(): FilmFilterController

        fun setSelectionUpdatesSuspended(suspended: Boolean)

        fun updateStartInfoProperty()

        fun updateFilmData()

        fun onReloadCompleted(fromSearchField: Boolean)
    }

    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    private val modelDispatcher = Dispatchers.Default.limitedParallelism(1)
    private var modelJob: Job? = null
    private var generation = 0L

    fun loadTable() {
        loadTable(false)
    }

    fun dispose() {
        invalidate()
        uiScope.cancel()
    }

    fun invalidate() {
        generation += 1
        modelJob?.cancel()
        modelJob = null
    }

    fun loadTable(fromSearchField: Boolean) {
        val requestedGeneration = ++generation
        modelJob?.cancel()
        modelJob = uiScope.launch {
            val result = runCatching {
                withContext(modelDispatcher) {
                    queryEngineFactory(host).query()
                }
            }

            result.fold(
                onSuccess = { films ->
                    if (requestedGeneration == generation) {
                        applyFilteredFilms(films, fromSearchField)
                    }
                },
                onFailure = { thrown ->
                    if (thrown is CancellationException) {
                        return@fold
                    }
                    logger.error("Model filtering failed!", thrown)
                    if (requestedGeneration == generation) {
                        host.setSelectionUpdatesSuspended(false)
                        host.onReloadCompleted(fromSearchField)
                    }
                },
            )
        }
    }

    private suspend fun applyFilteredFilms(
        films: Collection<DatenFilm>,
        fromSearchField: Boolean,
    ) {
        host.setSelectionUpdatesSuspended(true)
        host.tableBinding().replaceFilms(films)
        host.updateStartInfoProperty()
        host.updateFilmData()
        host.setSelectionUpdatesSuspended(false)
        host.onReloadCompleted(fromSearchField)
    }

    private companion object {
        private val logger = LogManager.getLogger()
    }
}
