/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.matchers;

import ca.odell.glazedlists.matchers.Matcher;

/**
 * A Matcher that matches if any child elements match.
 */
public class OrMatcher<E> implements Matcher<E> {

    /** The Matchers being combined with an "or" operator. */
    private final Matcher<? super E>[] matchers;

    @SafeVarargs
    public OrMatcher(Matcher<? super E>... matchers) {
        this.matchers = matchers;
    }

    /** {@inheritDoc} */
    @Override
    public boolean matches(E item) {
        for (Matcher<? super E> matcher : matchers) {
            if (matcher.matches(item))
                return true;
        }
        return false;
    }
}