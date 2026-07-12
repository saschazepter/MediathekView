package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.event.ListEventAssembler
import ca.odell.glazedlists.event.ListEventListener
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.Serializable
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
    fun sortingProducesCoherentReorderEvent() {
        val source = BasicEventList<String>().apply { addAll(listOf("B", "A", "C")) }
        val sorted = SortedList(source, naturalOrder())
        val consistency = ConsistencyListener(sorted)

        sorted.comparator = reverseOrder()

        assertEquals(listOf("C", "B", "A"), sorted)
        assertEquals(listOf(true), consistency.reorderings)
    }

    @Test
    fun serializableListenersSurviveRoundTrip() {
        SerializableListenerState.lastSource = null
        val original = BasicEventList<String>()
        original.addListEventListener(SerializableRecordingListener())
        original.add("before")
        assertSame(original, SerializableListenerState.lastSource)

        val restored = roundTrip(original)
        restored.add("after")

        assertSame(restored, SerializableListenerState.lastSource)
    }

    @Test
    fun nonSerializableListenersAreExcludedFromRoundTrip() {
        NonSerializableListenerState.lastSource = null
        val original = BasicEventList<String>()
        original.addListEventListener(NonSerializableRecordingListener())
        original.add("before")
        assertSame(original, NonSerializableListenerState.lastSource)

        val restored = roundTrip(original)
        restored.add("after")

        assertSame(original, NonSerializableListenerState.lastSource)
        assertFalse(restored === NonSerializableListenerState.lastSource)
    }

    @Test
    fun sharedLockAndPublisherRemainSharedAfterRoundTrip() {
        val sharedLock = ReentrantReadWriteLock()
        val sharedPublisher = ListEventAssembler.createListEventPublisher()
        val original = ArrayList<EventList<String>>()
        repeat(4) { index ->
            original += BasicEventList<String>(sharedPublisher, sharedLock).apply { add("Test $index") }
        }

        val restored = roundTrip(original)
        val restoredLock = restored.first().getReadWriteLock()
        val restoredPublisher = restored.first().getPublisher()
        val composite = CompositeList<String>(restoredPublisher, restoredLock)

        restored.forEach { list ->
            assertSame(restoredLock, list.getReadWriteLock())
            assertSame(restoredPublisher, list.getPublisher())
            composite.addMemberList(list)
        }
        assertEquals(original.flatten(), composite)
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

    private class SerializableRecordingListener : ListEventListener<String>, Serializable {
        override fun listChanged(changes: ListEvent<String>) {
            SerializableListenerState.lastSource = changes.sourceList
        }

        private companion object {
            const val serialVersionUID = 1L
        }
    }

    private class NonSerializableRecordingListener : ListEventListener<String> {
        override fun listChanged(changes: ListEvent<String>) {
            NonSerializableListenerState.lastSource = changes.sourceList
        }
    }

    private object SerializableListenerState {
        var lastSource: EventList<String>? = null
    }

    private object NonSerializableListenerState {
        var lastSource: EventList<String>? = null
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

    private fun <T> roundTrip(value: T): T {
        val bytes = ByteArrayOutputStream().use { output ->
            ObjectOutputStream(output).use { it.writeObject(value) }
            output.toByteArray()
        }
        return ObjectInputStream(ByteArrayInputStream(bytes)).use {
            @Suppress("UNCHECKED_CAST")
            it.readObject() as T
        }
    }
}
