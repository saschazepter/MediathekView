package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEvent;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SyncListenerJavaInteropTest {
    @Test
    void javaCanSubclassListenerAndOverrideLifecycleMethods() {
        try (BasicEventList<String> source = new BasicEventList<>()) {
            source.add("initial");
            List<String> target = new ArrayList<>(List.of("stale"));
            RecordingSyncListener<String> listener = new RecordingSyncListener<>(source, target);

            assertEquals(List.of("initial"), target);
            source.add("added");
            assertEquals(List.of("initial", "added"), target);
            assertEquals(1, listener.changeCount);

            listener.dispose();
            assertTrue(listener.disposed);
        }
    }

    @Test
    void javaFactoryReturnsTheConcreteListener() {
        try (BasicEventList<String> source = new BasicEventList<>()) {
            List<String> target = new ArrayList<>();

            SyncListener<String> listener = GlazedLists.INSTANCE.syncEventListToList(source, target);
            source.add("value");

            assertEquals(List.of("value"), target);
            listener.dispose();
        }
    }

    private static final class RecordingSyncListener<E> extends SyncListener<E> {
        private int changeCount;
        private boolean disposed;

        private RecordingSyncListener(EventList<E> source, List<E> target) {
            super(source, target);
        }

        @Override
        public void listChanged(@NonNull ListEvent<E> listChanges) {
            changeCount++;
            super.listChanged(listChanges);
        }

        @Override
        public void dispose() {
            disposed = true;
            super.dispose();
        }
    }
}
