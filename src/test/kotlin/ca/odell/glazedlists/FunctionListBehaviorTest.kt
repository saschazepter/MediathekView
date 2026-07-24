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
package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.event.ListEventListener
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.RandomAccess
import java.util.function.Function
import java.util.function.Predicate

internal class FunctionListBehaviorTest {
    @Test
    fun initialMappingPreservesNullSourcesNullResultsAndFunctionIdentity() {
        val source = BasicEventList<String?>().apply { addAll(listOf(null, "aa", "skip")) }
        val forward = Function<String?, Int?> { value -> if (value == "skip") null else value?.length }
        val reverse = Function<Int?, String?> { length -> length?.let { "x".repeat(it) } }

        val mapped = FunctionList(source, forward, reverse)

        assertTrue(mapped is RandomAccess)
        assertSame(forward, mapped.forwardFunction)
        assertSame(reverse, mapped.reverseFunction)
        assertEquals(listOf(null, 2, null), mapped.toList())
        assertNull(mapped[0])
    }

    @Test
    fun addSetAndRemoveWriteThroughAndReturnPriorMappedValues() {
        val source = BasicEventList<Int>().apply { addAll(listOf(1, 2)) }
        val mapped = FunctionList(source, Function<Int, String>(Int::toString), Function(String::toInt))

        assertEquals("1", mapped.set(0, "8"))
        mapped.add(1, "7")
        assertTrue(mapped.add("9"))
        assertEquals("2", mapped.removeAt(2))

        assertEquals(listOf(8, 7, 9), source)
        assertEquals(listOf("8", "7", "9"), mapped.toList())
    }

    @Test
    fun writesWithoutReverseFailWithExactDiagnosticBeforeMutation() {
        val source = BasicEventList<Int>().apply { add(1) }
        val mapped = FunctionList(source, Function<Int, String>(Int::toString))

        listOf<() -> Unit>(
            { mapped.add("2") },
            { mapped.add(0, "2") },
            { mapped[0] = "2" },
        ).forEach { operation ->
            val failure = assertThrows(IllegalStateException::class.java, operation)
            assertEquals(
                "A reverse mapping function must be specified to support this List operation",
                failure.message,
            )
            assertEquals(listOf(1), source)
            assertEquals(listOf("1"), mapped.toList())
        }

        mapped.reverseFunction = Function(String::toInt)
        mapped.add("2")
        mapped.reverseFunction = null
        assertNull(mapped.reverseFunction)
        assertEquals(listOf(1, 2), source)
    }

    @Test
    fun sourceInsertUpdateDeletePublishExactMappedEvents() {
        val source = BasicEventList<String>().apply { add("a") }
        val mapped = FunctionList(source, Function<String, String> { it.uppercase() })
        val events = mutableListOf<EventSnapshot<String>>()
        mapped.addListEventListener(recordingListener(events))

        source.add(0, "b")
        source[1] = "cc"
        source.removeAt(0)

        assertEquals(listOf("CC"), mapped.toList())
        assertEquals(
            listOf(
                EventSnapshot(false, null, listOf(Change(ListEvent.INSERT, 0, ListEvent.unknownValue(), "B"))),
                EventSnapshot(false, null, listOf(Change(ListEvent.UPDATE, 1, "A", "CC"))),
                EventSnapshot(false, null, listOf(Change(ListEvent.DELETE, 0, "B", ListEvent.unknownValue()))),
            ),
            events,
        )
    }

    @Test
    fun sourceReorderReusesMappedIdentitiesAndForwardsTheReorderMap() {
        val unsorted = BasicEventList<Int>().apply { addAll(listOf(2, 1, 3)) }
        val source = SortedList(unsorted, null)
        val mapped = FunctionList(source, Function<Int, Box> { Box(it) })
        val original = mapped.toList()
        val events = mutableListOf<EventSnapshot<Box>>()
        mapped.addListEventListener(recordingListener(events))

        source.comparator = Comparator.naturalOrder()

        assertEquals(listOf(1, 2, 3), mapped.map(Box::value))
        assertSame(original[1], mapped[0])
        assertSame(original[0], mapped[1])
        assertSame(original[2], mapped[2])
        assertEquals(
            listOf(EventSnapshot<Box>(true, listOf(1, 0, 2), emptyList())),
            events,
        )
    }

