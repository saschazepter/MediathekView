package ca.odell.glazedlists.event

import ca.odell.glazedlists.BasicEventList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ListEventAssemblerModernizationTest {
    @Test
    fun nullListenersAreRejectedWithTheExistingMessage() {
        val source = BasicEventList<String>()
        val assembler = ListEventAssembler(source, source.publisher)

        val addFailure = assertThrows(NullPointerException::class.java) {
            assembler.addListEventListener(null)
        }
        val removeFailure = assertThrows(NullPointerException::class.java) {
            assembler.removeListEventListener(null)
        }

        assertEquals("ListEventListener is undefined", addFailure.message)
        assertEquals("ListEventListener is undefined", removeFailure.message)
    }

    @Test
    fun bulkChangesRetainTheirTypesIndicesAndUnknownValues() {
        assertRange(ListEvent.INSERT, 2..4) { assembler -> assembler.elementsInserted(2, 4) }
        assertRange(ListEvent.UPDATE, 3..5) { assembler -> assembler.elementsUpdated(3, 5) }
        assertRange(ListEvent.DELETE, 1..3) { assembler -> assembler.elementsDeleted(1, 3) }
    }

    @Test
    fun linearEventsRetainSuppliedNewValues() {
        val oldValue = Any()
        val insertedValue = Any()
        val updatedValue = Any()

        val insert = collectChanges { assembler -> assembler.elementInserted(0, insertedValue) }.single()
        val update = collectChanges { assembler -> assembler.elementUpdated(0, oldValue, updatedValue) }.single()

        assertSame(ListEvent.UNKNOWN_VALUE, insert.oldValue)
        assertSame(insertedValue, insert.newValue)
        assertSame(oldValue, update.oldValue)
        assertSame(updatedValue, update.newValue)
    }

    @Test
    fun treeFallbackRetainsPerElementOldValuesForOverlappingUpdates() {
        val oldValue = Any()

        val changes = collectChanges(2) { assembler ->
            assembler.elementUpdated(0, oldValue, Any())
            assembler.elementsUpdated(0, 1)
        }

        assertEquals(listOf(0, 1), changes.map(Change::index))
        assertEquals(listOf(ListEvent.UPDATE, ListEvent.UPDATE), changes.map(Change::type))
        assertSame(oldValue, changes[0].oldValue)
        assertSame(ListEvent.UNKNOWN_VALUE, changes[1].oldValue)
    }

    @Test
    fun treeFallbackRetainsOriginalOldValueAndLatestNewValueForRepeatedUpdate() {
        val originalValue = Any()
        val intermediateValue = Any()
        val latestValue = Any()

        val update = collectChanges(1) { assembler ->
            assembler.elementUpdated(0, originalValue, intermediateValue)
            assembler.elementUpdated(0, intermediateValue, latestValue)
        }.single()

        assertEquals(ListEvent.UPDATE, update.type)
        assertSame(originalValue, update.oldValue)
        assertSame(latestValue, update.newValue)
    }

    @Test
    fun treeFallbackRetainsPerElementOldValuesForOverlappingDeletes() {
        val oldValue = Any()

        val changes = collectChanges(2) { assembler ->
            assembler.elementUpdated(0, oldValue, Any())
            assembler.elementsDeleted(0, 1)
        }

        assertEquals(listOf(0, 0), changes.map(Change::index))
        assertEquals(listOf(ListEvent.DELETE, ListEvent.DELETE), changes.map(Change::type))
        assertSame(oldValue, changes[0].oldValue)
        assertSame(ListEvent.UNKNOWN_VALUE, changes[1].oldValue)
    }

    @Test
    fun treeFallbackExposesInsertedValuesAsNewValues() {
        val firstValue = Any()
        val secondValue = Any()

        val changes = collectChanges(2) { assembler ->
            assembler.elementInserted(1, firstValue)
            assembler.elementInserted(0, secondValue)
        }

        assertEquals(listOf(0, 2), changes.map(Change::index))
        assertEquals(listOf(ListEvent.INSERT, ListEvent.INSERT), changes.map(Change::type))
        changes.forEach { change -> assertSame(ListEvent.UNKNOWN_VALUE, change.oldValue) }
        assertSame(secondValue, changes[0].newValue)
        assertSame(firstValue, changes[1].newValue)
    }

    @Test
    fun contradictoryUpdateOfInsertedElementRetainsMostRecentValue() {
        val insertedValue = Any()
        val updatedValue = Any()

        val changes = collectChanges(allowContradictingEvents = true) { assembler ->
            assembler.elementInserted(0, insertedValue)
            assembler.elementUpdated(0, insertedValue, updatedValue)
        }

        val insert = changes.single()
        assertEquals(ListEvent.INSERT, insert.type)
        assertSame(ListEvent.UNKNOWN_VALUE, insert.oldValue)
        assertSame(updatedValue, insert.newValue)
    }

    @Test
    fun linearEventReportsRemainingBlocks() {
        assertRemainingBlocks(forceTreeFallback = false)
    }

    @Test
    fun treeEventReportsRemainingBlocks() {
        assertRemainingBlocks(forceTreeFallback = true)
    }

    @Test
    fun treeFallbackPreservesPayloadIdentityAcrossCallsForCoalescing() {
        val oldValue = Any()
        val newValue = Any()
        val source = BasicEventList<Any>()
        repeat(3) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        assembler.addListEventListener { event ->
            assertEquals(2, event.blocksRemaining)
            val indices = mutableListOf<Int>()
            while (event.next()) {
                indices += event.index
                assertSame(oldValue, event.oldValue)
                assertSame(newValue, event.newValue)
            }
            assertEquals(listOf(0, 1, 2), indices)
        }

        assembler.beginEvent()
        assembler.elementUpdated(2, oldValue, newValue)
        assembler.elementUpdated(0, oldValue, newValue)
        assembler.elementUpdated(1, oldValue, newValue)
        assembler.commitEvent()
    }

    @Test
    fun treeEventCopiesIterateIndependentlyWithTheSameValues() {
        val firstValue = Any()
        val secondValue = Any()
        val source = BasicEventList<Any>()
        repeat(2) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        assembler.addListEventListener { event ->
            val copy = event.copy()

            assertTrue(event.next())
            assertTrue(copy.next())
            assertEquals(event.index, copy.index)
            assertSame(event.oldValue, copy.oldValue)
            assertSame(event.newValue, copy.newValue)

            assertTrue(event.next())
            assertEquals(0, copy.index)
            assertTrue(copy.next())
            assertEquals(event.index, copy.index)
            assertSame(secondValue, event.newValue)
            assertSame(secondValue, copy.newValue)
        }

        assembler.beginEvent()
        assembler.elementInserted(1, secondValue)
        assembler.elementInserted(0, firstValue)
        assembler.commitEvent()
    }

    private fun assertRange(
        expectedType: Int,
        expectedIndices: IntRange,
        addChanges: (ListEventAssembler<String>) -> Unit,
    ) {
        val source = BasicEventList<String>()
        val assembler = ListEventAssembler(source, source.publisher)
        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event ->
            while (event.next()) {
                changes += Change(event.type, event.index, event.oldValue, event.newValue)
            }
        }

        assembler.beginEvent()
        addChanges(assembler)
        assembler.commitEvent()

        val expectedEventIndices = if (expectedType == ListEvent.DELETE) {
            List(expectedIndices.count()) { expectedIndices.first }
        } else {
            expectedIndices.toList()
        }
        assertEquals(expectedEventIndices, changes.map(Change::index))
        assertEquals(List(expectedIndices.count()) { expectedType }, changes.map(Change::type))
        changes.forEach { change ->
            assertSame(ListEvent.UNKNOWN_VALUE, change.oldValue)
            assertSame(ListEvent.UNKNOWN_VALUE, change.newValue)
        }
    }

    private fun collectChanges(
        sourceSize: Int = 0,
        allowContradictingEvents: Boolean = false,
        addChanges: (ListEventAssembler<Any>) -> Unit,
    ): List<Change> {
        val source = BasicEventList<Any>()
        repeat(sourceSize) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        val changes = mutableListOf<Change>()
        assembler.addListEventListener { event ->
            while (event.next()) {
                changes += Change(event.type, event.index, event.oldValue, event.newValue)
            }
        }

        assembler.beginEvent(allowContradictingEvents)
        addChanges(assembler)
        assembler.commitEvent()
        return changes
    }

    private fun assertRemainingBlocks(forceTreeFallback: Boolean) {
        val source = BasicEventList<Any>()
        repeat(3) { source += Any() }
        val assembler = ListEventAssembler(source, source.publisher)
        val counts = mutableListOf<Int>()
        assembler.addListEventListener { event ->
            counts += event.blocksRemaining
            assertTrue(event.nextBlock())
            counts += event.blocksRemaining
            assertTrue(event.nextBlock())
            counts += event.blocksRemaining
            assertFalse(event.nextBlock())
        }

        assembler.beginEvent()
        if (forceTreeFallback) {
            assembler.elementUpdated(2, Any(), Any())
            assembler.elementUpdated(0, Any(), Any())
        } else {
            assembler.elementUpdated(0, Any(), Any())
            assembler.elementUpdated(2, Any(), Any())
        }
        assembler.commitEvent()

        assertEquals(listOf(2, 1, 0), counts)
    }

    private data class Change(val type: Int, val index: Int, val oldValue: Any?, val newValue: Any?)
}
