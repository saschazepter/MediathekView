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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Test;

class UniqueListJavaInteropTest {
    @Test
    void preservesFactoryConstructorsComparatorVarianceAndOverloads() throws Exception {
        final BasicEventList<String> source = new BasicEventList<>();
        source.addAll(Arrays.asList("bbb", "a", "bbb", "cc"));

        final UniqueList<String> natural = UniqueList.Companion.create(source);
        assertEquals(List.of("a", "bbb", "cc"), natural);

        final Comparator<Object> byLength = Comparator.comparingInt(value -> value.toString().length());
        final UniqueList<String> custom = new UniqueList<>(source, byLength);
        final UniqueList<String> deferredNullComparator = new UniqueList<>(new BasicEventList<>(), null);
        assertEquals(List.of("a", "cc", "bbb"), custom);
        assertEquals(1, custom.getCount("zz"));
        assertEquals(List.of("bbb", "bbb"), custom.getAll(2));
        assertEquals(List.of("cc"), custom.getAll("zz"));
        assertEquals(UniqueList.class, deferredNullComparator.getClass());

        custom.setComparator(null);
        assertEquals(List.of("a", "bbb", "cc"), custom);

        final Method create = UniqueList.Companion.getClass().getDeclaredMethod("create", EventList.class);
        assertTrue(Modifier.isPublic(create.getModifiers()));
        assertFalse(Modifier.isStatic(create.getModifiers()));
        assertEquals(int.class, UniqueList.class.getMethod("getCount", int.class).getReturnType());
        assertEquals(int.class, UniqueList.class.getMethod("getCount", Object.class).getReturnType());
    }

    @Test
    void keepsGrouperClientPrivateAndNonStatic() throws Exception {
        final Class<?> implementation = Class.forName(UniqueList.class.getName() + "$GrouperClient");

        assertTrue(Modifier.isPrivate(implementation.getModifiers()));
        assertFalse(Modifier.isStatic(implementation.getModifiers()));
        assertFalse(Modifier.isFinal(implementation.getModifiers()));
        assertEquals(UniqueList.class, implementation.getEnclosingClass());
    }
}
