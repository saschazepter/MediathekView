/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.tool;

import java.awt.Component;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import mediathek.controller.Log;
import mediathek.daten.Daten;

public class CellRendererColor extends DefaultTableCellRenderer {

    @Override
    public Component getTableCellRendererComponent(
            JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column) {
        setBackground(null);
//        setForeground(null);
//        setFont(null);
//        setIcon(null);
        setHorizontalAlignment(SwingConstants.LEADING);
        super.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column);
        try {
            int r = table.convertRowIndexToModel(row);
            int c = table.convertColumnIndexToModel(column);
            MVC color = Daten.mVColor.liste.get(r);
            if (c == MVColor.MVC_COLOR) {
                setHorizontalAlignment(SwingConstants.CENTER);
                //setIcon(GetIcon.getIcon("edit_16.png"));
                setBackground(color.color);
                setText("");
            }
        } catch (Exception ex) {
            Log.fehlerMeldung(630365892, this.getClass().getName(), ex);
        }
        return this;
    }
}
