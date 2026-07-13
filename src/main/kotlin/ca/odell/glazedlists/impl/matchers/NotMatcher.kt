/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.matchers

import ca.odell.glazedlists.matchers.Matcher

/** Inverts the result of [parent]. */
open class NotMatcher<E>(parent: Matcher<E>?) : Matcher<E> {
    private val parent = requireNotNull(parent) { "parent cannot be null" }

    override fun matches(item: E): Boolean = !parent.matches(item)

    override fun toString(): String = "[NotMatcher parent:$parent]"
}
