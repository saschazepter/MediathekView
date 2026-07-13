/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.matchers

import ca.odell.glazedlists.matchers.Matcher

/** Matches when at least one child matcher matches. */
open class OrMatcher<E>(vararg matchers: Matcher<in E>) : Matcher<E> {
    private val matchers = matchers

    override fun matches(item: E): Boolean = matchers.any { it.matches(item) }
}
