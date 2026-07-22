/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.swing;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.SortedList;
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser;
import ca.odell.glazedlists.gui.AdvancedTableFormat;
import ca.odell.glazedlists.gui.TableFormat;
import ca.odell.glazedlists.impl.SortIconFactory;
import ca.odell.glazedlists.impl.gui.SortingStrategy;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Comparator;

/**
 * A TableComparatorChooser is a tool that allows the user to sort a table by clicking
 * on the table's headers. It requires that the {@link JTable}s model is an
 * {@link AdvancedTableModel} with a {@link SortedList} as a source.
 *
 * <p>This class includes custom arrow icons that indicate the sort
 * order. The icons used are chosen based on the current Swing look and feel.
 * Icons are available for the following look and feels: Mac OS X, Metal, Windows.
 *
 * <p>This class supports multiple sort strategies for each
 * column, specified by having muliple comparators for each column. This may
 * be useful when you want to sort a single column in either of two ways. For
 * example, when sorting movie names, "The Phantom Menace" may be sorted under
 * "T" for "The", or "P" for "Phantom".
 *
 * <p>This class supports sorting multiple columns simultaneously.
 * In this mode, the user clicks a first column to sort by, and then the user
 * clicks subsequent columns. The list is sorted by the first column and ties
 * are broken by the second column.
 *
 * <p>If the {@link AdvancedTableModel} uses an {@link AdvancedTableFormat}, its
 * {@link AdvancedTableFormat#getColumnComparator} method will be used to
 * populate the initial column {@link Comparator}s.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 * @see <a href="https://glazedlists.dev.java.net/issues/show_bug.cgi?id=4">Bug 4</a>
 * @see <a href="https://glazedlists.dev.java.net/issues/show_bug.cgi?id=31">Bug 31</a>
 * @see <a href="https://glazedlists.dev.java.net/issues/show_bug.cgi?id=391">Bug 391</a>
 */
public final class TableComparatorChooser<E> extends AbstractTableComparatorChooser<E> {

    /**
     * the header renderer which decorates an underlying renderer
     * (the table header's default renderer) with a sort arrow icon.
     */
    private @Nullable SortArrowHeaderRenderer sortArrowHeaderRenderer;

    /**
     * listen for UI delegate changes to the table header
     */
    private final TableHeaderUIHandler tableHeaderUIHandler = new TableHeaderUIHandler();

    /**
     * listen for table and property change events
     */
    private final TableModelHandler tableModelHandler = new TableModelHandler();

    /**
     * the table being sorted
     */
    private final JTable table;

    /**
     * listeners to sort change events
     */
    private @Nullable ActionListener sortListener;

    /**
     * the sort icons to use
     */
    private static final Icon[] icons = SortIconFactory.loadIcons();

    /**
     * when somebody clicks on the header, update the sorting state
     */
    private final HeaderClickHandler headerClickHandler;

    /**
     * Creates and installs a TableComparatorChooser.
     *
     * @param table       the table with headers that can be clicked on
     * @param sortedList  the sorted list to update
     * @param strategy    an implementations of {@link ca.odell.glazedlists.impl.gui.SortingStrategy}, typically one of
     *                    <ul>
     *                        <li> {@link ca.odell.glazedlists.gui.AbstractTableComparatorChooser#SINGLE_COLUMN}
     *                        <li> {@link ca.odell.glazedlists.gui.AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE}
     * @param tableFormat the TableFormat providing the columns for the table
     */
    private TableComparatorChooser(JTable table, SortedList<E> sortedList, SortingStrategy strategy, TableFormat<? super E> tableFormat) {
        super(sortedList, tableFormat, strategy);

        // save the Swing-specific state
        this.table = table;
        this.table.addPropertyChangeListener("model", tableModelHandler);
        this.table.getTableHeader().addPropertyChangeListener("UI", tableHeaderUIHandler);

        // wrap the default table header with logic that decorates it with a sorting icon
        wrapDefaultTableHeaderRenderer();

        // listen for events on the specified table
        table.getModel().addTableModelListener(tableModelHandler);

        // install the sorting strategy to interpret clicks
        headerClickHandler = new HeaderClickHandler(table, strategy);
    }

    /**
     * A method to wrap the default renderer of the JTableHeader if it does not
     * appear to be wrapped already. This is particularly useful when the UI
     * delegate of the table header changes.
     */
    private void wrapDefaultTableHeaderRenderer() {
        final TableCellRenderer defaultRenderer = table.getTableHeader().getDefaultRenderer();

        // if the renderer does not appear to be wrapped, do it
        if (defaultRenderer != null && defaultRenderer != sortArrowHeaderRenderer) {
            // decorate the default table header renderer with sort arrows
            sortArrowHeaderRenderer = new SortArrowHeaderRenderer(defaultRenderer);
            table.getTableHeader().setDefaultRenderer(sortArrowHeaderRenderer);
        }
    }

