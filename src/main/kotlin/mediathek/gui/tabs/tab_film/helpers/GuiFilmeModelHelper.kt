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
import java.util.stream.Stream
import javax.swing.table.TableModel

class GuiFilmeModelHelper(
    searchFieldData: SearchFieldData,
    filterController: FilmFilterController,
) : GuiModelHelper {
    private val support = GuiModelHelperSupport(searchFieldData, filterController)

    override val filteredTableModel: TableModel
        get() {
            val allFilms = allFilms()
            return support.getFilteredTableModel(allFilms, ::filterFilms)
        }

    private fun allFilms(): Collection<DatenFilm> = Daten.getInstance().listeFilmeNachBlackList

    private fun filterFilms(): Collection<DatenFilm> {
        val filterContext = support.createFilterExecutionContext()

        if (support.state().showUnseenOnly) {
            SeenHistoryController.prepareSharedMemoryCache()
        }

        var stream = Daten.getInstance().listeFilmeNachBlackList.parallelStream()
        if (filterContext.hasSelectedSenders()) {
            stream = stream.filter(filterContext.senderFilter)
        }
        stream = applyConfiguredPredicates(stream)

        stream = support.applyCommonFilters(stream, filterContext.filterThema, filterContext.lengthFilterRange)

        if (filterContext.hasSearchTerms()) {
            stream = stream.filter(filterContext.finalStageFilter)
        }

        return stream.toList()
    }

    private fun applyConfiguredPredicates(source: Stream<DatenFilm>): Stream<DatenFilm> {
        var stream = source
        for (predicateSpec in createPredicateSpecs()) {
            if (predicateSpec.enabled()) {
                stream = stream.filter { film -> predicateSpec.predicate(film) }
            }
        }
        return stream
    }

    private fun createPredicateSpecs(): List<PredicateSpec> {
        val geographicLocation = ApplicationConfiguration.getInstance().geographicLocation

        return listOf(
            predicateSpec({ support.state().showNewOnly }, DatenFilm::isNew),
            predicateSpec({ support.state().showBookMarkedOnly }, DatenFilm::isBookmarked),
            predicateSpec({ support.state().showLivestreamsOnly }, DatenFilm::isLivestream),
            predicateSpec({ support.state().showHighQualityOnly }, DatenFilm::isHighQuality),
            predicateSpec({ support.state().dontShowTrailers }) { film -> !film.isTrailerTeaser },
            predicateSpec({ support.state().dontShowSignLanguage }) { film -> !film.isSignLanguage },
            predicateSpec({ support.state().dontShowGeoblocked }) { film ->
                !film.isGeoBlockedForLocation(geographicLocation)
            },
            predicateSpec({ support.state().dontShowAudioVersions }) { film -> !film.isAudioVersion },
            predicateSpec({ support.state().dontShowAbos }) { film -> film.abo == null },
            predicateSpec({ support.state().dontShowDuplicates }) { film -> !film.isDuplicate },
            predicateSpec({ support.state().showSubtitlesOnly }, DatenFilm::hasAnySubtitles),
        )
    }

    private fun predicateSpec(enabled: () -> Boolean, predicate: (DatenFilm) -> Boolean): PredicateSpec =
        PredicateSpec(enabled, predicate)

    private data class PredicateSpec(
        val enabled: () -> Boolean,
        val predicate: (DatenFilm) -> Boolean,
    )
}
