/*
 * Copyright (c) 2025 derreisende77.
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

package mediathek.gui.tabs.tab_film.helpers;

import mediathek.config.Daten;
import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.DatenFilm;
import mediathek.gui.tabs.tab_film.SearchFieldData;
import mediathek.tool.FilterConfiguration;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class GuiFilmeModelHelper extends GuiModelHelper {
    public GuiFilmeModelHelper(@NotNull SeenHistoryController historyController,
                               @NotNull SearchFieldData searchFieldData,
                               @NotNull FilterConfiguration filterConfiguration) {
        super(historyController, searchFieldData, filterConfiguration);
    }

    @Override
    protected Collection<DatenFilm> getAllFilms() {
        return Daten.getInstance().getListeFilmeNachBlackList();
    }

    @Override
    protected Collection<DatenFilm> filterFilms() {
        var lengthFilterRange = createLengthFilterRange();

        if (filterConfiguration.isShowUnseenOnly()) {
            historyController.prepareMemoryCache();
        }

        var stream = Daten.getInstance().getListeFilmeNachBlackList().parallelStream();
        var selectedSenders = getSelectedSendersFromFilter();
        if (!selectedSenders.isEmpty()) {
            stream = stream.filter(f -> selectedSenders.contains(f.getSender()));
        }
        stream = applyConfiguredPredicates(stream);

        stream = applyCommonFilters(stream, filterConfiguration.getThema(), lengthFilterRange);

        //final stage filtering...
        String[] arrIrgendwo = searchFieldData.evaluateThemaTitel();
        final boolean searchFieldEmpty = arrIrgendwo.length == 0;
        if (!searchFieldEmpty) {
            stream = stream.filter(FinalStageFilterFactory
                    .createFinalStageFilter(searchFieldData.searchThroughDescriptions(), arrIrgendwo));
        }

        var list = stream.toList();
        return list;
    }

    private Stream<DatenFilm> applyConfiguredPredicates(Stream<DatenFilm> stream) {
        for (var predicateSpec : createPredicateSpecs()) {
            if (predicateSpec.enabled().getAsBoolean()) {
                stream = stream.filter(predicateSpec.predicate());
            }
        }
        return stream;
    }

    private List<PredicateSpec> createPredicateSpecs() {
        return List.of(
                predicateSpec(filterConfiguration::isShowNewOnly, DatenFilm::isNew),
                predicateSpec(filterConfiguration::isShowBookMarkedOnly, DatenFilm::isBookmarked),
                predicateSpec(filterConfiguration::isShowLivestreamsOnly, DatenFilm::isLivestream),
                predicateSpec(filterConfiguration::isShowHighQualityOnly, DatenFilm::isHighQuality),
                predicateSpec(filterConfiguration::isDontShowTrailers, film -> !film.isTrailerTeaser()),
                predicateSpec(filterConfiguration::isDontShowSignLanguage, film -> !film.isSignLanguage()),
                predicateSpec(filterConfiguration::isDontShowAudioVersions, film -> !film.isAudioVersion()),
                predicateSpec(filterConfiguration::isDontShowAbos, film -> film.getAbo() == null),
                predicateSpec(filterConfiguration::isDontShowDuplicates, film -> !film.isDuplicate()),
                predicateSpec(filterConfiguration::isShowSubtitlesOnly, DatenFilm::hasAnySubtitles));
    }

    private PredicateSpec predicateSpec(@NotNull BooleanSupplier enabled, @NotNull Predicate<DatenFilm> predicate) {
        return new PredicateSpec(enabled, predicate);
    }

    private record PredicateSpec(BooleanSupplier enabled, Predicate<DatenFilm> predicate) {}
}
