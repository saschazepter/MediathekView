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

package mediathek.gui.tabs.tab_film.helpers;

import mediathek.config.Daten;
import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.IndexedFilmList;
import mediathek.gui.tabs.tab_film.SearchFieldData;
import mediathek.gui.tabs.tab_film.filter.FilmFilterController;
import org.jetbrains.annotations.NotNull;

public class GuiModelHelperFactory {
    public static GuiModelHelper createGuiModelHelper(@NotNull SeenHistoryController historyController,
                                                      @NotNull SearchFieldData searchFieldData,
                                                      @NotNull FilmFilterController filterController) {
        GuiModelHelper helper;
        if (Daten.getInstance().getListeFilmeNachBlackList() instanceof IndexedFilmList) {
            helper = new LuceneGuiFilmeModelHelper(historyController, searchFieldData, filterController);
        }
        else {
            helper = new GuiFilmeModelHelper(historyController, searchFieldData, filterController);
        }
        return helper;
    }
}
