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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.odell.glazedlists.event;

import ca.odell.glazedlists.BasicEventList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

class Tree4DeltasJavaInteropTest {

    @Test
    void eventImplementationRemainsPackagePrivate() {
        assertFalse(Modifier.isPublic(Tree4DeltasListEvent.class.getModifiers()));
    }

    @Test
    void linearEventsRetainNullValues() {
        BasicEventList<Object> source = new BasicEventList<>();
        ListEventAssembler<Object> assembler = new ListEventAssembler<>(source, source.getPublisher());
        AtomicInteger changes = new AtomicInteger();
        assembler.addListEventListener(event -> {
            while (event.next()) {
                assertNull(event.getOldValue());
                assertNull(event.getNewValue());
                changes.incrementAndGet();
            }
        });

        assembler.beginEvent();
        assembler.elementUpdated(0, null, null);
        assembler.commitEvent();

        assertEquals(1, changes.get());
    }

    @Test
    void treeEventsRetainNullValues() {
        BasicEventList<Object> source = new BasicEventList<>();
        source.add(new Object());
        source.add(new Object());
        ListEventAssembler<Object> assembler = new ListEventAssembler<>(source, source.getPublisher());
        AtomicInteger changes = new AtomicInteger();
        assembler.addListEventListener(event -> {
            while (event.next()) {
                assertNull(event.getOldValue());
                assertNull(event.getNewValue());
                changes.incrementAndGet();
            }
        });

        assembler.beginEvent();
        assembler.elementUpdated(1, null, null);
        assembler.elementUpdated(0, null, null);
        assembler.commitEvent();

        assertEquals(2, changes.get());
    }
}
