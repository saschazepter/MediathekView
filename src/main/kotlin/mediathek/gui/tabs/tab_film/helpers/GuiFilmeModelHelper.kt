/*
 * Copyright (c) 2025-2026 derreisende77.
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

package mediathek.gui.tabs.tab_film.helpers

import mediathek.config.Daten
import mediathek.controller.history.SeenHistoryController
import mediathek.daten.DatenFilm
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.gui.tabs.tab_film.search.SearchFieldData
import mediathek.tool.ApplicationConfiguration
import javax.swing.table.TableModel

class GuiFilmeModelHelper(
    searchFieldData: SearchFieldData,
    filterController: FilmFilterController,
) : GuiModelHelper {
    private val support = GuiModelHelperSupport(searchFieldData, filterController)

    override val filteredTableModel: TableModel
        get() {
            val allFilms = allFilms()
            return support.getFilteredTableModel(allFilms) { filterContext ->
                filterFilms(allFilms, filterContext)
            }
        }

    private fun allFilms(): Collection<DatenFilm> = Daten.getInstance().listeFilmeNachBlackList

    private fun filterFilms(
        allFilms: Collection<DatenFilm>,
        filterContext: GuiModelHelperSupport.FilterExecutionContext,
    ): Collection<DatenFilm> {
        val state = filterContext.state
        if (state.showUnseenOnly) {
            SeenHistoryController.prepareSharedMemoryCache()
        }

        var stream = allFilms.parallelStream()
        if (filterContext.hasSelectedSenders()) {
            stream = stream.filter { film -> filterContext.senderFilter(film) }
        }
        if (state.showNewOnly) {
            stream = stream.filter(DatenFilm::isNew)
        }
        if (state.showBookMarkedOnly) {
            stream = stream.filter(DatenFilm::isBookmarked)
        }
        if (state.showLivestreamsOnly) {
            stream = stream.filter(DatenFilm::isLivestream)
        }
        if (state.showHighQualityOnly) {
            stream = stream.filter(DatenFilm::isHighQuality)
        }
        if (state.dontShowTrailers) {
            stream = stream.filter { film -> !film.isTrailerTeaser }
        }
        if (state.dontShowSignLanguage) {
            stream = stream.filter { film -> !film.isSignLanguage }
        }
        if (state.dontShowGeoblocked) {
            val geographicLocation = ApplicationConfiguration.getInstance().geographicLocation
            stream = stream.filter { film -> !film.isGeoBlockedForLocation(geographicLocation) }
        }
        if (state.dontShowAudioVersions) {
            stream = stream.filter { film -> !film.isAudioVersion }
        }
        if (state.dontShowAbos) {
            stream = stream.filter { film -> film.abo == null }
        }
        if (state.dontShowDuplicates) {
            stream = stream.filter { film -> !film.isDuplicate }
        }
        if (state.showSubtitlesOnly) {
            stream = stream.filter(DatenFilm::hasAnySubtitles)
        }

        stream = support.applyCommonFilters(stream, filterContext)
        if (filterContext.hasSearchTerms()) {
            stream = stream.filter { film -> filterContext.finalStageFilter(film) }
        }

        return stream.toList()
    }
}
