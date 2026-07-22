/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;

import java.util.*;
import java.util.function.Function;

/**
 * A utility class containing all sorts of random things that are useful for
 * implementing Glazed Lists but not for using Glazed Lists.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public final class GlazedListsImpl {

    /**
     * A dummy constructor to prevent instantiation of this class
     */
    private GlazedListsImpl() {
        throw new UnsupportedOperationException();
    }

    // Utility Methods // // // // // // // // // // // // // // // // // // //

    /**
     * Replace all elements in the target {@link EventList} with the elements in
     * the source {@link List}.
     *
     * <p>Note that both collections must be sorted prior to execution using the
     * {@link Comparator} specified.
     */
    public static <E> void replaceAll(EventList<E> target, Collection<E> source, boolean updates, Comparator<E> comparator) {
        // use the default comparator if none is specified
        if(comparator == null) {
            comparator = (Comparator<E>)GlazedLists.comparableComparator();
        }

        // walk through each list simultaneously
        int targetIndex = -1;
        Iterator<E> sourceIterator = source.iterator();

        // define marker objects that mean we need new objects
        final E NEW_VALUE_NEEDED = (E)Void.class;
        E targetObject = NEW_VALUE_NEEDED;
        E sourceObject = NEW_VALUE_NEEDED;

        // while there are elements left in either list
        while(true) {

            // do our best to get new objects if necessary
            if(targetObject == NEW_VALUE_NEEDED) {
                if(targetIndex < target.size()) targetIndex++;
                if(targetIndex < target.size()) targetObject = target.get(targetIndex);
            }
            if(sourceObject == NEW_VALUE_NEEDED) {
                if(sourceIterator.hasNext()) sourceObject = sourceIterator.next();
            }

            // we've exhausted our dataset
            if(targetObject == NEW_VALUE_NEEDED && sourceObject == NEW_VALUE_NEEDED) {
                break;
            }

            // figure out if this is an insert, update or delete on the target
            int compareResult;
            if(targetObject == NEW_VALUE_NEEDED) compareResult = 1;
            else if(sourceObject == NEW_VALUE_NEEDED) compareResult = -1;
            else compareResult = comparator.compare(targetObject, sourceObject);

            // the target value precedes the source value, delete it
            if(compareResult < 0) {
                target.remove(targetIndex);
                targetIndex--;
                targetObject = NEW_VALUE_NEEDED;

            // the values are equal, keep them
            } else if(compareResult == 0) {
                if(updates) {
                    target.set(targetIndex, sourceObject);
                }
                targetObject = NEW_VALUE_NEEDED;
                sourceObject = NEW_VALUE_NEEDED;

            // the source value precedes the target, insert it
            } else if(compareResult > 0) {
                target.add(targetIndex, sourceObject);
                targetIndex++;
                sourceObject = NEW_VALUE_NEEDED;
            }
        }
    }

    /**
     * Returns a non-symmetric Comparator that returns 0 if two Objects are
     * equal as specified by {@link Object#equals(Object)}, or 1 otherwise.
     */
    public static <T> Comparator<T> equalsComparator() {
        return new EqualsComparator<>();
    }
    private static class EqualsComparator<T> implements Comparator<T> {
        @Override
        @SuppressWarnings("ComparatorMethodParameterNotUsed")
        public int compare(T alpha, T beta) {
            return Objects.equals(alpha, beta) ? 0 : 1;
        }
    }

    /**
     * Returns a {@link Function} that simply reflects the
     * function's argument as its result.
     */
    public static <E> Function<E,E> identityFunction() {
        return Function.identity();
    }
}
