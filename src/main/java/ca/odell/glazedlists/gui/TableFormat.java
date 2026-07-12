/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.gui;

import org.jspecify.annotations.Nullable;

/**
 * Specifies how a set of records are rendered in a table.
 *
 * @see ca.odell.glazedlists.GlazedLists#tableFormat(Class,String[],String[])
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public interface TableFormat<E> {

    /**
     * The number of columns to display.
     */
    int getColumnCount();

    /**
     * Gets the title of the specified column.
     */
    String getColumnName(int column);

    /**
     * Gets the value of the specified field for the specified object. This
     * is the value that will be passed to the editor and renderer for the
     * column. If you have defined a custom renderer, you may choose to return
     * simply the baseObject.
     *
     * @return the cell value, which may be {@code null}
     */
    @Nullable Object getColumnValue(E baseObject, int column);
}