    /**
     * Installs a new TableComparatorChooser that responds to clicks on the
     * header of the specified table and uses them to sort the specified
     * <code>sortedList</code> by delegating to the given <code>strategy</code>
     * If at any time the table should no longer sort, the behaviour can be
     * removed calling {@link #dispose()} on the object returned by this method.
     *
     * <p>This method assumes that the JTable is backed by an AdvancedTableModel
     * and it is from that AdvancedTableModel that the TableFormat should be
     * extracted. This is, by far, the typical case and so we provide this
     * simpler install method for convenience.
     *
     * <p>Note, that the default table header renderer will be wrapped with a
     * SortArrowHeaderRenderer, which takes care of rendering the sort icons.
     *
     * @param table      the table with headers that can be clicked on
     * @param sortedList the sorted list to update
     * @param strategy   an implementations of {@link SortingStrategy}, typically one of
     *                   <ul>
     *                       <li> {@link AbstractTableComparatorChooser#SINGLE_COLUMN}
     *                       <li> {@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE}
     *                   </ul>
     * @return TableComparatorChooser object that is responsible for translating
     * mouse clicks on the table header into sorting actions on the sortedList.
     */
    public static <E> TableComparatorChooser<E> install(JTable table, SortedList<E> sortedList, SortingStrategy strategy) {
        return install(table, sortedList, strategy, getTableFormat(table.getModel()));
    }

    /**
     * Installs a new TableComparatorChooser that responds to clicks on the
     * header of the specified table and uses them to sort the specified
     * <code>sortedList</code> by delegating to the given <code>strategy</code>
     * If at any time the table should no longer sort, the behaviour can be
     * removed calling {@link #dispose()} on the object returned by this method.
     *
     * <p>This method makes no assumptions about the TableModel implementation
     * that backs the JTable. As such, it requires the TableFormat as an explicit
     * parameter and expects the TableFormat to be constant (i.e. never changes)
     * for the life of the TableComparatorChooser.
     *
     * <p>Note, that the default table header renderer will be wrapped with a
     * SortArrowHeaderRenderer, which takes care of rendering the sort icons.
     *
     * @param table       the table with headers that can be clicked on
     * @param tableFormat the TableFormat providing the columns for the table
     * @param sortedList  the sorted list to update
     * @param strategy    an implementations of {@link SortingStrategy}, typically one of
     *                    <ul>
     *                        <li> {@link AbstractTableComparatorChooser#SINGLE_COLUMN}
     *                        <li> {@link AbstractTableComparatorChooser#MULTIPLE_COLUMN_MOUSE}
     *                    </ul>
     * @return TableComparatorChooser object that is responsible for translating
     * mouse clicks on the table header into sorting actions on the sortedList.
     */
    public static <E> TableComparatorChooser<E> install(JTable table, SortedList<E> sortedList, SortingStrategy strategy, TableFormat<? super E> tableFormat) {
        return new TableComparatorChooser<>(table, sortedList, strategy, tableFormat);
    }

    @SuppressWarnings("unchecked")
    private static <E> TableFormat<? super E> getTableFormat(TableModel tableModel) {
        return ((AdvancedTableModel<E>) tableModel).getTableFormat();
    }

    /**
     * Registers the specified {@link ActionListener} to receive notification whenever
     * the {@link JTable} is sorted by this {@link TableComparatorChooser}.
     */
    public void addSortActionListener(ActionListener sortActionListener) {
        sortListener = AWTEventMulticaster.add(sortListener, sortActionListener);
    }


    /**
     * Examines the current {@link Comparator} of the SortedList and
     * adds icons to the table header renderers in response.
     *
     * <p>To do this, clicks are injected into each of the
     * corresponding <code>ColumnClickTracker</code>s.
     */
    @Override
    protected void redetectComparator(@Nullable Comparator<? super E> currentComparator) {
        super.redetectComparator(currentComparator);

        // force the table header to redraw itself
        table.getTableHeader().revalidate();
        table.getTableHeader().repaint();
    }

    /**
     * Updates the comparator in use and applies it to the table.
     */
    @Override
    protected void rebuildComparator() {
        super.rebuildComparator();

        // force the table header to redraw itself
        table.getTableHeader().revalidate();
        table.getTableHeader().repaint();

        // notify interested listeners that the sorting has changed
        if (sortListener != null)
            sortListener.actionPerformed(new ActionEvent(this, 0, "sort"));
    }

