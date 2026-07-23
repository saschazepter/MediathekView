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

import ca.odell.glazedlists.matchers.Matcher;
import ca.odell.glazedlists.matchers.MatcherEditor;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

class FilterListJavaInteropTest {
    @Test
    void preservesConstructorsMatcherVarianceNullableSettersAndMappedListMethods() throws Exception {
        final BasicEventList<String> source = new BasicEventList<>();
        source.addAll(Arrays.asList("a", "bbb", "cc"));

        final FilterList<String> all = new FilterList<>(source);
        assertEquals(source, all);

        final Matcher<Object> longText = value -> value.toString().length() > 1;
        final FilterList<String> matched = new FilterList<>(source, longText);
        assertEquals(List.of("bbb", "cc"), matched);

        matched.setMatcher(null);
        assertEquals(source, matched);
        matched.setMatcher(longText);
        assertEquals(List.of("bbb", "cc"), matched);

        final MatcherEditor<String> editor = MatcherEditor.fromMatcher(value -> value.startsWith("b"));
        final FilterList<String> edited = new FilterList<>(source, editor);
        assertEquals(List.of("bbb"), edited);
        edited.setMatcherEditor(null);
        assertEquals(source, edited);

        final FilterList<String> nullMatcher = new FilterList<>(source, (Matcher<String>) null);
        final FilterList<String> nullEditor = new FilterList<>(source, (MatcherEditor<String>) null);
        assertEquals(source, nullMatcher);
        assertEquals(source, nullEditor);

        matched.add(1, "dddd");
        assertEquals("dddd", matched.remove(1));

        assertEquals(int.class, FilterList.class.getMethod("size").getReturnType());
        assertEquals(Object.class, FilterList.class.getMethod("remove", int.class).getReturnType());
    }

    @Test
    void keepsMatcherEditorListenerPrivateAndNonStatic() throws Exception {
        final Class<?> implementation = Class.forName(FilterList.class.getName() + "$PrivateMatcherEditorListener");

        assertTrue(Modifier.isPrivate(implementation.getModifiers()));
        assertFalse(Modifier.isStatic(implementation.getModifiers()));
        assertFalse(Modifier.isFinal(implementation.getModifiers()));
        assertEquals(FilterList.class, implementation.getEnclosingClass());
    }
}
