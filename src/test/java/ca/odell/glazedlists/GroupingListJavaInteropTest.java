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

import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GroupingListJavaInteropTest {
    @Test
    void preservesFactoryComparatorVarianceAndMappedListMethods() throws Exception {
        final BasicEventList<String> source = new BasicEventList<>();
        source.addAll(Arrays.asList("bbb", "a", "bbb", "cc"));

        final GroupingList<String> natural = GroupingList.Companion.create(source);
        assertEquals(List.of(List.of("a"), List.of("bbb", "bbb"), List.of("cc")), natural);

        final Comparator<Object> byLength = Comparator.comparingInt(value -> value.toString().length());
        final GroupingList<String> custom = new GroupingList<>(source, byLength);
        assertEquals(List.of(List.of("a"), List.of("cc"), List.of("bbb", "bbb")), custom);
        assertEquals(1, custom.indexOfGroup("zz"));

        custom.add(0, List.of("d", "ee"));
        final int firstGroupIndex = custom.indexOfGroup("a");
        assertEquals(List.of("a", "d"), custom.get(firstGroupIndex));
        final List<String> removed = custom.remove(firstGroupIndex);
        assertEquals(List.of("a", "d"), removed);

        custom.setComparator(null);
        assertFalse(Modifier.isStatic(
                GroupingList.Companion.getClass().getDeclaredMethod("create", EventList.class).getModifiers()));
        assertEquals(List.class, GroupingList.class.getMethod("get", int.class).getReturnType());
        assertEquals(List.class, GroupingList.class.getMethod("remove", int.class).getReturnType());
        assertEquals(List.class, GroupingList.class.getDeclaredMethod("set", int.class, List.class).getReturnType());
    }

    @Test
    void keepsImplementationClassesPrivateAndNonStatic() throws Exception {
        for (String simpleName : List.of("GrouperClient", "GroupList")) {
            final Class<?> implementation = Class.forName(GroupingList.class.getName() + "$" + simpleName);
            assertTrue(Modifier.isPrivate(implementation.getModifiers()));
            assertFalse(Modifier.isStatic(implementation.getModifiers()));
            assertFalse(Modifier.isFinal(implementation.getModifiers()));
            assertEquals(GroupingList.class, implementation.getEnclosingClass());
        }
    }

}
