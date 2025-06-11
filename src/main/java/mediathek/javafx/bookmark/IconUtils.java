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

package mediathek.javafx.bookmark;

import com.formdev.flatlaf.FlatLaf;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.kordamp.ikonli.Ikon;
import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;


public class IconUtils {
    private static final int DEFAULT_SIZE = 16;
    private static final int DEFAULT_TOOLBAR_SIZE = 18;
    private static final Color DEFAULT_LIGHT_COLOR = new Color(110,110,110);
    private static final Color DEFAULT_DARK_COLOR = new Color(176,177,179);
    private static final List<WeakReference<FontIcon>> themedIcons = new ArrayList<>();
    private static final Logger logger = LogManager.getLogger();

    static {
        PropertyChangeListener lafListener = evt -> {
            if ("lookAndFeel".equals(evt.getPropertyName())) {
                // The L&F has changed, update our icons
                updateIconColors();
            }
        };
        UIManager.addPropertyChangeListener(lafListener);
    }

    public static FontIcon of(Ikon ikon) {
        return of(ikon, DEFAULT_SIZE);
    }

    public static FontIcon toolbarIcon(Ikon ikon) {
        return of(ikon, DEFAULT_TOOLBAR_SIZE);
    }

    public static FontIcon of(Ikon ikon, int size) {
        var color = FlatLaf.isLafDark() ? DEFAULT_DARK_COLOR : DEFAULT_LIGHT_COLOR;
        var icon = FontIcon.of(ikon, size, color);
        themedIcons.add(new WeakReference<>(icon));
        return icon;
    }

    private static void updateIconColors() {
        if (themedIcons.isEmpty()) {
            return;
        }

        var color = FlatLaf.isLafDark() ? DEFAULT_DARK_COLOR : DEFAULT_LIGHT_COLOR;

        var iter = themedIcons.iterator();
        while (iter.hasNext()) {
            var icon = iter.next().get();
            // remove dead icons which got GCed...
            if (icon == null) {
                logger.trace("Removed lost reference to icon");
                iter.remove();
            }
            else  {
                icon.setIconColor(color);
            }
        }
    }
}
