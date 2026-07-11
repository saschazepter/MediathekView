/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl;

import ca.odell.glazedlists.ObservableElementChangeHandler;
import ca.odell.glazedlists.ObservableElementList;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.EventListener;

/**
 * An {@link ObservableElementList.Connector} for elements that publish
 * standard JavaBeans property-change events.
 *
 * @author James Lemieux
 */
public class ObservableConnector<E extends ObservableConnector.PropertyChangeObservable> implements ObservableElementList.Connector<E>, PropertyChangeListener, EventListener {

    /**
     * Contract for elements that support standard property-change listeners.
     */
    public interface PropertyChangeObservable {
        void addPropertyChangeListener(PropertyChangeListener listener);
        void removePropertyChangeListener(PropertyChangeListener listener);
    }

    /** The list which contains the elements being observed via this {@link ObservableElementList.Connector}. */
    private ObservableElementChangeHandler<? extends E> list;

    /**
     * This method is called whenever an observed property is changed. It
     * responds by notifying the associated ObservableElementList that the given
     * element has been changed.
     *
     * @param event the property-change event
     */
    @Override
    public void propertyChange(PropertyChangeEvent event) {
        update(event);
    }

    /**
     * Updates the associated list for a property-change event.
     */
    @SuppressWarnings("unchecked")
    public void update(PropertyChangeEvent event) {
        list.elementChanged((E) event.getSource());
    }

    /**
     * Start observing the specified <code>element</code>.
     *
     * @param element the element to be observed
     * @return the listener that was installed on the <code>element</code>
     *      to be used as a parameter to {@link #uninstallListener(Object, EventListener)}
     */
    @Override
    public EventListener installListener(E element) {
        element.addPropertyChangeListener(this);
        return this;
    }

    /**
     * Stop observing the specified <code>element</code>.
     *
     * @param element the observed element
     * @param listener the listener that was installed on the <code>element</code>
     *      in {@link #installListener(Object)}
     */
    @Override
    public void uninstallListener(E element, EventListener listener) {
        element.removePropertyChangeListener(this);
    }

    /** {@inheritDoc} */
    @Override
    public void setObservableElementList(ObservableElementChangeHandler<? extends E> list) {
        this.list = list;
    }
}
