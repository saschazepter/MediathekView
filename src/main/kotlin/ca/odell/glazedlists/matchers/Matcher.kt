/* Glazed Lists                                                 (c) 2003-2014 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.matchers

import java.util.function.Predicate

/** Determines whether a value matches a filter. */
fun interface Matcher<E> : Predicate<E> {
    fun matches(item: E): Boolean

    override fun test(item: E): Boolean = matches(item)
}
