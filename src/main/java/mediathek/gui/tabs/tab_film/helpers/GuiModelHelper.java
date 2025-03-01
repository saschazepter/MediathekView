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

import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.DatenFilm;
import mediathek.gui.tabs.tab_film.SearchFieldData;
import mediathek.javafx.filterpanel.FilmLengthSlider;
import mediathek.javafx.filterpanel.FilterActionPanel;
import mediathek.javafx.filterpanel.ZeitraumSpinner;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableModel;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

public abstract class GuiModelHelper {
    protected SliderRange sliderRange;
    protected long maxLength;
    protected FilterActionPanel filterActionPanel;
    protected SeenHistoryController historyController;
    protected SearchFieldData searchFieldData;

    protected GuiModelHelper(@NotNull FilterActionPanel filterActionPanel,
                          @NotNull SeenHistoryController historyController,
                          @NotNull SearchFieldData searchFieldData) {
        this.filterActionPanel = filterActionPanel;
        this.historyController = historyController;
        this.searchFieldData = searchFieldData;
    }

    /**
     * Filter the filmlist.
     *
     * @return the filtered table model.
     */
    public abstract TableModel getFilteredTableModel();

    protected boolean maxLengthCheck(DatenFilm film) {
        return film.getFilmLength() < sliderRange.maxLengthInSeconds();
    }

    protected boolean minLengthCheck(DatenFilm film) {
        var filmLength = film.getFilmLength();
        if (filmLength == 0)
            return true; // always show entries with length 0, which are internally "no length"
        else
            return filmLength >= sliderRange.minLengthInSeconds();
    }

    public Stream<DatenFilm> applyCommonFilters(Stream<DatenFilm> stream, final String filterThema) {
        if (!filterThema.isEmpty()) {
            stream = stream.filter(film -> film.getThema().equalsIgnoreCase(filterThema));
        }
        if (maxLength < FilmLengthSlider.UNLIMITED_VALUE) {
            stream = stream.filter(this::maxLengthCheck);
        }
        if (filterActionPanel.isShowUnseenOnly()) {
            stream = stream.filter(this::seenCheck);
        }
        //perform min length filtering after all others may have reduced the available entries...
        return stream.filter(this::minLengthCheck);
    }

    protected boolean noFiltersAreSet() {
        return filterActionPanel.getViewSettingsPane().senderCheckList.getCheckModel().isEmpty()
                && getFilterThema().isEmpty()
                && searchFieldData.isEmpty()
                && filterActionPanel.getFilmLengthSliderValues().noFiltersAreSet()
                && !filterActionPanel.isDontShowAbos()
                && !filterActionPanel.isShowUnseenOnly()
                && !filterActionPanel.isShowOnlyHighQuality()
                && !filterActionPanel.isShowSubtitlesOnly()
                && !filterActionPanel.isShowLivestreamsOnly()
                && !filterActionPanel.isShowNewOnly()
                && !filterActionPanel.isShowBookMarkedOnly()
                && !filterActionPanel.isDontShowTrailers()
                && !filterActionPanel.isDontShowSignLanguage()
                && !filterActionPanel.isDontShowAudioVersions()
                && !filterActionPanel.isDontShowDuplicates()
                && filterActionPanel.zeitraumProperty().get().equalsIgnoreCase(ZeitraumSpinner.UNLIMITED_VALUE);
    }

    protected boolean seenCheck(DatenFilm film) {
        return !historyController.hasBeenSeenFromCache(film);
    }

    protected void calculateFilmLengthSliderValues() {
        var sliderVals = filterActionPanel.getFilmLengthSliderValues();
        maxLength = sliderVals.maxLength();
        var minLengthInSeconds = TimeUnit.SECONDS.convert(sliderVals.minLength(), TimeUnit.MINUTES);
        var maxLengthInSeconds = TimeUnit.SECONDS.convert(maxLength, TimeUnit.MINUTES);
        sliderRange = new SliderRange(minLengthInSeconds, maxLengthInSeconds);
    }

    protected String getFilterThema() {
        String filterThema = filterActionPanel.getViewSettingsPane().themaComboBox.getSelectionModel().getSelectedItem();

        return filterThema == null ? "" : filterThema;
    }
}
