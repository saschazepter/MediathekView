package ca.odell.glazedlists.impl;

import ca.odell.glazedlists.BasicEventList;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

class ReadOnlyListJavaInteropTest {
    @Test
    void constructorRemainsJavaVisible() {
        try (final BasicEventList<String> source = new BasicEventList<>()) {
            source.add("a");

            assertTrue(Modifier.isFinal(ReadOnlyList.class.getModifiers()));
            assertInstanceOf(ReadOnlyList.class, new ReadOnlyList<>(source));
        }
    }

    @Test
    void allMutationPathsRetainTheExactFailure() {
        try (final BasicEventList<String> source = new BasicEventList<>()) {
            source.addAll(List.of("a", "b"));
            final ReadOnlyList<String> readOnly = new ReadOnlyList<>(source);

            assertReadOnly(() -> readOnly.add("x"));
            assertReadOnly(() -> readOnly.add(1, "x"));
            assertReadOnly(() -> readOnly.addAll(List.of("x", "y")));
            assertReadOnly(() -> readOnly.addAll(0, List.of("x")));
            assertReadOnly(readOnly::clear);
            assertReadOnly(() -> readOnly.remove("a"));
            assertReadOnly(() -> readOnly.remove(1));
            assertReadOnly(() -> readOnly.removeAll(List.of("a")));
            assertReadOnly(() -> readOnly.retainAll(List.of("a")));
            assertReadOnly(() -> readOnly.set(0, "x"));
            assertReadOnly(() -> readOnly.replaceAll(String::trim));
            assertReadOnly(() -> readOnly.removeIf(String::isEmpty));
            assertReadOnly(() -> readOnly.sort(null));

            final var iterator = readOnly.listIterator();
            iterator.next();
            assertReadOnly(iterator::remove);
            assertReadOnly(() -> iterator.set("x"));
            assertReadOnly(() -> iterator.add("x"));
        }
    }

    @Test
    @SuppressWarnings("SuspiciousToArrayCall")
    void typedArrayOverloadKeepsJavaArraySemantics() {
        try (final BasicEventList<String> source = new BasicEventList<>()) {
            source.addAll(List.of("a", "b"));
            final ReadOnlyList<String> readOnly = new ReadOnlyList<>(source);

            final String[] oversized = {"old", "old", "tail", "untouched"};
            assertSame(oversized, readOnly.toArray(oversized));
            assertArrayEquals(new String[]{"a", "b", null, "untouched"}, oversized);
            assertThrows(
                    ArrayStoreException.class,
                    () -> readOnly.toArray(new Integer[0]));
        }
    }

    @Test
    void jspecifyTypeUseAnnotationsRemainVisible() throws ReflectiveOperationException {
        final Method untypedArray = ReadOnlyList.class.getMethod("toArray");
        final Method typedArray = ReadOnlyList.class.getMethod("toArray", Object[].class);

        assertTrue(untypedArray.getAnnotatedReturnType().isAnnotationPresent(NonNull.class));
        assertTrue(typedArray.getAnnotatedReturnType().isAnnotationPresent(NonNull.class));
        assertTrue(typedArray.getAnnotatedParameterTypes()[0].isAnnotationPresent(NonNull.class));

        assertParameterNonNull("addAll", new Class<?>[]{Collection.class}, 0);
        assertParameterNonNull("addAll", new Class<?>[]{int.class, Collection.class}, 1);
        assertParameterNonNull("removeAll", new Class<?>[]{Collection.class}, 0);
        assertParameterNonNull("retainAll", new Class<?>[]{Collection.class}, 0);
        assertParameterNonNull("replaceAll", new Class<?>[]{UnaryOperator.class}, 0);
        assertParameterNonNull("removeIf", new Class<?>[]{Predicate.class}, 0);

        final Method sort = ReadOnlyList.class.getMethod("sort", java.util.Comparator.class);
        assertFalse(sort.getAnnotatedParameterTypes()[0].isAnnotationPresent(NonNull.class));
    }

    private static void assertReadOnly(Executable executable) {
        final UnsupportedOperationException failure =
                assertThrows(UnsupportedOperationException.class, executable);
        org.junit.jupiter.api.Assertions.assertEquals("ReadOnlyList cannot be modified", failure.getMessage());
    }

    private static void assertParameterNonNull(String name, Class<?>[] parameterTypes, int index)
            throws ReflectiveOperationException {
        final Method method = ReadOnlyList.class.getMethod(name, parameterTypes);
        assertTrue(method.getAnnotatedParameterTypes()[index].isAnnotationPresent(NonNull.class));
    }

}
