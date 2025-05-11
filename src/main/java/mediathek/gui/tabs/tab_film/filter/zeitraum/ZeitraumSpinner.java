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

package mediathek.gui.tabs.tab_film.filter.zeitraum;

import mediathek.tool.FilterConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class ZeitraumSpinner extends JSpinner {
    private static Logger logger;

    private static Logger getLogger() {
        if (logger == null)
            logger = LogManager.getLogger(ZeitraumSpinner.class);
        return logger;
    }

    public ZeitraumSpinner() {
        super(new SpinnerNumberModel(0, 0, 365, 1));
        ((DefaultEditor) getEditor()).getTextField().setFormatterFactory(new ZeitraumSpinnerFormatterFactory());
    }

    public void restoreFilterConfig(@NotNull FilterConfiguration filterConfiguration) throws NumberFormatException {
        var zeitraumVal = filterConfiguration.getZeitraum();
        try {
            int zeitraumValInt;
            if (zeitraumVal.equals(ZeitraumSpinnerFormatter.INFINITE_TEXT))
                zeitraumValInt = ZeitraumSpinnerFormatter.INFINITE_VALUE;
            else
                zeitraumValInt = Integer.parseInt(zeitraumVal);
            setValue(zeitraumValInt);
        }
        catch (NumberFormatException ex) {
            getLogger().error("Failed to parse zeitraum value: {}", zeitraumVal, ex);
            getLogger().error("Using default value: {}", ZeitraumSpinnerFormatter.INFINITE_VALUE);
            setValue(ZeitraumSpinnerFormatter.INFINITE_VALUE);
            filterConfiguration.setZeitraum(ZeitraumSpinnerFormatter.INFINITE_TEXT);
        }
    }

    public void installFilterConfigurationChangeListener(@NotNull FilterConfiguration filterConfiguration) {
        addChangeListener(l -> {
            var val = (int) getValue();
            String strVal;
            if (val == ZeitraumSpinnerFormatter.INFINITE_VALUE)
                strVal = ZeitraumSpinnerFormatter.INFINITE_TEXT;
            else
                strVal = String.valueOf(val);

            filterConfiguration.setZeitraum(strVal);
        });
    }
}