    @Test
    fun advancedFunctionReceivesPriorMappedValueAndCurrentSourceOnUpdateAndDelete() {
        val calls = mutableListOf<String>()
        val source = BasicEventList<String>().apply { add("a") }
        val function = object : FunctionList.AdvancedFunction<String, Box> {
            override fun apply(value: String): Box = Box(value.length).also { calls += "apply:$value:${it.value}" }

            override fun reevaluate(sourceValue: String, transformedValue: Box): Box =
                Box(sourceValue.length).also {
                    calls += "reevaluate:$sourceValue:${transformedValue.value}:${it.value}"
                }

            override fun dispose(sourceValue: String, transformedValue: Box) {
                calls += "dispose:$sourceValue:${transformedValue.value}"
            }
        }
        val mapped = FunctionList(source, function)
        val first = mapped[0]

        source[0] = "bbb"
        val second = mapped[0]
        source.removeAt(0)

        assertNotSame(first, second)
        assertSame(function, mapped.forwardFunction)
        assertEquals(
            listOf("apply:a:1", "reevaluate:bbb:1:3", "dispose:bbb:3"),
            calls,
        )
    }

    @Test
    fun advancedFunctionPreservesTheJavaSourceCopyReorderBehavior() {
        val disposals = mutableListOf<Pair<Int, Int>>()
        val source = SortedList(BasicEventList<Int>().apply { addAll(listOf(2, 1)) }, null)
        val function = object : FunctionList.AdvancedFunction<Int, Box> {
            override fun apply(value: Int): Box = Box(value)
            override fun reevaluate(sourceValue: Int, transformedValue: Box): Box = Box(sourceValue)
            override fun dispose(sourceValue: Int, transformedValue: Box) {
                disposals += sourceValue to transformedValue.value
            }
        }
        FunctionList(source, function)

        source.comparator = Comparator.naturalOrder()
        source.removeAt(0)

        assertEquals(listOf(2 to 1), disposals)
    }

    @Test
    fun switchingNormalAndAdvancedFunctionsRebuildsAndTruncatesDisposalState() {
        val source = BasicEventList<Int>().apply { addAll(listOf(1, 2)) }
        val normal = Function<Int, Box> { Box(it) }
        val mapped = FunctionList(source, normal)
        val disposals = mutableListOf<Pair<Int, Int>>()
        val advanced = object : FunctionList.AdvancedFunction<Int, Box> {
            override fun apply(value: Int): Box = Box(value * 10)
            override fun reevaluate(sourceValue: Int, transformedValue: Box): Box = Box(sourceValue * 10)
            override fun dispose(sourceValue: Int, transformedValue: Box) {
                disposals += sourceValue to transformedValue.value
            }
        }

        mapped.forwardFunction = advanced
        source.removeAt(0)
        mapped.forwardFunction = normal
        source.removeAt(0)

        assertEquals(listOf(1 to 10), disposals)
        assertTrue(mapped.isEmpty())
    }

    @Test
    fun forwardReplacementPublishesUpdatesAndRetainsPartialStateWhenMappingThrows() {
        val source = BasicEventList<String>().apply { addAll(listOf("a", "bb", "ccc")) }
        val mapped = FunctionList(source, Function<String, Int>(String::length))
        val successfulEvents = mutableListOf<EventSnapshot<Int>>()
        mapped.addListEventListener(recordingListener(successfulEvents))
        val replacement = Function<String, Int> { it.length * 10 }

        mapped.forwardFunction = replacement

        assertSame(replacement, mapped.forwardFunction)
        assertEquals(listOf(10, 20, 30), mapped.toList())
        assertEquals(
            listOf(
                Change(ListEvent.UPDATE, 0, 1, 10),
                Change(ListEvent.UPDATE, 1, 2, 20),
                Change(ListEvent.UPDATE, 2, 3, 30),
            ),
            successfulEvents.single().changes,
        )

        val throwing = Function<String, Int> { if (it == "bb") error("boom") else 100 + it.length }
        val failure = assertThrows(IllegalStateException::class.java) { mapped.forwardFunction = throwing }
        assertEquals("boom", failure.message)
        assertSame(throwing, mapped.forwardFunction)
        assertEquals(101, mapped[0])
        assertEquals(20, mapped[1])
        assertEquals(30, mapped[2])
        assertEquals(1, successfulEvents.size)
    }

