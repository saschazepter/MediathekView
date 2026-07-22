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

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.gui.WritableTableFormat;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.swing.SwingUtilities;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class DefaultEventTableModelJavaInteropTest {
    @Test
    void constructorsProtectedHooksAndFactorySamRemainAvailableToJava() throws Exception {
        BasicEventList<String> source = new BasicEventList<>();
        JavaTableModel<String> model = new JavaTableModel<>(source, new StringTableFormat());
        TableModelEventAdapter.Factory<String> factory = ignored -> model.getEventAdapter();

        SwingUtilities.invokeAndWait(() -> source.add("value"));

        assertTrue(model.handledListChange);
        assertSame(source, model.sourceField());
        assertAdvancedModelContract(model);
        assertSame(model.getEventAdapter(), factory.create(model));
        assertSame(model.getEventAdapter(), factory.apply(model));
        model.dispose();
    }

    private static void assertAdvancedModelContract(AdvancedTableModel<String> model) {
        assertNotNull(model.getTableFormat());
    }

    @Test
    void nullWritableResultStillDiscardsAnEdit() {
        BasicEventList<String> source = new BasicEventList<>();
        source.add("original");
        DefaultEventTableModel<String> model = new DefaultEventTableModel<>(
                source,
                true,
                new DiscardingTableFormat());

        model.setValueAt(null, 0, 0);

        assertEquals("original", source.getFirst());
        model.dispose();
    }

    private static final class JavaTableModel<E> extends DefaultEventTableModel<E> {
        private boolean handledListChange;

        private JavaTableModel(EventList<E> source, TableFormat<? super E> tableFormat) {
            super(source, false, tableFormat);
        }

        @Override
        protected void handleListChange(ListEvent<E> listChanges) {
            handledListChange = true;
            super.handleListChange(listChanges);
        }

        private @Nullable EventList<E> sourceField() {
            return source;
        }
    }

    private static final class StringTableFormat implements TableFormat<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "value";
        }

        @Override
        public String getColumnValue(String baseObject, int column) {
            return baseObject;
        }
    }

    private static final class DiscardingTableFormat implements WritableTableFormat<String> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "value";
        }

        @Override
        public String getColumnValue(String baseObject, int column) {
            return baseObject;
        }

        @Override
        public boolean isEditable(String baseObject, int column) {
            return true;
        }

        @Override
        @SuppressWarnings("NullableProblems")
        public @Nullable String setColumnValue(String baseObject, Object editedValue, int column) {
            return null;
        }
    }
}
