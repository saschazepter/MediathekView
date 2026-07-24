package ca.odell.glazedlists.swing;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.impl.SimpleIterator;
import ca.odell.glazedlists.impl.TypeSafetyListener;
import ca.odell.glazedlists.impl.WeakReferenceProxy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RemainingLeavesJavaInteropTest {
    @Test
    void publicJavaConstructionAndSubclassingRemainAvailable() {
        assertInstanceOf(SimpleIterator.class, new SimpleIteratorSubclass<>(List.of("value")));
        try (final BasicEventList<String> source = new BasicEventList<>()) {
            assertInstanceOf(TypeSafetyListener.class, new TypeSafetyListenerSubclass<>(source, Set.of(String.class)));
            final var target = (ca.odell.glazedlists.event.ListEventListener<String>) Assertions::assertNotNull;
            assertInstanceOf(WeakReferenceProxy.class, new WeakReferenceProxy<>(source, target));
        }
    }

    private static final class SimpleIteratorSubclass<E> extends SimpleIterator<E> {
        private SimpleIteratorSubclass(List<E> source) {
            super(source);
        }
    }

    private static final class TypeSafetyListenerSubclass<E> extends TypeSafetyListener<E> {
        private TypeSafetyListenerSubclass(BasicEventList<E> source, Set<Class<?>> types) {
            super(source, types);
        }
    }
}
