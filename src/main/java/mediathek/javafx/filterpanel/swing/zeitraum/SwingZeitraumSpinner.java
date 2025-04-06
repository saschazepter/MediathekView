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

package mediathek.javafx.filterpanel.swing.zeitraum;

import mediathek.javafx.filterpanel.ZeitraumSpinner;
import mediathek.tool.FilterConfiguration;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;

public class SwingZeitraumSpinner extends JSpinner {
    private static final Logger logger = LogManager.getLogger();

    public SwingZeitraumSpinner() {
        super(new SpinnerNumberModel(0, 0, 365, 1));
        ((DefaultEditor) getEditor()).getTextField().setFormatterFactory(new ZeitraumSpinnerFormatterFactory());
    }

    public void restoreFilterConfig(@NotNull FilterConfiguration filterConfiguration) {
        try {
            var zeitraumVal = filterConfiguration.getZeitraum();
            int zeitraumValInt;
            if (zeitraumVal.equals(ZeitraumSpinnerFormatter.INFINITE_TEXT))
                zeitraumValInt = (int) ZeitraumSpinnerFormatter.INFINITE_VALUE;
            else
                zeitraumValInt = Integer.parseInt(zeitraumVal);
            setValue(zeitraumValInt);
        } catch (Exception exception) {
            logger.error("Failed to restore filter config!", exception);
        }
    }

    public void installFilterConfigurationChangeListener(@NotNull FilterConfiguration filterConfiguration) {
        addChangeListener(l -> {
            try {
                var val = (int) getValue();
                String strVal;
                if (val == (int) ZeitraumSpinnerFormatter.INFINITE_VALUE)
                    strVal = ZeitraumSpinner.UNLIMITED_VALUE;
                else
                    strVal = String.valueOf(val);

                filterConfiguration.setZeitraum(strVal);
            } catch (Exception exception) {
                logger.error("Failed to save filter config!", exception);
            }
        });
    }
}
