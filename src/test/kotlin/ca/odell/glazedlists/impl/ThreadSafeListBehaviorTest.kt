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
package ca.odell.glazedlists.impl

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.event.ListEvent
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Collections
import java.util.concurrent.locks.ReentrantReadWriteLock
import java.util.function.Consumer

internal class ThreadSafeListBehaviorTest {
    @Test
    fun mutationsWriteThroughAndEventsAreForwarded() {
        val source = BasicEventList<String>().apply { addAll(listOf("alpha", "beta")) }
        val list = ThreadSafeList(source)
        val eventTypes = mutableListOf<Int>()
        list.addListEventListener { changes ->
            while (changes.next()) eventTypes += changes.type
        }

        list.add("gamma")
        list[0] = "ALPHA"
        list.removeAt(1)

        assertEquals(listOf("ALPHA", "gamma"), source)
        assertEquals(listOf(ListEvent.INSERT, ListEvent.UPDATE, ListEvent.DELETE), eventTypes)
        list.dispose()
    }

    @Test
    fun callbacksRunWhileTheAppropriateLockIsHeld() {
        val source = BasicEventList<String>().apply { addAll(listOf("alpha", "beta")) }
        val list = ThreadSafeList(source)
        val lock = source.readWriteLock as ReentrantReadWriteLock

        list.forEach(Consumer {
            assertTrue(lock.readHoldCount > 0)
        })
        list.removeIf {
            assertTrue(lock.isWriteLockedByCurrentThread)
            false
        }
        list.replaceAll {
            assertTrue(lock.isWriteLockedByCurrentThread)
            it.uppercase()
        }

        assertEquals(listOf("ALPHA", "BETA"), list)
        list.dispose()
    }

    @Test
    fun arrayAndNaturalSortContractsArePreserved() {
        val source = BasicEventList<String>().apply { addAll(listOf("beta", "alpha")) }
        val list = ThreadSafeList(source)

        Collections.sort(list, null)
        val oversized = arrayOf("unused", "unused", "tail")
        val result = list.toArray(oversized)

        assertEquals(listOf("alpha", "beta"), list)
        assertSame(oversized, result)
        assertArrayEquals(arrayOf("alpha", "beta", null), result)
        assertArrayEquals(arrayOf("alpha", "beta"), list.toArray())
        list.dispose()
    }
}
