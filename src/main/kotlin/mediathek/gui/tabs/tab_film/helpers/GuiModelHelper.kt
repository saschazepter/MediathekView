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

import mediathek.controller.SenderFilmlistLoadApprover
import mediathek.controller.history.SeenHistoryController
import mediathek.daten.DatenFilm
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.gui.tabs.tab_film.filter.FilmFilterState
import mediathek.gui.tabs.tab_film.filter.FilmLengthSlider
import mediathek.gui.tabs.tab_film.filter.ZeitraumSpinner
import mediathek.gui.tabs.tab_film.search.SearchFieldData
import mediathek.tool.models.TModelFilm
import java.util.concurrent.TimeUnit
import java.util.function.Predicate
import java.util.stream.Stream
import javax.swing.table.TableModel

sealed interface GuiModelHelper {
    val filteredTableModel: TableModel
}

internal class GuiModelHelperSupport(
    private val searchFieldData: SearchFieldData,
    private val filterController: FilmFilterController,
) {
    fun getFilteredTableModel(
        allFilms: Collection<DatenFilm>,
        filteredFilmSupplier: () -> Collection<DatenFilm>,
    ): TableModel {
        if (allFilms.isEmpty()) {
            return createEmptyFilmTableModel()
        }
        if (noFiltersAreSet()) {
            return createFilmTableModel(allFilms)
        }
        return createFilmTableModel(filteredFilmSupplier())
    }

    fun applyCommonFilters(
        source: Stream<DatenFilm>,
        filterThema: String,
        lengthFilterRange: LengthFilterRange,
    ): Stream<DatenFilm> {
        var stream = source
        if (filterThema.isNotEmpty()) {
            stream = stream.filter { film -> film.thema.equals(filterThema, ignoreCase = true) }
        }
        if (lengthFilterRange.hasUpperLimit()) {
            stream = stream.filter { film -> film.filmLength < lengthFilterRange.maxLengthInSeconds }
        }
        if (state().showUnseenOnly) {
            stream = stream.filter(::seenCheck)
        }
        return stream.filter { film -> minLengthCheck(film, lengthFilterRange) }
    }

    fun noFiltersAreSet(): Boolean = noFiltersAreSet(state()) && searchFieldData.isEmpty()

    fun createFilterExecutionContext(): FilterExecutionContext {
        val state = state()
        val selectedSenders = getSelectedSendersFromFilter()
        val searchTerms = searchFieldData.evaluateThemaTitel().toList()
        return FilterExecutionContext(
            lengthFilterRange = createLengthFilterRange(),
            selectedSenders = selectedSenders,
            filterThema = state.thema,
            searchFieldText = searchFieldData.searchFieldText,
            searchThroughDescriptions = searchFieldData.searchThroughDescriptions(),
            searchTerms = searchTerms,
            senderFilter = Predicate { film -> selectedSenders.isEmpty() || film.sender in selectedSenders },
            finalStageFilter = if (searchTerms.isEmpty()) {
                Predicate { true }
            } else {
                createFinalStageFilter(
                    searchFieldData.searchThroughDescriptions(),
                    searchTerms.toTypedArray(),
                )
            },
        )
    }

    fun state(): FilmFilterState = filterController.state()

    private fun noFiltersAreSet(state: FilmFilterState): Boolean =
        state.checkedChannels.isEmpty() &&
            state.thema.isEmpty() &&
            state.filmLengthMin == 0 &&
            state.filmLengthMax == FilmLengthSlider.UNLIMITED_VALUE &&
            !state.dontShowAbos &&
            !state.showUnseenOnly &&
            !state.showHighQualityOnly &&
            !state.showSubtitlesOnly &&
            !state.showLivestreamsOnly &&
            !state.showNewOnly &&
            !state.showBookMarkedOnly &&
            !state.dontShowTrailers &&
            !state.dontShowSignLanguage &&
            !state.dontShowGeoblocked &&
            !state.dontShowAudioVersions &&
            !state.dontShowDuplicates &&
            state.zeitraum.equals(ZeitraumSpinner.INFINITE_TEXT, ignoreCase = true)

    private fun minLengthCheck(film: DatenFilm, lengthFilterRange: LengthFilterRange): Boolean {
        val filmLength = film.filmLength
        if (filmLength == 0) {
            return true
        }
        return filmLength >= lengthFilterRange.minLengthInSeconds
    }

    private fun getSelectedSendersFromFilter(): Set<String> =
        state().checkedChannels
            .filter(SenderFilmlistLoadApprover::isApproved)
            .toSet()

    private fun seenCheck(film: DatenFilm): Boolean = !SeenHistoryController.hasBeenSeenFromSharedCache(film)

    private fun createLengthFilterRange(): LengthFilterRange {
        val state = state()
        return LengthFilterRange(
            minLengthInSeconds = TimeUnit.SECONDS.convert(state.filmLengthMin.toLong(), TimeUnit.MINUTES),
            maxLengthInSeconds = TimeUnit.SECONDS.convert(state.filmLengthMax.toLong(), TimeUnit.MINUTES),
        )
    }

    private fun createFilmTableModel(films: Collection<DatenFilm>): TModelFilm {
        val filmModel = TModelFilm(films.size)
        filmModel.addAll(films as? List<DatenFilm> ?: films.toList())
        return filmModel
    }

    private fun createEmptyFilmTableModel(): TModelFilm = TModelFilm()

    data class LengthFilterRange(
        val minLengthInSeconds: Long,
        val maxLengthInSeconds: Long,
    ) {
        fun minLengthInSeconds(): Long = minLengthInSeconds

        fun maxLengthInSeconds(): Long = maxLengthInSeconds

        fun hasUpperLimit(): Boolean = maxLengthInSeconds < UNLIMITED_LENGTH_IN_SECONDS
    }

    data class FilterExecutionContext(
        val lengthFilterRange: LengthFilterRange,
        val selectedSenders: Set<String>,
        val filterThema: String,
        val searchFieldText: String,
        val searchThroughDescriptions: Boolean,
        val searchTerms: List<String>,
        val senderFilter: Predicate<DatenFilm>,
        val finalStageFilter: Predicate<DatenFilm>,
    ) {
        fun lengthFilterRange(): LengthFilterRange = lengthFilterRange

        fun selectedSenders(): Set<String> = selectedSenders

        fun filterThema(): String = filterThema

        fun searchFieldText(): String = searchFieldText

        fun searchThroughDescriptions(): Boolean = searchThroughDescriptions

        fun searchTerms(): List<String> = searchTerms

        fun senderFilter(): Predicate<DatenFilm> = senderFilter

        fun finalStageFilter(): Predicate<DatenFilm> = finalStageFilter

        fun hasSearchTerms(): Boolean = searchTerms.isNotEmpty()

        fun hasSelectedSenders(): Boolean = selectedSenders.isNotEmpty()
    }

    private companion object {
        private val UNLIMITED_LENGTH_IN_SECONDS =
            TimeUnit.SECONDS.convert(FilmLengthSlider.UNLIMITED_VALUE.toLong(), TimeUnit.MINUTES)
    }
}
