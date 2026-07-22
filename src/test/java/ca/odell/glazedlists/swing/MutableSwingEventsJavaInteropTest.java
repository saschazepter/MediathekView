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
package ca.odell.glazedlists.swing;

import ca.odell.glazedlists.event.ListEvent;
import org.junit.jupiter.api.Test;

import javax.swing.event.ListDataEvent;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MutableSwingEventsJavaInteropTest {

    @Test
    void publicConstructorsAndMutatorsRemainCallableFromJava() {
        MutableListDataEvent listEvent = new MutableListDataEvent(this);
        listEvent.setRange(2, 5);
        listEvent.setType(ListDataEvent.INTERVAL_ADDED);

        assertEquals(2, listEvent.getIndex0());
        assertEquals(5, listEvent.getIndex1());
        assertEquals(ListDataEvent.INTERVAL_ADDED, listEvent.getType());

        MutableTableModelEvent tableEvent = new MutableTableModelEvent(new DefaultTableModel());
        tableEvent.setRange(7, 9);
        tableEvent.setType(TableModelEvent.DELETE);
        assertEquals(7, tableEvent.getFirstRow());
        assertEquals(9, tableEvent.getLastRow());
        assertEquals(TableModelEvent.DELETE, tableEvent.getType());

        tableEvent.setValues(1, 3, ListEvent.UPDATE);
        assertEquals(1, tableEvent.getFirstRow());
        assertEquals(3, tableEvent.getLastRow());
        assertEquals(TableModelEvent.UPDATE, tableEvent.getType());
    }
}