    @Test
    fun removeIfScansBackwardsUsesMappedIdentityAndBatchesAbstractSourceEvents() {
        val source = BasicEventList<Int>().apply { addAll(listOf(1, 2, 3, 4)) }
        val mapped = FunctionList(source, Function<Int, Box>(::Box))
        val visited = mutableListOf<Int>()
        val target = mapped[1]
        var sourceEvents = 0
        source.addListEventListener { sourceEvents++ }

        val changed = mapped.removeIf(Predicate { value ->
            visited += value.value
            value === target || value.value == 4
        })

        assertTrue(changed)
        assertEquals(listOf(4, 3, 2, 1), visited)
        assertEquals(listOf(1, 3), source)
        assertEquals(1, sourceEvents)
    }

    @Test
    fun removeIfOnNonAbstractSourcePublishesOneSourceEventPerRemoval() {
        val delegate = BasicEventList<Int>().apply { addAll(listOf(1, 2, 3, 4)) }
        val source = NonAbstractEventList(delegate)
        val mapped = FunctionList(source, Function<Int, Int> { it })
        var sourceEvents = 0
        delegate.addListEventListener { sourceEvents++ }

        assertTrue(mapped.removeIf(Predicate { it % 2 == 0 }))

        assertEquals(listOf(1, 3), delegate)
        assertEquals(2, sourceEvents)
    }

    @Test
    fun removeIfPredicateFailureLeavesCompletedRemovalsVisible() {
        val source = BasicEventList<Int>().apply { addAll(listOf(1, 2, 3)) }
        val mapped = FunctionList(source, Function<Int, Int> { it })

        val failure = assertThrows(IllegalArgumentException::class.java) {
            mapped.removeIf(Predicate { value ->
                when (value) {
                    3 -> true
                    2 -> throw IllegalArgumentException("predicate failed")
                    else -> false
                }
            })
        }

        assertEquals("predicate failed", failure.message)
        assertEquals(listOf(1, 2), source)
        assertEquals(listOf(1, 2), mapped.toList())
    }

    @Test
    fun inheritedDisposeDetachesTheListenerWithoutDisposingMappedValues() {
        val disposals = mutableListOf<Pair<String, Int>>()
        val source = BasicEventList<String>().apply { add("a") }
        val function = object : FunctionList.AdvancedFunction<String, Int> {
            override fun apply(value: String): Int = value.length
            override fun reevaluate(sourceValue: String, transformedValue: Int): Int = sourceValue.length
            override fun dispose(sourceValue: String, transformedValue: Int) {
                disposals += sourceValue to transformedValue
            }
        }
        val mapped = FunctionList(source, function)
        var events = 0
        mapped.addListEventListener { events++ }

        mapped.dispose()
        source.add("bb")

        assertEquals(0, events)
        assertTrue(disposals.isEmpty())
        assertEquals(2, mapped.size)
        assertEquals(1, mapped[0])
        assertThrows(IndexOutOfBoundsException::class.java) { mapped[1] }
    }

    private fun <E> recordingListener(target: MutableList<EventSnapshot<E>>): ListEventListener<E> =
        ListEventListener { event ->
            if (event.isReordering) {
                target += EventSnapshot(true, event.reorderMap.toList(), emptyList())
            } else {
                val changes = mutableListOf<Change<E>>()
                while (event.next()) {
                    changes += Change(event.type, event.index, event.getOldValue(), event.getNewValue())
                }
                target += EventSnapshot(false, null, changes)
            }
        }

    private data class Box(val value: Int)

    private data class Change<E>(val type: Int, val index: Int, val oldValue: E?, val newValue: E?)

    private data class EventSnapshot<E>(
        val reordering: Boolean,
        val reorderMap: List<Int>?,
        val changes: List<Change<E>>,
    )

    private class NonAbstractEventList<E>(private val delegate: EventList<E>) : EventList<E> by delegate
}
