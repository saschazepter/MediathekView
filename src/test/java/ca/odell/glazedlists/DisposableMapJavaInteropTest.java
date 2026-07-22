package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisposableMapJavaInteropTest {
    @Test
    void javaMapImplementationRetainsMapAndDisposalContracts() {
        TrackingDisposableMap<String, Integer> disposableMap = new TrackingDisposableMap<>();
        verifyMapOperations(disposableMap);
        assertFalse(disposableMap.disposed);

        disposableMap.dispose();
        assertTrue(disposableMap.disposed);
    }

    @Test
    void glazedListsFactoriesRetainDisposableMapReturnTypes() {
        BasicEventList<String> source = new BasicEventList<>();
        source.addAll(List.of("alpha", "beta", "apricot"));

        DisposableMap<Integer, String> map = GlazedLists.syncEventListToMap(source, String::length);
        assertEquals(Map.of(5, "alpha", 4, "beta", 7, "apricot"), map);

        DisposableMap<Character, List<String>> multiMap =
                GlazedLists.syncEventListToMultiMap(source, value -> value.charAt(0));
        assertEquals(List.of("alpha", "apricot"), multiMap.get('a'));
        assertEquals(List.of("beta"), multiMap.get('b'));

        map.dispose();
        multiMap.dispose();
        source.dispose();
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
