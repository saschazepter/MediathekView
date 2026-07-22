/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.gui;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.impl.sort.ComparatorChain;
import ca.odell.glazedlists.impl.sort.ReverseComparator;
import ca.odell.glazedlists.impl.sort.TableColumnComparator;
import org.jspecify.annotations.Nullable;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;

/**
 * Keep track of which columns are sorted and how. This is
 * largely independent of how that state is applied to a
 * <code>SortedList</code>, which is managed independently by
 * <code>TableComparatorChooser</code>.
 *
 * <p>Users must explicity call {@link #fireSortingChanged()} in order
 * to prepare a new Comparator for the target table.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public final class SortingState<E> {

    /** the sorting style on a column is used for icon choosing */
    private static final int COLUMN_UNSORTED = 0;
    private static final int COLUMN_PRIMARY_SORTED = 1;
    private static final int COLUMN_PRIMARY_SORTED_REVERSE = 2;
    private static final int COLUMN_PRIMARY_SORTED_ALTERNATE = 3;
    private static final int COLUMN_PRIMARY_SORTED_ALTERNATE_REVERSE = 4;
    private static final int COLUMN_SECONDARY_SORTED = 5;
    private static final int COLUMN_SECONDARY_SORTED_REVERSE = 6;
    private static final int COLUMN_SECONDARY_SORTED_ALTERNATE = 7;
    private static final int COLUMN_SECONDARY_SORTED_ALTERNATE_REVERSE = 8;

    /** the columns and their click counts in indexed order */
    private List<SortingColumn> sortingColumns = List.of();

    /** a list that contains all ColumnClickTrackers with non-zero click counts in their visitation order */
    private final List<SortingColumn> recentlyClickedColumns = new ArrayList<>(2);

    /** whom to notify when the sorting state is chaged */
    private final PropertyChangeSupport changeSupport = new PropertyChangeSupport(this);


    public void fireSortingChanged() {
        changeSupport.firePropertyChange("comparator", null, null);
    }
    public void addPropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.addPropertyChangeListener(listener);
    }
    public void removePropertyChangeListener(PropertyChangeListener listener) {
        changeSupport.removePropertyChangeListener(listener);
    }

    public @Nullable Comparator<E> buildComparator() {
        // build a new comparator
        if(recentlyClickedColumns.isEmpty()) {
            return null;
        } else {
            List<Comparator<E>> comparators = new ArrayList<>(recentlyClickedColumns.size());
            for (SortingColumn sortingColumn : recentlyClickedColumns) {
                Comparator<E> comparator = sortingColumn.getComparator();
                if (comparator == null)
                    throw new IllegalStateException();
                comparators.add(comparator);
            }

            return GlazedLists.chainComparators(comparators);
        }
    }


    public boolean disableSortingForColumn(int column) {
        final SortingColumn sortingColumn = requireColumn(column);
        final boolean activeSortRemoved = recentlyClickedColumns.remove(sortingColumn);
        sortingColumn.clear();
        sortingColumn.getComparators().clear();
        return activeSortRemoved;
    }

    public boolean appendComparator(int column, int comparatorIndex, boolean reverse, boolean multipleColumnSort) {
        final SortingColumn sortingColumn = validateComparator(column, comparatorIndex);
        if(recentlyClickedColumns.contains(sortingColumn)) return false;

        if (!multipleColumnSort) clearComparators();

        // add clicks to the specified column
        sortingColumn.setComparatorIndex(comparatorIndex);
        sortingColumn.setReverse(reverse);

        // rebuild the clicked column list
        recentlyClickedColumns.add(sortingColumn);
        return true;
    }

    public SortingColumn validateComparator(int column, int comparatorIndex) {
        final SortingColumn sortingColumn = requireColumn(column);
        final int comparatorCount = sortingColumn.getComparators().size();
        if (comparatorIndex < 0 || comparatorIndex >= comparatorCount) {
            throw new IllegalArgumentException("invalid comparator index " + comparatorIndex + ", must be in range [0, " + comparatorCount + ")");
        }
        return sortingColumn;
    }

    private SortingColumn requireColumn(int column) {
        if (column < 0 || column >= sortingColumns.size()) {
            throw new IllegalArgumentException("invalid column " + column + ", must be in range [0, " + sortingColumns.size() + ")");
        }
        return sortingColumns.get(column);
    }

    public boolean detectStateFromComparator(@Nullable Comparator<?> foreignComparator) {
        // Clear the current click counts
        clearComparators();

        // Populate a list of Comparators
        final List<? extends Comparator<?>> comparatorsList;
        if(foreignComparator == null) {
            comparatorsList = Collections.emptyList();
        } else if(foreignComparator instanceof ComparatorChain<?> chain) {
            comparatorsList = Arrays.asList(chain.getComparators());
        } else {
            comparatorsList = Collections.singletonList(foreignComparator);
        }

        boolean fullyDetected = true;

        // walk through the list of Comparators and assign click counts
        for (Comparator<?> comparator : comparatorsList) {
            // get the current comparator
            boolean reverse = false;
            if (comparator instanceof ReverseComparator<?> reverseComparator) {
                reverse = true;
                comparator = reverseComparator.getSourceComparator();
            }

            // discover where to add clicks for this comparator
            boolean detected = false;
            for (SortingColumn sortingColumn : sortingColumns) {
                if (recentlyClickedColumns.contains(sortingColumn)) {
                    continue;
                }
                int comparatorIndex = sortingColumn.getComparators().indexOf(comparator);
                if (comparatorIndex != -1) {
                    sortingColumn.setComparatorIndex(comparatorIndex);
                    sortingColumn.setReverse(reverse);
                    recentlyClickedColumns.add(sortingColumn);
                    detected = true;
                    break;
                }
            }
            fullyDetected &= detected;
        }
        return fullyDetected;
    }

    public void clearComparators() {
        // clear the click counts
        for (SortingColumn sortingColumn : recentlyClickedColumns) {
            sortingColumn.clear();
        }
        recentlyClickedColumns.clear();
    }

    /**
     * When the column model is changed, this resets the column clicks and
     * comparator list for each column.
     */
    public void rebuildColumns(TableFormat<? super E> tableFormat) {
        // build the column click trackers
        final int columnCount = tableFormat.getColumnCount();

        sortingColumns = new ArrayList<>(columnCount);
        for(int i = 0; i < columnCount; i++) {
            sortingColumns.add(new SortingColumn(tableFormat, i));
        }

        recentlyClickedColumns.clear();
    }


    public List<SortingColumn> getColumns() {
        return sortingColumns;
    }

    public List<SortingColumn> getRecentlyClickedColumns() {
        return recentlyClickedColumns;
    }


    public class SortingColumn {
        /** the column whose sorting state is being managed */
        private final int column;
        /** the sequence of comparators for this column */
        private final List<Comparator<E>> comparators = new ArrayList<>(1);
        /** whether this column is sorted in reverse order */
        private boolean reverse;
        /** the comparator in the comparator list to sort by */
        private int comparatorIndex = -1;


        public SortingColumn(TableFormat<? super E> tableFormat, int column) {
            this.column = column;

            // add the preferred comparator for AdvancedTableFormat
            if(tableFormat instanceof AdvancedTableFormat<?> advancedTableFormat) {
                Comparator<?> columnComparator = advancedTableFormat.getColumnComparator(column);
                if(columnComparator != null) comparators.add(new TableColumnComparator<>(tableFormat, column, columnComparator));
            // otherwise just add the default comparator
            } else {
                comparators.add(new TableColumnComparator<>(tableFormat, column));
            }
        }

        public void clear() {
            this.reverse = false;
            this.comparatorIndex = -1;
        }

        public int getColumn() {
            return column;
        }

        /**
         * Gets the index of the comparator to use for this column.
         */
        public void setComparatorIndex(int comparatorIndex) {
            assert(comparatorIndex < comparators.size());
            this.comparatorIndex = comparatorIndex;
        }
        public int getComparatorIndex() {
            return comparatorIndex;
        }

        /**
         * Gets the list of comparators for this column.
         */
        public List<Comparator<E>> getComparators() {
            return comparators;
        }

        /**
         * Gets the current best comparator to sort this column.
         */
        public @Nullable Comparator<E> getComparator() {
            if(comparatorIndex == -1) return null;
            Comparator<E> comparator = comparators.get(getComparatorIndex());
            if(isReverse()) comparator = GlazedLists.reverseComparator(comparator);
            return comparator;
        }

        /**
         * Get whether this column is in reverse order.
         */
        public boolean isReverse() {
            return reverse;
        }
        public void setReverse(boolean reverse) {
            this.reverse = reverse;
        }

        /**
         * Gets the sorting style for this column.
         */
        public int getSortingStyle() {
            if(comparatorIndex == -1) return COLUMN_UNSORTED;
            boolean primaryColumn = !recentlyClickedColumns.isEmpty() && recentlyClickedColumns.getFirst() == this;
            boolean primaryComparator = getComparatorIndex() == 0;

            if(primaryColumn) {
                if(!isReverse()) {
                    if(primaryComparator) return COLUMN_PRIMARY_SORTED;
                    else return COLUMN_PRIMARY_SORTED_ALTERNATE;
                } else {
                    if(primaryComparator) return COLUMN_PRIMARY_SORTED_REVERSE;
                    else return COLUMN_PRIMARY_SORTED_ALTERNATE_REVERSE;
                }
            } else {
                if(!isReverse()) {
                    if(primaryComparator) return COLUMN_SECONDARY_SORTED;
                    else return COLUMN_SECONDARY_SORTED_ALTERNATE;
                } else {
                    if(primaryComparator) return COLUMN_SECONDARY_SORTED_REVERSE;
                    else return COLUMN_SECONDARY_SORTED_ALTERNATE_REVERSE;
                }
            }
        }
    }
}
