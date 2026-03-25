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

package mediathek.tool.cellrenderer;

import mediathek.tool.MVC;
import mediathek.tool.models.TModelColor;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

public class CellRendererColor extends DefaultTableCellRenderer {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                   boolean hasFocus, int row, int column) {
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        final int r = table.convertRowIndexToModel(row);

        try {
            TModelColor colorModel = (TModelColor) table.getModel();
            MVC color = colorModel.getEntry(r);
            setHorizontalAlignment(SwingConstants.CENTER);
            setBackground(color.getColor(colorModel.isDarkMode()));
            setText("");
        } catch (IndexOutOfBoundsException ex) {
            logger.error("unable to get color", ex);
        }
        return this;
    }
}
