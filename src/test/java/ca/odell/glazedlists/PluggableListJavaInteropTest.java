package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class PluggableListJavaInteropTest {
    @Test
    void constructorsAndOverridableMethodsRemainJavaVisible() throws Exception {
        final EventList<String> source = new BasicEventList<>();
        final PluggableList<String> fromSource = new PluggableList<>(source);
        final PluggableList<String> fromInfrastructure =
                new PluggableList<>(source.getPublisher(), source.getReadWriteLock());
        final PluggableList<String> subclass = new JavaSubclass<>(source);

        try (fromSource; fromInfrastructure; subclass) {
            assertEquals(0, fromSource.size());
            fromInfrastructure.add("value");
            assertEquals(1, fromInfrastructure.size());
            assertEquals(0, subclass.size());
            assertFalse(Modifier.isFinal(PluggableList.class.getModifiers()));
        }

        assertOverridable("createSourceList");
        assertOverridable("setSource", EventList.class);
        assertOverridable("listChanged", ListEvent.class);
        assertOverridable("dispose");

        final Method writable = PluggableList.class.getDeclaredMethod("isWritable");
        assertTrue(Modifier.isProtected(writable.getModifiers()));
        assertFalse(Modifier.isFinal(writable.getModifiers()));
    }

    @Test
    void nullAndDisposedSourceDiagnosticsRemainAvailableToJava() {
        final PluggableList<String> list = new PluggableList<>(new BasicEventList<>());

        final IllegalArgumentException nullFailure =
                assertThrows(IllegalArgumentException.class, () -> list.setSource(null));
        assertEquals("source may not be null", nullFailure.getMessage());

        final EventList<String> replacement = list.createSourceList();
        list.dispose();
        final IllegalStateException disposedFailure =
                assertThrows(IllegalStateException.class, () -> list.setSource(replacement));
        assertEquals("setSource may not be called on a disposed PluggableList", disposedFailure.getMessage());
    }

    private static void assertOverridable(String name, Class<?>... parameterTypes) throws Exception {
        final Method method = PluggableList.class.getDeclaredMethod(name, parameterTypes);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertFalse(Modifier.isFinal(method.getModifiers()));
    }

    private static final class JavaSubclass<E> extends PluggableList<E> {
        private JavaSubclass(EventList<E> source) {
            super(source);
        }


        @Override
        public int size() {
            return super.size();
        }

        @Override
        public E remove(int index) {
            return super.remove(index);
        }

        @Override
        public void setSource(EventList<E> source) {
            super.setSource(source);
        }

        @Override
        protected boolean isWritable() {
            return super.isWritable();
        }


        @Override
        public void dispose() {
            super.dispose();
        }
    }
}
