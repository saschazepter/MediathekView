/*
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.daten

import mediathek.tool.models.NonEditableTableModel
import javax.swing.table.TableModel

class ListeProg : ArrayList<DatenProg>() {
    fun remove(name: String): DatenProg? {
        val iterator = iterator()
        while (iterator.hasNext()) {
            val prog = iterator.next()
            if (prog.arr[DatenProg.PROGRAMM_NAME] == name) {
                iterator.remove()
                return prog
            }
        }
        return null
    }

    fun moveEntryAtIndex(idx: Int, up: Boolean): Int {
        val prog = removeAt(idx)
        var neu = idx
        if (up) {
            if (neu > 0) {
                --neu
            }
        } else if (neu < size) {
            ++neu
        }
        add(neu, prog)
        return neu
    }

    fun createModel(): TableModel {
        val rows = Array(size) { arrayOfNulls<Any>(DatenProg.MAX_ELEM) }
        for ((index, daten) in this.withIndex()) {
            System.arraycopy(daten.arr, 0, rows[index], 0, DatenProg.MAX_ELEM)
            rows[index][DatenProg.PROGRAMM_RESTART] = daten.isRestart
            rows[index][DatenProg.PROGRAMM_DOWNLOADMANAGER] = daten.isDownloadManager
        }
        return ProgramTableModel(this, rows)
    }

    private class ProgramTableModel(
        private val listeProg: ListeProg,
        data: Array<Array<Any?>>,
    ) : NonEditableTableModel(data, DatenProg.COLUMN_NAMES) {
        override fun getColumnClass(columnIndex: Int): Class<*> =
            when (columnIndex) {
                DatenProg.PROGRAMM_RESTART,
                DatenProg.PROGRAMM_DOWNLOADMANAGER,
                -> Boolean::class.javaObjectType

                else -> super.getColumnClass(columnIndex)
            }

        override fun setValueAt(
            aValue: Any?,
            row: Int,
            column: Int,
        ) {
            if (column == DatenProg.PROGRAMM_RESTART || column == DatenProg.PROGRAMM_DOWNLOADMANAGER) {
                val selected = aValue.toString().toBoolean()
                listeProg[row].arr[column] = selected.toString()
                super.setValueAt(selected, row, column)
            } else {
                listeProg[row].arr[column] = aValue?.toString()
                super.setValueAt(aValue, row, column)
            }
        }
    }
}
