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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.odell.glazedlists.swing;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.TableFormat;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import javax.swing.JTable;
import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class TableComparatorChooserJavaInteropTest {

    @Test
    void staticInstallOverloadsRemainCallableFromJava() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            final BasicEventList<Row> source = new BasicEventList<>();
            final SortedList<Row> sorted = new SortedList<>(source, null);
            final TableFormat<Row> format = new TableFormat<>() {
                @Override
                public int getColumnCount() {
                    return 1;
                }

                @Override
                public @NonNull String getColumnName(int column) {
                    return "Value";
                }

                @Override
                public Object getColumnValue(Row baseObject, int column) {
                    return baseObject.value;
                }
            };

            final JTable explicitFormatTable = new JTable(0, 1);
            final TableComparatorChooser<Row> explicitFormatChooser = TableComparatorChooser.install(
                    explicitFormatTable,
                    sorted,
                    AbstractTableComparatorChooser.SINGLE_COLUMN,
                    format);
            assertNotNull(explicitFormatChooser);
            explicitFormatChooser.dispose();

            final JTable inferredFormatTable = new JTable(new DefaultEventTableModel<>(sorted, format));
            final TableComparatorChooser<Row> inferredFormatChooser = TableComparatorChooser.install(
                    inferredFormatTable,
                    sorted,
                    AbstractTableComparatorChooser.SINGLE_COLUMN);
            assertNotNull(inferredFormatChooser);
            inferredFormatChooser.dispose();
        });
    }

    private record Row(int value) {
    }
}
