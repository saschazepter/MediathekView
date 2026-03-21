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

package mediathek.gui.tabs.tab_film.filter;

import com.jidesoft.swing.RangeSlider;

import javax.swing.*;
import java.util.Hashtable;

public class FilmLengthSlider extends RangeSlider {
    private static final int MIN_FILM_LENGTH = 0;
    private static final int MAX_FILM_LENGTH = 240;
    public static final int UNLIMITED_VALUE = MAX_FILM_LENGTH;
    private static final int TICK_SPACING = 30;
    private static final String UNLIMITED_TEXT = "∞";

    public FilmLengthSlider() {
        super(MIN_FILM_LENGTH, MAX_FILM_LENGTH);

        setPaintLabels(true);
        setPaintTicks(true);
        setPaintTrack(true);
        setMajorTickSpacing(TICK_SPACING);
        setLabelTable(new Hashtable<Integer, JComponent>() {
            {
                for (int filmLength = MIN_FILM_LENGTH; filmLength < MAX_FILM_LENGTH; filmLength += TICK_SPACING) {
                    put(filmLength, new JLabel(Integer.toString(filmLength)));
                }
                put(MAX_FILM_LENGTH, new JLabel(UNLIMITED_TEXT));
            }
        });
    }

    public String getHighValueText() {
        return getHighValue() == UNLIMITED_VALUE ? UNLIMITED_TEXT : Integer.toString(getHighValue());
    }
}
