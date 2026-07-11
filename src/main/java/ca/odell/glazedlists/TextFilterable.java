/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists;

import java.util.List;
import java.util.function.Consumer;

/**
 * An item that can be compared to a list of filters to see if it matches.
 *
 * @see <a href="http://publicobject.com/glazedlists/tutorial/">Glazed Lists Tutorial</a>
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
@FunctionalInterface
public interface TextFilterable extends Consumer<List<String>> {

    /**
     * Gets this object as a list of Strings. These Strings
     * should contain all object information so that it can be compared
     * to the filter set.
     *
     * @param baseList a list that the implementor shall add their filter
     *      strings to via <code>baseList.add()</code>. This may be a non-empty
     *      List and it is an error to call any method other than add().
     */
    void getFilterStrings(List<String> baseList);

    @Override
    default void accept(List<String> baseList) {
        getFilterStrings(baseList);
    }
}
