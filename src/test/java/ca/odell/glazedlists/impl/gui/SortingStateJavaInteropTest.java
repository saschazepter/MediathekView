package ca.odell.glazedlists.impl.gui;

import ca.odell.glazedlists.gui.TableFormat;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullMarked;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@NullMarked
class SortingStateJavaInteropTest {
    @Test
    void innerSortingColumnRemainsSubclassableFromJava() {
        SortingState<Integer> state = new SortingState<>();
        TableFormat<Integer> format = new IntegerTableFormat();
        JavaSortingColumn column = new JavaSortingColumn(state, format, 0);

        column.setComparatorIndex(0);
        column.setReverse(true);

        assertEquals(0, column.getColumn());
        assertEquals(0, column.getComparatorIndex());
        assertTrue(column.isReverse());
        column.clear();
        assertEquals(-1, column.getComparatorIndex());
    }

    private static final class JavaSortingColumn extends SortingState<Integer>.SortingColumn {
        private JavaSortingColumn(SortingState<Integer> state, TableFormat<? super Integer> format, int column) {
            state.super(format, column);
        }

        @Override
        public void clear() {
            super.clear();
        }
    }

    private static final class IntegerTableFormat implements TableFormat<Integer> {
        @Override
        public int getColumnCount() {
            return 1;
        }

        @Override
        public String getColumnName(int column) {
            return "Value";
        }

        @Override
        public Object getColumnValue(Integer baseObject, int column) {
            return baseObject;
        }
    }
}
