/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEventPublisher;
import org.jspecify.annotations.NonNull;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

/**
 * An {@link EventList} that wraps any simple {@link List}, such as {@link ArrayList}
 * or {@link LinkedList}.
 *
 * <p><table border="1" width="100%" cellpadding="3" cellspacing="0">
 * <tr class="TableHeadingColor"><td colspan=2><font size="+2"><b>EventList Overview</b></font></td></tr>
 * <tr><td class="TableSubHeadingColor"><b>Writable:</b></td><td>yes</td></tr>
 * <tr><td class="TableSubHeadingColor"><b>Concurrency:</b></td><td>thread ready, not thread safe</td></tr>
 * <tr><td class="TableSubHeadingColor"><b>Performance:</b></td><td>reads: O(1), writes O(1) amortized</td></tr>
 * <tr><td class="TableSubHeadingColor"><b>Memory:</b></td><td>O(N)</td></tr>
 * <tr><td class="TableSubHeadingColor"><b>Unit Tests:</b></td><td>N/A</td></tr>
 * <tr><td class="TableSubHeadingColor"><b>Issues:</b></td><td>N/A</td></tr>
 * </table>
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public final class BasicEventList<E> extends AbstractEventList<E> implements RandomAccess {

    /** the underlying data list */
    private final List<E> data;

    /**
     * Creates a {@link BasicEventList}.
     */
    public BasicEventList() {
        this(new ReentrantReadWriteLock());
    }

    /**
     * Creates a {@link BasicEventList} that uses the specified {@link ReadWriteLock}
     * for concurrent access.
     */
    public BasicEventList(ReadWriteLock readWriteLock) {
        this(null, readWriteLock);
    }

    /**
     * Creates an empty {@link BasicEventList} with the given
     * <code>initialCapacity</code>.
     */
    public BasicEventList(int initalCapacity) {
        this(initalCapacity, null, new ReentrantReadWriteLock());
    }

    /**
     * Creates a {@link BasicEventList} using the specified
     * {@link ListEventPublisher} and {@link ReadWriteLock}.
     *
     * @since 2006-June-12
     */
    public BasicEventList(ListEventPublisher publisher, ReadWriteLock readWriteLock) {
        this(10, publisher, readWriteLock);
    }

    /**
     * Creates a {@link BasicEventList} using the specified initial capacity,
     * {@link ListEventPublisher} and {@link ReadWriteLock}.
     *
     * @since 2007-April-19
     */
    public BasicEventList(int initialCapacity, ListEventPublisher publisher, ReadWriteLock readWriteLock) {
        super(publisher);
        this.data = new ArrayList<>(initialCapacity);
        this.readWriteLock = (readWriteLock == null) ? new ReentrantReadWriteLock() : readWriteLock;
    }

    /**
     * Creates a {@link BasicEventList} that uses the specified {@link List} as
     * the underlying implementation.
     *
     * <p><strong><font color="#FF0000">Warning:</font></strong> all editing to
     * the specified {@link List} <strong>must</strong> be done through via this
     * {@link BasicEventList} interface. Otherwise this {@link BasicEventList} will
     * become out of sync and operations will fail.
     *
     * @deprecated As of 2005/03/06, this constructor has been declared unsafe
     *     because the source list is exposed. This allows it to be modified without
     *     the required events being fired. This constructor has been replaced by
     *     the factory method {@link GlazedLists#eventList(Collection)}.
     */
    @Deprecated
    public BasicEventList(List<E> list) {
        super(null);
        this.data = list;
        this.readWriteLock = new ReentrantReadWriteLock();
    }

    /** {@inheritDoc} */
    @Override
    public void add(int index, E element) {
        // create the change event
        updates.beginEvent();
        updates.elementInserted(index, element);
        // do the actual add
        data.add(index, element);
        // fire the event
        updates.commitEvent();
    }

    /** {@inheritDoc} */
    @Override
    public boolean add(E element) {
        // create the change event
        updates.beginEvent();
        updates.elementInserted(size(), element);
        // do the actual add
        boolean result = data.add(element);
        // fire the event
        updates.commitEvent();
        return result;
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(Collection<? extends E> collection) {
        return addAll(size(), collection);
    }

    /** {@inheritDoc} */
    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        // don't do an add of an empty set
        if (collection.isEmpty()) return false;

        // create the change event
        updates.beginEvent();
        for (E value : collection) {
            updates.elementInserted(index, value);
            data.add(index, value);
            index++;
        }
        // fire the event
        updates.commitEvent();
        return !collection.isEmpty();
    }

    /** {@inheritDoc} */
    @Override
    public E remove(int index) {
        // create the change event
        updates.beginEvent();
        // do the actual remove
        E removed = data.remove(index);
        // fire the event
        updates.elementDeleted(index, removed);
        updates.commitEvent();
        return removed;
    }

    /** {@inheritDoc} */
    @Override
    @SuppressWarnings("SuspiciousMethodCalls")
    public boolean remove(Object element) {
        int index = data.indexOf(element);
        if(index == -1) return false;
        remove(index);
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public void clear() {
        // don't do a clear on an empty set
        if(isEmpty()) return;
        // create the change event
        updates.beginEvent();
        data.forEach(e -> updates.elementDeleted(0, e));
        // do the actual clear
        data.clear();
        // fire the event
        updates.commitEvent();
    }

    /** {@inheritDoc} */
    @Override
    public E set(int index, E element) {
        // create the change event
        updates.beginEvent();
        // do the actual set
        E previous = data.set(index, element);
        // fire the event
        updates.elementUpdated(index, previous, element);
        updates.commitEvent();
        return previous;
    }

    /** {@inheritDoc} */
    @Override
    public E get(int index) {
        return data.get(index);
    }

    /** {@inheritDoc} */
    @Override
    public int size() {
        return data.size();
    }

    /** {@inheritDoc} */
    @Override
    public boolean removeIf(Predicate<? super E> filter) {
        if (isEmpty()) return false;

        boolean changed = false;
        updates.beginEvent();
        // Work backwards to prevent index changes
        for (int i = data.size() - 1; i >= 0; i--) {
            if (filter.test(data.get(i))) {
                E removed = data.remove(i);
                updates.elementDeleted(i, removed);
                changed = true;
            }
        }
        updates.commitEvent();
        return changed;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void replaceAll(UnaryOperator<E> operator) {
        updates.beginEvent();
        for (int i = size() - 1; i >= 0; i--) {
            E oldValue = data.get(i);
            E newValue = operator.apply(oldValue);
            if (oldValue != newValue) {               // instance check
                data.set(i, newValue);
                updates.elementUpdated(i, oldValue, newValue);
            }
        }
        updates.commitEvent();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forEach(Consumer<? super E> action) {
        data.forEach(action);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Stream<E> stream() {
        return data.stream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Stream<E> parallelStream() {
        return data.parallelStream();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull Spliterator<E> spliterator() {
        return data.spliterator();
    }

    /**
     * This method does nothing. It is not necessary to dispose a BasicEventList.
     */
    @Override
    public void dispose() { }
}
