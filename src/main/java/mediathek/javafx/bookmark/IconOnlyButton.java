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

import org.kordamp.ikonli.swing.FontIcon;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class IconOnlyButton extends JButton {
    public IconOnlyButton(Action action) {
        super(action);
        setHideActionText(true);

        FontIcon normalIcon = (FontIcon) action.getValue(Action.SMALL_ICON);
        BufferedImage img = new BufferedImage(normalIcon.getIconWidth(), normalIcon.getIconHeight(),
                BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        try {
            normalIcon.paintIcon(null, g2, 0, 0);
        }
        finally {
            g2.dispose();
        }

        var disabledImg = GrayFilter.createDisabledImage(img);
        var disabledIcon = new ImageIcon(disabledImg);

        setDisabledIcon(disabledIcon);
    }
}
