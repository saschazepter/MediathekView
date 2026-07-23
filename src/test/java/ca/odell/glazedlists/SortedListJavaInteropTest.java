/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.odell.glazedlists;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

class SortedListJavaInteropTest {

    @Test
    void preservesFactoriesConstructorsConstantsAndComparatorWildcards() throws ReflectiveOperationException {
        final BasicEventList<String> source = new BasicEventList<>();
        source.addAll(Arrays.asList("bbb", "a", "cc"));

        final SortedList<String> natural = SortedList.create(source);
        assertEquals(List.of("a", "bbb", "cc"), natural);

        final Comparator<Object> byTextLength = Comparator.comparingInt(value -> value.toString().length());
        final SortedList<String> custom = new SortedList<>(source, byTextLength);
        assertEquals(List.of("a", "cc", "bbb"), custom);
        assertSame(byTextLength, custom.getComparator());

        custom.setComparator(null);
        assertNull(custom.getComparator());
        assertEquals(source, custom);

        assertEquals(0, SortedList.STRICT_SORT_ORDER);
        assertEquals(1, SortedList.AVOID_MOVING_ELEMENTS);
        assertTrue(Modifier.isFinal(SortedList.class.getModifiers()));

        final Method create = SortedList.class.getDeclaredMethod("create", EventList.class);
        assertTrue(Modifier.isPublic(create.getModifiers()));
        assertTrue(Modifier.isStatic(create.getModifiers()));
    }

    @Test
    void preservesWritableIteratorAndModeSurface() {
        final BasicEventList<Integer> source = new BasicEventList<>();
        source.addAll(Arrays.asList(3, 1, 2));
        final SortedList<Integer> sorted = new SortedList<>(source, Comparator.naturalOrder());

        final Iterator<Integer> iterator = sorted.iterator();
        assertEquals(1, iterator.next());
        iterator.remove();

        assertEquals(List.of(3, 2), source);
        assertEquals(List.of(2, 3), sorted);

        sorted.setMode(SortedList.AVOID_MOVING_ELEMENTS);
        assertEquals(SortedList.AVOID_MOVING_ELEMENTS, sorted.getMode());
        assertThrows(IllegalArgumentException.class, () -> sorted.setMode(-1));
    }

    @Test
    void keepsImplementationClassesPrivateAndNonStatic() throws ClassNotFoundException {
        for (String simpleName : List.of("ElementComparator", "ElementRawOrderComparator", "SortedListIterator")) {
            final Class<?> implementation = Class.forName(SortedList.class.getName() + "$" + simpleName);
            assertTrue(Modifier.isPrivate(implementation.getModifiers()));
            assertFalse(Modifier.isStatic(implementation.getModifiers()));
            assertEquals(SortedList.class, implementation.getEnclosingClass());
        }
    }

    @Test
    void jspecifyAnnotationsRemainRuntimeVisibleOnTheJavaApi() throws Exception {
        final var comparatorConstructor =
                SortedList.class.getConstructor(EventList.class, Comparator.class);
        final var getComparator = SortedList.class.getDeclaredMethod("getComparator");
        final var setComparator = SortedList.class.getDeclaredMethod("setComparator", Comparator.class);
        final var iterator = SortedList.class.getDeclaredMethod("iterator");

        assertTrue(comparatorConstructor.getAnnotatedParameterTypes()[1]
                .isAnnotationPresent(Nullable.class));
        assertTrue(getComparator.getAnnotatedReturnType().isAnnotationPresent(Nullable.class));
        assertTrue(setComparator.getAnnotatedParameterTypes()[0]
                .isAnnotationPresent(Nullable.class));
        assertTrue(iterator.getAnnotatedReturnType().isAnnotationPresent(NonNull.class));
    }
}