    /**
     * Gets the sorting style currently applied to the specified column.
     */
    @Override
    protected int getSortingStyle(int column) {
        return super.getSortingStyle(table.convertColumnIndexToModel(column));
    }

    /**
     * Determines if the specified mouse event shall be handled by this
     * {@link TableComparatorChooser}. The default implementation handles only clicks
     * with the left mouse button. Extending classes can customize which mouse
     * events the table comparator chooser responds to by overriding this method.
     *
     * <p>As of 2005/12/20, this method is no longer called when the
     * corresponding mouse press event was a popup trigger. In effect, if this
     * is a right-click on Windows or a 'control-click' on the Mac.
     *
     * <p>As of 2008/02/05, this method is no longer called when the
     * Cursor over the JTableHeader indicates a column resize is expected to
     * take place, rather than a change in sort.
     */
    private boolean isSortingMouseEvent(MouseEvent e) {
        // skip the sort if it's not button 1
        return e.getButton() == MouseEvent.BUTTON1;
    }


    /**
     * Releases the resources consumed by this {@link TableComparatorChooser} so that it
     * may eventually be garbage collected.
     *
     * <p>A {@link TableComparatorChooser} will be garbage collected without a call to
     * {@code dispose()}, but not before its source {@link EventList} is garbage
     * collected. By calling {@code dispose()}, you allow the {@link TableComparatorChooser}
     * to be garbage collected before its source {@link EventList}. This is
     * necessary for situations where an {@link TableComparatorChooser} is short-lived but
     * its source {@link EventList} is long-lived.
     *
     * <p><strong><font color="#FF0000">Warning:</font></strong> It is an error
     * to call any method on a {@link TableComparatorChooser} after it has been disposed.
     */
    @Override
    protected void disposeInternal() {
        headerClickHandler.dispose();

        // if the default renderer within the table header is our sort arrow renderer,
        // uninstall it by restoring the table header's original default renderer
        if (sortArrowHeaderRenderer != null && table.getTableHeader().getDefaultRenderer() == sortArrowHeaderRenderer)
            table.getTableHeader().setDefaultRenderer(sortArrowHeaderRenderer.getDelegateRenderer());

        // remove our listeners from the table's header and model
        table.getModel().removeTableModelListener(tableModelHandler);
        table.removePropertyChangeListener("model", tableModelHandler);
        table.getTableHeader().removePropertyChangeListener("UI", tableHeaderUIHandler);
        sortListener = null;
    }

