package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.RandomAccess;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SequenceListJavaInteropTest {
    @Test
    void preservesNestedSequencerAndBothPublicConstructors() throws Exception {
        final Method previous = SequenceList.Sequencer.class.getDeclaredMethod("previous", Object.class);
        final Method next = SequenceList.Sequencer.class.getDeclaredMethod("next", Object.class);
        assertEquals(Object.class, previous.getReturnType());
        assertEquals(Object.class, next.getReturnType());
        assertSame(SequenceList.class, SequenceList.Sequencer.class.getEnclosingClass());
        assertTrue(Modifier.isPublic(SequenceList.Sequencer.class.getModifiers()));
        assertTrue(Modifier.isInterface(SequenceList.Sequencer.class.getModifiers()));

        final Constructor<?> natural = SequenceList.class.getConstructor(EventList.class, SequenceList.Sequencer.class);
        final Constructor<?> ordered = SequenceList.class.getConstructor(EventList.class, SequenceList.Sequencer.class, Comparator.class);
        assertTrue(Modifier.isPublic(natural.getModifiers()));
        assertTrue(Modifier.isPublic(ordered.getModifiers()));
        assertEquals(2, Arrays.stream(SequenceList.class.getDeclaredConstructors()).filter(c -> Modifier.isPublic(c.getModifiers())).count());
    }

    @Test
    void remainsFinalRandomAccessAndReadOnlyForJavaCallers() {
        final BasicEventList<Integer> source = new BasicEventList<>();
        source.addAll(Arrays.asList(5, 25));

        try (SequenceList<Integer> sequence = new SequenceList<>(source, tensSequencer())) {
            assertEquals(Arrays.asList(0, 10, 20, 30), sequence);
            assertEquals(10, sequence.getPreviousSequenceValue(15));
            assertEquals(20, sequence.getNextSequenceValue(15));
            assertInstanceOf(RandomAccess.class, sequence);
            assertTrue(Modifier.isFinal(SequenceList.class.getModifiers()));

            final IllegalStateException failure = assertThrows(IllegalStateException.class, () -> sequence.add(40));
            assertEquals("Non-writable List cannot be modified", failure.getMessage());
        }
    }

    @Test
    void keepsMappedCollectionJvmNames() throws Exception {
        assertEquals(int.class, SequenceList.class.getDeclaredMethod("size").getReturnType());
        assertEquals(Object.class, SequenceList.class.getDeclaredMethod("get", int.class).getReturnType());
        assertFalse(hasDeclaredMethod("getSize"));
        assertFalse(hasDeclaredMethod("removeAt", int.class));
    }

    private static SequenceList.Sequencer<Integer> tensSequencer() {
        return new SequenceList.Sequencer<>() {
            @Override
            public Integer previous(Integer value) {
                return Math.floorDiv(value - 1, 10) * 10;
            }

            @Override
            public Integer next(Integer value) {
                return (Math.floorDiv(value, 10) + 1) * 10;
            }
        };
    }

    private static boolean hasDeclaredMethod(String name, Class<?>... parameterTypes) {
        try {
            SequenceList.class.getDeclaredMethod(name, parameterTypes);
            return true;
        }
        catch (NoSuchMethodException ignored) {
            return false;
        }
    }
}
