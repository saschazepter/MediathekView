/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.event;

// the core Glazed Lists package
import ca.odell.glazedlists.CompositeList;
import ca.odell.glazedlists.ListSelection;

/**
 * Define a strategy for managing dependencies in the observer pattern.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public interface ListEventPublisher {

    /**
     * Attach the specified listener to the specified subject, so that when
     * dependencies are being prepared, notifying the listener will be
     * considered equivalent to notifying the subject. This makes it possible
     * to support multiple listeners in a single subject, typically using
     * inner classes.
     *
     * <p>For example, the {@link CompositeList} class uses multiple listeners
     * for a single subject, and uses this method to define that relationship.
     */
    void setRelatedSubject(Object listener, Object relatedSubject);

    /**
     * Detach the listener from its related subject.
     */
    void clearRelatedSubject(Object listener);

    /**
     * Attach the specified subject to the specified listener, so that the
     * listener's dependencies are satisfied before the subject is notified.
     * This makes it possible for a single listener to have multiple subjects,
     * typically using inner classes.
     *
     * <p>For example, the {@link ListSelection} class uses a single listener
     * for multiple subjects (selected and unselected), and uses this method
     * to define that relationship.
     */
    void setRelatedListener(Object subject, Object relatedListener);

    /**
     * Detach the subject from its related listener.
     */
    void clearRelatedListener(Object subject, Object relatedListener);
}
