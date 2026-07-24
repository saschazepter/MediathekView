package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEventAssembler;
import ca.odell.glazedlists.event.ListEventListener;
import ca.odell.glazedlists.event.ListEventPublisher;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.*;

class EventListJavaInteropTest {
    @Test
    void interfaceShapeAndGenericListenerContractsRemainStable() throws Exception {
        assertTrue(EventList.class.isInterface());
        assertTrue(Modifier.isPublic(EventList.class.getModifiers()));
        assertEquals("java.util.List<E>", EventList.class.getGenericInterfaces()[0].getTypeName());
        assertEquals(AutoCloseable.class, EventList.class.getGenericInterfaces()[1]);

        final Method addListener = EventList.class.getMethod("addListEventListener", ListEventListener.class);
        final Method removeListener = EventList.class.getMethod("removeListEventListener", ListEventListener.class);
        final Method readWriteLock = EventList.class.getMethod("getReadWriteLock");
        final Method publisher = EventList.class.getMethod("getPublisher");
        final Method dispose = EventList.class.getMethod("dispose");
        final Method close = EventList.class.getMethod("close");

        assertEquals("ca.odell.glazedlists.event.ListEventListener<? super E>",
                addListener.getGenericParameterTypes()[0].getTypeName());
        assertEquals("ca.odell.glazedlists.event.ListEventListener<? super E>",
                removeListener.getGenericParameterTypes()[0].getTypeName());
        assertEquals(ReadWriteLock.class, readWriteLock.getReturnType());
        assertEquals(ListEventPublisher.class, publisher.getReturnType());
        assertTrue(Modifier.isAbstract(dispose.getModifiers()));
        assertTrue(close.isDefault());
        assertEquals(0, close.getExceptionTypes().length);
    }

    @Test
    void javaImplementationsInheritCloseAsDispose() {
        final JavaEventList<String> list = new JavaEventList<>();
        list.add("value");

        list.close();

        assertEquals(List.of("value"), list);
        assertTrue(list.disposed);
    }

    private static final class JavaEventList<E> extends AbstractList<E> implements EventList<E> {
        private final List<E> values = new ArrayList<>();
        private final ReadWriteLock lock = new ReentrantReadWriteLock();
        private final ListEventPublisher publisher = ListEventAssembler.Companion.createListEventPublisher();
        private boolean disposed;

        @Override
        public E get(int index) {
            return values.get(index);
        }

        @Override
        public int size() {
            return values.size();
        }

        @Override
        public void add(int index, E element) {
            values.add(index, element);
        }

        @Override
        public E remove(int index) {
            return values.remove(index);
        }

        @Override
        public E set(int index, E element) {
            return values.set(index, element);
        }

        @Override
        public void addListEventListener(@NonNull ListEventListener<? super E> listChangeListener) {
        }

        @Override
        public void removeListEventListener(@NonNull ListEventListener<? super E> listChangeListener) {
        }

        @Override
        public @NonNull ReadWriteLock getReadWriteLock() {
            return lock;
        }

        @Override
        public @NonNull ListEventPublisher getPublisher() {
            return publisher;
        }

        @Override
        public void dispose() {
            disposed = true;
        }
    }
}
