package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.event.ListEventListener
import ca.odell.glazedlists.impl.UpgradeDetectingReadWriteLock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.Spliterator
import java.util.concurrent.locks.ReentrantReadWriteLock


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
    fun constructorsPreserveOrCreateTheirInfrastructure() {
        val defaultList = BasicEventList<String>()
        assertInstanceOf(UpgradeDetectingReadWriteLock::class.java, defaultList.readWriteLock)

        val lock = ReentrantReadWriteLock()
        assertSame(lock, BasicEventList<String>(lock).readWriteLock)

        val publisher = defaultList.publisher
        val configured = BasicEventList<String>(4, publisher, lock)
        assertSame(publisher, configured.publisher)
        assertSame(lock, configured.readWriteLock)

        val fallbackLock = BasicEventList<String>(4, publisher, null)
        assertSame(publisher, fallbackLock.publisher)
        assertInstanceOf(UpgradeDetectingReadWriteLock::class.java, fallbackLock.readWriteLock)

        assertThrows(IllegalArgumentException::class.java) {
            BasicEventList<String>(-1)
        }
    }

    @Test
    fun emptyBulkOperationsAreNoOps() {
        val source = BasicEventList<String>()
        var events = 0
        source.addListEventListener { events++ }

        assertFalse(source.addAll(emptyList()))
        source.clear()
        assertFalse(source.removeIf { true })

        assertEquals(0, events)
    }

    @Test
    fun removeIfPublishesDeletedInstancesAndPreservesSurvivors() {
        val first = Box(1)
        val second = Box(2)
        val third = Box(3)
        val source = BasicEventList<Box>().apply { addAll(listOf(first, second, third)) }
        val removed = mutableListOf<Box>()
        source.addListEventListener { event ->
            while (event.next()) {
                if (event.type == ListEvent.DELETE) removed += event.oldValue
            }
        }

        assertTrue(source.removeIf { it.value != 2 })

        assertEquals(listOf(second), source)
        assertEquals(listOf(first, third), removed)
        assertSame(first, removed[0])
        assertSame(third, removed[1])
    }

    @Test
    fun replaceAllUsesIdentityToDecideWhetherToPublishUpdates() {
        val first = Box(1)
        val second = Box(2)
        val source = BasicEventList<Box>().apply { addAll(listOf(first, second)) }
        val updates = mutableListOf<Pair<Box, Box>>()
        source.addListEventListener { event ->
            while (event.next()) {
                if (event.type == ListEvent.UPDATE) updates += event.oldValue to event.newValue
            }
        }

        source.replaceAll { it }
        assertTrue(updates.isEmpty())

        source.replaceAll { Box(it.value) }
        assertEquals(2, updates.size)
        assertSame(first, updates[0].first)
        assertNotSame(first, updates[0].second)
        assertEquals(listOf(Box(1), Box(2)), source)
    }

    @Test
    fun traversalMethodsDelegateToTheBackingArrayList() {
        val source = BasicEventList<Int>().apply { addAll(listOf(1, 2, 3)) }
        val visited = mutableListOf<Int>()

        source.forEach(visited::add)

        assertEquals(listOf(1, 2, 3), visited)
        assertEquals(listOf(1, 2, 3), source.stream().toList())
        assertEquals(listOf(1, 2, 3), source.parallelStream().toList())
        assertTrue(source.spliterator().hasCharacteristics(Spliterator.ORDERED or Spliterator.SIZED))
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

        override fun listChanged(listChanges: ListEvent<E>) {
            if (listChanges.isReordering) {
                expected = listChanges.reorderMap.mapTo(ArrayList(expected.size), expected::get)
                changeCounts += listChanges.reorderMap.size
                reorderings += true
            } else {
                var changeCount = 0
                var previousIndex = -1
                var previousType = ListEvent.DELETE
                while (listChanges.next()) {
                    val index = listChanges.index
                    val type = listChanges.type
                    assertTrue(index > previousIndex || index == previousIndex && previousType == ListEvent.DELETE)
                    when (type) {
                        ListEvent.INSERT -> expected.add(index, source[index])
                        ListEvent.DELETE -> assertSame(expected.removeAt(index), listChanges.oldValue)
                        ListEvent.UPDATE -> assertSame(expected.set(index, source[index]), listChanges.oldValue)
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

    private data class Box(val value: Int)
}