    /**
     * Nested Listener class handles changes in the UI delegate for the table's
     * header. It responds by rewrapping the default renderer for the table
     * header, if it was replaced in the course of installing the new
     * TableHeaderUI.
     */
    private class TableHeaderUIHandler implements PropertyChangeListener {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            wrapDefaultTableHeaderRenderer();
        }
    }

    /**
     * Nested Listener class handles TableModelEvents and PropertyChangeEvents.
     * TableModelEvents tell us when the TableModel's data changes in place.
     * PropertyChangeEvents tell us when the TableModel has been replaced.
     */
    private class TableModelHandler implements TableModelListener, PropertyChangeListener {

        /**
         * This method is only called when the TableModel of the JTable is
         * changed. It allows us to stop listening to the previous
         * AdvancedTableModel and start listening to the new one. It also resets
         * the sorting state of this TableComparatorChooser.
         */
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            final TableModel oldModel = (TableModel) evt.getOldValue();
            final TableModel newModel = (TableModel) evt.getNewValue();

            oldModel.removeTableModelListener(this);
            newModel.addTableModelListener(this);

            if (newModel instanceof AdvancedTableModel<?>) {

                // the table structure has probably changed due to the new AdvancedTableModel
                // so we reset the TableFormat (which clears the sorting state)
                setTableFormat(getTableFormat(newModel));
            }
        }

        /**
         * When the number of columns changes in the table, we need to
         * clear the comparators and columns.
         */
        @Override
        public void tableChanged(TableModelEvent event) {
            if (event.getFirstRow() == TableModelEvent.HEADER_ROW && event.getColumn() == TableModelEvent.ALL_COLUMNS) {
                if (table.getModel() instanceof AdvancedTableModel) {
                    // the table structure may have changed due to a change in the table format
                    // so we conservatively reset the TableFormat on this TableComparatorChooser
                    setTableFormat(getTableFormat(table.getModel()));
                }
            }

            // if the comparator has changed
            final Comparator<? super E> currentComparator = getSortedList().getComparator();
            if (currentComparator != sortedListComparator) {
                redetectComparator(currentComparator);
            }
        }
    }

    /**
     * The SortArrowHeaderRenderer simply delegates most of the rendering
     * to a given delegate renderer, and adds an icon to indicate sorting
     * direction. This allows TableComparatorChooser to work equally well
     * with any custom TableCellRenderers that are used as the default
     * table header renderer.
     *
     * <p>This class fails to add indicator arrows on table headers where the
     * default table header render does not return a JLabel or does not
     * implement {@link SortableRenderer}.
     *
     * <p>We implement UIResource here so that changes to the UI delegate of
     * the JTableHeader will replace our renderer with a new one that is
     * appropriate for the new LaF. We, in turn, react to the change of UI
     * delegates by *re-wrapping* the new default renderer with our sort icon
     * injection logic.
     */
    private final class SortArrowHeaderRenderer implements TableCellRenderer, UIResource {

        /**
         * the renderer to which we delegate
         */
        private TableCellRenderer delegateRenderer;

        /**
         * Creates a new SortArrowHeaderRenderer that attempts to decorate the
         * given <code>delegateRenderer</code> which a sorting icon.
         */
        public SortArrowHeaderRenderer(TableCellRenderer delegateRenderer) {
            this.delegateRenderer = delegateRenderer;
        }

        /**
         * Returns the delegate renderer that is decorated with sort arrows.
         */
        public TableCellRenderer getDelegateRenderer() {
            return delegateRenderer;
        }

        /**
         * Renders the header in the default way but with the addition of an
         * icon to indicate sorting state.
         */
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            // if column index is negative, just call the delegate renderer
            // this is a special case for JideTable with nested table columns
            if (column < 0)
                return getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

            final Icon sortIcon = icons[getSortingStyle(column)];
            final Component rendered;

            // 1. look for our custom SortableRenderer interface
            if (delegateRenderer instanceof SortableRenderer) {
                ((SortableRenderer) delegateRenderer).setSortIcon(sortIcon);
                rendered = getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // 2. Otherwise check whether the rendered component is a JLabel (this is the case of the default header renderer)
            }
            else {
                rendered = getDelegateTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

                // we check for a JLabel rather than a DefaultTableCellRenderer to support WinLAF,
                // which installs a decorator over the DefaultTableCellRenderer
                if (rendered instanceof JLabel label) {
                    label.setIcon(sortIcon);
                    label.setHorizontalTextPosition(SwingConstants.LEADING);
                }
            }

            return rendered;
        }

        /**
         * Attempts to retrieve the decorated Component from the delegate
         * renderer. If a RuntimeException occurs, this method replaces the
         * delegate renderer with a {@link DefaultTableCellRenderer} and
         * requests the Component from it. This exists because our decorating
         * approach is the victim of a SUN bug in WindowsTableHeaderUI:
         * <a href="http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6429812">JDK-6429812</a>
         * <p>
         * See also more information reported by Eric Burke here:
         * <a href="http://stuffthathappens.com/blog/2007/10/02/rich-client-developers-avoid-java-6/">Eric Burke's report</a>
         */
        private Component getDelegateTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            try {
                return delegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
            catch (RuntimeException e) {
                delegateRenderer = new DefaultTableCellRenderer();
                return delegateRenderer.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            }
        }
    }

    /**
     * Handle clicks to the table's header by adjusting the sorting state.
     */
    private class HeaderClickHandler extends MouseAdapter {
        private final JTable table;
        private final SortingStrategy delegate;
        private boolean mouseEventIsPerformingPopupTrigger;

        public HeaderClickHandler(JTable table, SortingStrategy delegate) {
            this.table = table;
            this.delegate = delegate;
            table.getTableHeader().addMouseListener(this);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            // if the MouseEvent is popping up a context menu, do not sort
            if (mouseEventIsPerformingPopupTrigger)
                return;

            // if the cursor indicates we're resizing columns, do not sort
            if (table.getTableHeader().getCursor() == Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR))
                return;

            // check if there is any other reason to ignore this MouseEvent
            if (!isSortingMouseEvent(e))
                return;

            final TableColumnModel columnModel = table.getColumnModel();
            final int viewColumn = columnModel.getColumnIndexAtX(e.getX());
            final int column = table.convertColumnIndexToModel(viewColumn);
            final int clicks = e.getClickCount();

            if (clicks >= 1 && column != -1) {
                final boolean shift = e.isShiftDown();
                final boolean control = e.isControlDown() || e.isMetaDown();
                delegate.columnClicked(sortingState, column, clicks, shift, control);
            }
        }

        /**
         * Keep track of whether the mouse is triggering a popup, so we
         * can avoid sorting the table when the poor user just wants to show
         * a context menu.
         */
        @Override
        public void mousePressed(MouseEvent mouseEvent) {
            this.mouseEventIsPerformingPopupTrigger = mouseEvent.isPopupTrigger();
        }

        public void dispose() {
            table.getTableHeader().removeMouseListener(this);
        }
    }
}
