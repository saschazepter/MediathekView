package ca.odell.glazedlists.impl;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.event.ListEventListener;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.ListIterator;

import static org.junit.jupiter.api.Assertions.*;

class EventListIteratorAndSubListJavaInteropTest {
    @Test
    void constructorsAndInterfacesRemainJavaVisible() {
        try (final BasicEventList<String> source = new BasicEventList<>()) {
            source.add("value");

            assertInstanceOf(ListIterator.class, new EventListIterator<>(source));
            assertInstanceOf(ListEventListener.class, new EventListIterator<>(source, 0));
            assertInstanceOf(ListIterator.class, new EventListIterator<>(source, 0, false));

            final EventListIteratorSubclass<String> subclass = new EventListIteratorSubclass<>(source);
            assertEquals("value", subclass.next());

            final SubEventList<String> subList = new SubEventList<>(source, 0, 1, false);
            assertEquals("value", subList.getFirst());
            subList.dispose();
        }
    }

    @Test
    void classExtensibilityRemainsUnchanged() {
        assertFalse(Modifier.isFinal(EventListIterator.class.getModifiers()));
        assertTrue(Modifier.isFinal(SubEventList.class.getModifiers()));
    }

    private static final class EventListIteratorSubclass<E> extends EventListIterator<E> {
        private EventListIteratorSubclass(BasicEventList<E> source) {
            super(source, 0, false);
        }
    }
}
