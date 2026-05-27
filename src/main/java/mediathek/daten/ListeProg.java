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
package mediathek.daten;

import mediathek.tool.models.NonEditableTableModel;

import javax.swing.table.TableModel;
import java.util.ArrayList;
import java.util.Iterator;

public class ListeProg extends ArrayList<DatenProg> {
    public DatenProg remove(String name) {
        DatenProg ret = null;
        Iterator<DatenProg> it = this.iterator();
        DatenProg prog;
        while (it.hasNext()) {
            prog = it.next();
            if (prog.arr[DatenProg.PROGRAMM_NAME].equals(name)) {
                it.remove();
                ret = prog;
                break;
            }
        }
        return ret;
    }

    public int auf(int idx, boolean auf) {
        DatenProg prog = this.remove(idx);
        int neu = idx;
        if (auf) {
            if (neu > 0) {
                --neu;
            }
        } else if (neu < this.size()) {
            ++neu;
        }
        this.add(neu, prog);
        return neu;
    }

    public TableModel createModel() {
        Object[][] object = new Object[this.size()][DatenProg.MAX_ELEM];
        int i = 0;
        for (DatenProg daten : this) {
            System.arraycopy(daten.arr, 0, object[i], 0, DatenProg.MAX_ELEM);
            object[i][DatenProg.PROGRAMM_RESTART] = daten.isRestart();
            object[i][DatenProg.PROGRAMM_DOWNLOADMANAGER] = daten.isDownloadManager();
            ++i;
        }
        return new ProgramTableModel(this, object);
    }

    private static class ProgramTableModel extends NonEditableTableModel {
        private final ListeProg listeProg;

        ProgramTableModel(ListeProg listeProg, Object[][] data) {
            super(data, DatenProg.COLUMN_NAMES);
            this.listeProg = listeProg;
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            if (columnIndex == DatenProg.PROGRAMM_RESTART
                    || columnIndex == DatenProg.PROGRAMM_DOWNLOADMANAGER) {
                return Boolean.class;
            }
            return super.getColumnClass(columnIndex);
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == DatenProg.PROGRAMM_RESTART
                    || column == DatenProg.PROGRAMM_DOWNLOADMANAGER) {
                boolean selected = Boolean.parseBoolean(String.valueOf(aValue));
                listeProg.get(row).arr[column] = Boolean.toString(selected);
                super.setValueAt(selected, row, column);
            } else {
                listeProg.get(row).arr[column] = aValue == null ? null : aValue.toString();
                super.setValueAt(aValue, row, column);
            }
        }
    }

}
