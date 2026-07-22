/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.gui;

import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.impl.gui.MouseOnlySortingStrategy;
import ca.odell.glazedlists.impl.gui.SortingState;
import ca.odell.glazedlists.impl.gui.SortingStrategy;
import org.jspecify.annotations.Nullable;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A TableComparatorChooser is a tool that allows the user to sort a table
 * widget by clicking on the table's headers. It requires that the table has a
 * SortedList as a source as the sorting on that list is used.
 *
 * @author <a href="mailto:kevin@swank.ca">Kevin Maltby</a>
 */
public abstract class AbstractTableComparatorChooser<E> {

    public record SortKey(int column, int comparatorIndex, boolean reverse) {
    }

    /**
     * Emulate the sorting behaviour of Windows Explorer and Mac OS X Finder.
     *
     * <p>Single clicks toggles between forward and reverse. If multiple comparators
     * are available for a particular column, they will be cycled in order.
     *
     * <p>At most one column can be sorted at a time.
     */
    public static final SortingStrategy SINGLE_COLUMN = new MouseOnlySortingStrategy(false);

    /**
     * Sort multiple columns without use of the keyboard.  Single clicks cycle
     * through comparators, double clicks clear all secondary sorts before
     * performing the normal behaviour.
     *
     * <p>This is the original sorting strategy provided by Glazed Lists, with a
     * limitation that it is impossible to clear a sort order that is already in
     * place. It's designed to be used with multiple columns and multiple comparators
     * per column.
     *
     * <p>The overall behaviour is as follows:
     *
     * <li>Click: sort this column. If it's already sorted, reverse the sort order.
     * If its already reversed, sort using the column's next comparator in forward
     * order. If there are no more comparators, go to the first comparator. If there
     * are multiple sort columns, sort this column after those columns.
     *
     * <li>Double click: like a single click, but clear all sorting columns first.
     */
    public static final SortingStrategy MULTIPLE_COLUMN_MOUSE = new MouseOnlySortingStrategy(true);


    private final boolean multipleColumnSort;

    /** the sorted list to choose the comparators for */
    private final SortedList<E> sortedList;

    private boolean disposed;

    /** the potentially foreign comparator associated with the sorted list */
    protected @Nullable Comparator<? super E> sortedListComparator;

    /** whether every component of the cached comparator is represented by the sorting state */
    private boolean sortedListComparatorFullyRepresented;

    /** manage which columns are sorted and in which order */
    protected final SortingState<E> sortingState;

    /**
     * Create a {@link AbstractTableComparatorChooser} that sorts the specified
     * {@link SortedList} over the specified columns.
     */
    protected AbstractTableComparatorChooser(SortedList<E> sortedList, TableFormat<? super E> tableFormat, SortingStrategy sortingStrategy) {
        this.sortedList = sortedList;
        this.multipleColumnSort = sortingStrategy.supportsMultipleColumnSorting();
        this.sortingState = new SortingState<>();
        this.sortingState.rebuildColumns(tableFormat);
        this.sortedListComparator = readSortedListComparator();
        this.sortedListComparatorFullyRepresented = sortingState.detectStateFromComparator(sortedListComparator);

        this.sortingState.addPropertyChangeListener(new SortingStateListener());
    }


