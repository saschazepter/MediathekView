package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.event.ListEventListener
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

internal class BasicEventListBehaviorTest {
    @Test
    fun kotlinUseDisposesEventListEvenWhenTheBlockFails() {
        val source = TrackingEventList<String>()

        assertThrows(IllegalStateException::class.java) {
            source.use { error("expected failure") }
        }

        assertTrue(source.disposed)
    }

    @Test
    fun bulkMutationsProduceSingleCoherentEvents() {
        val source = BasicEventList<String>()
        val consistency = ConsistencyListener(source)

        source.addAll(listOf("A", "B", "C"))
        source.clear()

        assertEquals(listOf(3, 3), consistency.changeCounts)
        assertEquals(2, consistency.eventCount)
        assertTrue(source.isEmpty())
    }

    @Test
    fun sortingProducesCoherentReorderEvent() {
        val source = BasicEventList<String>().apply { addAll(listOf("B", "A", "C")) }
        val sorted = SortedList(source, naturalOrder())
        val consistency = ConsistencyListener(sorted)

        sorted.comparator = reverseOrder()

        assertEquals(listOf("C", "B", "A"), sorted)
        assertEquals(listOf(true), consistency.reorderings)
    }

    private class ConsistencyListener<E>(private val source: EventList<E>) : ListEventListener<E> {
        private var expected = source.toMutableList()
        val changeCounts = mutableListOf<Int>()
        val reorderings = mutableListOf<Boolean>()
        val eventCount: Int get() = changeCounts.size

        init {
            source.addListEventListener(this)
        }

        override fun listChanged(changes: ListEvent<E>) {
            if (changes.isReordering) {
                expected = changes.reorderMap.mapTo(ArrayList(expected.size), expected::get)
                changeCounts += changes.reorderMap.size
                reorderings += true
            } else {
                var changeCount = 0
                var previousIndex = -1
                var previousType = ListEvent.DELETE
                while (changes.next()) {
                    val index = changes.index
                    val type = changes.type
                    assertTrue(index > previousIndex || index == previousIndex && previousType == ListEvent.DELETE)
                    when (type) {
                        ListEvent.INSERT -> expected.add(index, source[index])
                        ListEvent.DELETE -> assertSame(expected.removeAt(index), changes.oldValue)
                        ListEvent.UPDATE -> assertSame(expected.set(index, source[index]), changes.oldValue)
                    }
                    previousIndex = index
                    previousType = type
                    changeCount++
                }
                changeCounts += changeCount
                reorderings += false
            }
            assertEquals(expected, source)
        }
    }

    private class TrackingEventList<E> : AbstractEventList<E>() {
        var disposed = false
            private set

        override val size: Int = 0

        override fun get(index: Int): E = throw IndexOutOfBoundsException(index)

        override fun dispose() {
            disposed = true
        }
    }
}
