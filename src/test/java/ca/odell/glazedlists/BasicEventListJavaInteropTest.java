package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEventPublisher;
import ca.odell.glazedlists.impl.UpgradeDetectingReadWriteLock;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.RandomAccess;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import static org.junit.jupiter.api.Assertions.*;

class BasicEventListJavaInteropTest {
    @Test
    @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
    void preservesAllConstructorsAndInfrastructureSelection() {
        final BasicEventList<String> defaults = new BasicEventList<>();
        final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
        final ListEventPublisher publisher = defaults.getPublisher();
        try (defaults;
             BasicEventList<String> locked = new BasicEventList<>(lock);
             BasicEventList<String> capacity = new BasicEventList<>(4);
             BasicEventList<String> published = new BasicEventList<>(publisher, lock);
             BasicEventList<String> configured = new BasicEventList<>(4, publisher, lock);
             BasicEventList<String> fallback = new BasicEventList<>(4, publisher, null)) {
            assertSame(lock, locked.getReadWriteLock());
            assertTrue(capacity.isEmpty());
            assertSame(publisher, published.getPublisher());
            assertSame(publisher, configured.getPublisher());
            assertSame(lock, configured.getReadWriteLock());
            assertInstanceOf(UpgradeDetectingReadWriteLock.class, fallback.getReadWriteLock());
        }
    }

    @Test
    void preservesJavaListOverloadsAndMarkers() throws Exception {
        try (BasicEventList<String> source = new BasicEventList<>()) {
            source.add("A");
            //noinspection SequencedCollectionMethodCanBeUsed -- exercise add(int, E)
            source.add(0, "prefix");

            //noinspection SequencedCollectionMethodCanBeUsed -- exercise remove(int)
            assertEquals("prefix", source.remove(0));
            assertTrue(source.remove("A"));
            assertFalse(source.remove("missing"));
            assertInstanceOf(RandomAccess.class, source);
        }
        assertTrue(Modifier.isFinal(BasicEventList.class.getModifiers()));

        assertEquals(Object.class, BasicEventList.class.getDeclaredMethod("remove", int.class).getReturnType());
        assertEquals(boolean.class, BasicEventList.class.getDeclaredMethod("remove", Object.class).getReturnType());
        assertEquals(int.class, BasicEventList.class.getDeclaredMethod("size").getReturnType());
        assertFalse(hasDeclaredMethod("removeAt", int.class));
        assertFalse(hasDeclaredMethod("getSize"));
    }

    private static boolean hasDeclaredMethod(String name, Class<?>... parameterTypes) {
        try {
            BasicEventList.class.getDeclaredMethod(name, parameterTypes);
            return true;
        }
        catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
