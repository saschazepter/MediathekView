package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class DisposableMapJavaInteropTest {
    @Test
    void javaMapImplementationRetainsMapAndDisposalContracts() {
        TrackingDisposableMap<String, Integer> disposableMap = new TrackingDisposableMap<>();
        verifyMapOperations(disposableMap);
        assertFalse(disposableMap.disposed);

        disposableMap.dispose();
        assertTrue(disposableMap.disposed);
    }

    private static void verifyMapOperations(Map<String, Integer> map) {
        assertNull(map.put("one", 1));
        map.putAll(Map.of("two", 2, "three", 3));
        assertEquals(2, map.remove("two"));
        assertEquals(Map.of("one", 1, "three", 3), map);
    }

    private static final class TrackingDisposableMap<K, V> extends HashMap<K, V> implements DisposableMap<K, V> {
        private boolean disposed;

        @Override
        public void dispose() {
            disposed = true;
        }
    }
}