    /**
     * Handle changes to the sorting state by applying the new comparator
     * to the {@link SortedList}.
     */
    private class SortingStateListener implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent propertyChangeEvent) {
            rebuildComparator();
        }
    }

    /**
     * Updates the comparator in use and applies it to the table.
     */
    protected void rebuildComparator() {
        final Comparator<E> rebuiltComparator = sortingState.buildComparator();
        final SortedList<E> currentSortedList = getSortedList();

        // select the new comparator
        currentSortedList.getReadWriteLock().writeLock().lock();
        try {
            currentSortedList.setComparator(rebuiltComparator);
            sortedListComparator = rebuiltComparator;
            sortedListComparatorFullyRepresented = true;
        } finally {
            currentSortedList.getReadWriteLock().writeLock().unlock();
        }
    }

    /**
     * Returns the sorted list, rejecting access after this chooser is disposed.
     */
    protected final SortedList<E> getSortedList() {
        if (disposed) {
            throw new IllegalStateException("TableComparatorChooser has been disposed");
        }
        return sortedList;
    }

    private @Nullable Comparator<? super E> readSortedListComparator() {
        final SortedList<E> currentSortedList = getSortedList();
        currentSortedList.getReadWriteLock().readLock().lock();
        try {
            return currentSortedList.getComparator();
        } finally {
            currentSortedList.getReadWriteLock().readLock().unlock();
        }
    }

    /**
     * Adjusts the TableFormat this comparator chooser uses when selecting
     * comparators. Calling this method will clear any active sorting.
     */
    protected final void setTableFormat(TableFormat<? super E> tableFormat) {
        // handle a change in the layout of our columns
        sortingState.rebuildColumns(tableFormat);
        sortingState.fireSortingChanged();
    }


    /**
     * Disables sorting for the specified column and clears an active sort on it.
     */
    public void disableSortingForColumn(int column) {
        if (sortingState.disableSortingForColumn(column)) {
            sortingState.fireSortingChanged();
        }
    }

    public List<SortKey> getSortKeys() {
        final List<SortKey> sortKeys = new ArrayList<>(sortingState.getRecentlyClickedColumns().size());
        for (SortingState<E>.SortingColumn sortingColumn : sortingState.getRecentlyClickedColumns()) {
            sortKeys.add(new SortKey(sortingColumn.getColumn(), sortingColumn.getComparatorIndex(), sortingColumn.isReverse()));
        }
        return List.copyOf(sortKeys);
    }


    /**
     * Append the comparator specified by the column, comparator index and reverse
     * parameters to the end of the sequence of comparators this
     * {@link AbstractTableComparatorChooser} is sorting the {@link SortedList}
     * by.
     *
     * <p><i>Append</i> implies that if this {@link AbstractTableComparatorChooser}
     * is already sorting that list by another column, this comparator will only
     * be used to break ties from that {@link Comparator}. If the table is already
     * sorting by the specified column, it will be silently discarded.
     *
     * <p>Suppose we're currently not sorting the table, this method will cause
     * the table to be sorted by the column specified. If we are sorting the table
     * by some column c, this will sort by that column first and the column
     * specified here second.
     *
     * <p>If this {@link AbstractTableComparatorChooser} doesn't support multiple
     * column sort, this will replace the current {@link Comparator} rather than
     * appending to it.
     *
     * @param column the column to sort by
     * @param comparatorIndex the comparator to use, specify <code>0</code> for the
     *      default comparator.
     * @param reverse whether to reverse the specified comparator.
     */
    public void appendComparator(int column, int comparatorIndex, boolean reverse) {
        if (sortingState.appendComparator(column, comparatorIndex, reverse, multipleColumnSort)) {
            sortingState.fireSortingChanged();
        }
    }

    /**
     * Atomically replaces the complete sorting state. All keys are validated
     * before the current comparator is changed.
     */
    public boolean setSortKeys(List<SortKey> sortKeys) {
        Objects.requireNonNull(sortKeys, "sortKeys");
        for (SortKey sortKey : sortKeys) {
            Objects.requireNonNull(sortKey, "sortKeys contains null");
            sortingState.validateComparator(sortKey.column(), sortKey.comparatorIndex());
        }

        final List<SortKey> normalizedSortKeys = normalizeSortKeys(sortKeys);
        if (getSortKeys().equals(normalizedSortKeys)
                && sortedListComparatorFullyRepresented
                && readSortedListComparator() == sortedListComparator) {
            return false;
        }

        sortingState.clearComparators();
        for (SortKey sortKey : normalizedSortKeys) {
            sortingState.appendComparator(sortKey.column(), sortKey.comparatorIndex(), sortKey.reverse(), true);
        }
        sortingState.fireSortingChanged();
        return true;
    }

    private List<SortKey> normalizeSortKeys(List<SortKey> sortKeys) {
        final List<SortKey> normalizedSortKeys = new ArrayList<>(sortKeys.size());
        final Set<Integer> usedColumns = new HashSet<>();
        for (SortKey sortKey : sortKeys) {
            if (multipleColumnSort) {
                if (usedColumns.add(sortKey.column())) normalizedSortKeys.add(sortKey);
            } else if (normalizedSortKeys.isEmpty() || normalizedSortKeys.getFirst().column() != sortKey.column()) {
                normalizedSortKeys.clear();
                normalizedSortKeys.add(sortKey);
            }
        }
        return normalizedSortKeys;
    }

    /**
     * Clear all sorting state and set the {@link SortedList} to use its
     * source order.
     */
    public void clearComparator() {
        if (sortingState.getRecentlyClickedColumns().isEmpty() && readSortedListComparator() == null) return;

        sortingState.clearComparators();
        sortingState.fireSortingChanged();
    }

    /**
     * Examines the current {@link Comparator} of the SortedList and
     * adds icons to the table header renderers in response.
     *
     * <p>To do this, clicks are injected into each of the
     * corresponding <code>ColumnClickTracker</code>s.
     */
    protected void redetectComparator(@Nullable Comparator<? super E> currentComparator) {
        sortedListComparator = currentComparator;
        sortedListComparatorFullyRepresented = sortingState.detectStateFromComparator(currentComparator);
    }

    /**
     * Gets the sorting style currently applied to the specified column.
     */
    protected int getSortingStyle(int column) {
        return sortingState.getColumns().get(column).getSortingStyle();
    }

    public final void dispose() {
        if (disposed) return;

        disposed = true;
        disposeInternal();
        sortedListComparator = null;
        sortedListComparatorFullyRepresented = false;
    }

    protected void disposeInternal() {
    }
}
