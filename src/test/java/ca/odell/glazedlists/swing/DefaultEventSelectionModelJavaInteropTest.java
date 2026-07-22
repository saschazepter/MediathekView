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
package ca.odell.glazedlists.swing;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.matchers.Matcher;
import org.junit.jupiter.api.Test;

import javax.swing.ListSelectionModel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultEventSelectionModelJavaInteropTest {

    @Test
    void constructorsAndJavaSelectionInterfacesRemainUsable() {
        final BasicEventList<String> source = new BasicEventList<>();
        source.add("first");
        source.add("second");

        final DefaultEventSelectionModel<String> concrete = new DefaultEventSelectionModel<>(source, false);
        assertAdvancedSelectionModel(concrete);
        selectSecondRow(concrete, concrete);
        concrete.dispose();

        final DefaultEventSelectionModel<String> oneArgument = new DefaultEventSelectionModel<>(source);
        final Matcher<String> matcher = value -> value.startsWith("f");
        oneArgument.addValidSelectionMatcher(matcher);
        assertNotNull(oneArgument.getSelected());
        assertNotNull(oneArgument.getTogglingSelected());
        assertNotNull(oneArgument.getDeselected());
        assertNotNull(oneArgument.getTogglingDeselected());
        oneArgument.removeValidSelectionMatcher(matcher);
        oneArgument.dispose();
    }

    private static void assertAdvancedSelectionModel(AdvancedListSelectionModel<String> advanced) {
        assertTrue(advanced.getEnabled());
        advanced.setEnabled(false);
        assertFalse(advanced.getEnabled());
        advanced.setEnabled(true);
    }

    private static void selectSecondRow(ListSelectionModel swing, AdvancedListSelectionModel<String> advanced) {
        swing.setSelectionInterval(1, 1);

        assertEquals(1, advanced.getSelected().size());
        assertEquals("second", advanced.getSelected().getFirst());
    }
}
