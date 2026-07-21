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
package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class GroupingListBehaviorTest {
    @Test
    fun insertingNewGroupPublishesInsert() {
        val source = BasicEventList<String>()

        GroupingList(source).use { grouping ->
            val changeTypes = grouping.recordChangeTypes()
            source.add("a")

            assertEquals(listOf(listOf("a")), grouping.map { it.toList() })
            assertEquals(listOf(ListEvent.INSERT), changeTypes)
        }
    }

    @Test
    fun deletingCompleteGroupPublishesDelete() {
        val source = BasicEventList<String>().apply { add("a") }

        GroupingList(source).use { grouping ->
            val changeTypes = grouping.recordChangeTypes()
            source.removeAt(0)

            assertTrue(grouping.isEmpty())
            assertEquals(listOf(ListEvent.DELETE), changeTypes)
        }
    }

    @Test
    fun updatingGroupPublishesUpdate() {
        val source = BasicEventList<String>().apply { add("a") }

        GroupingList(source, String.CASE_INSENSITIVE_ORDER).use { grouping ->
            val changeTypes = grouping.recordChangeTypes()
            source[0] = "A"

            assertEquals(listOf(listOf("A")), grouping.map { it.toList() })
            assertEquals(listOf(ListEvent.UPDATE), changeTypes)
        }
    }

    @Test
    fun batchedDeletionsKeepGroupsConsistent() {
        val source = BasicEventList<String>().apply { addAll(listOf("a", "b", "b", "c")) }

        TransactionList(source).use { transaction ->
            GroupingList(transaction).use { grouping ->
                transaction.withTransaction {
                    transaction.removeAt(0)
                    transaction.removeAt(0)
                }

                assertEquals(listOf(listOf("b"), listOf("c")), grouping.map { it.toList() })
            }
        }
    }

    private fun <E> EventList<E>.recordChangeTypes() = mutableListOf<Int>().also { changeTypes ->
        addListEventListener { changes ->
            while (changes.next()) changeTypes += changes.type
        }
    }
}
