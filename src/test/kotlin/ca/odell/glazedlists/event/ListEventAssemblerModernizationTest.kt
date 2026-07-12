package ca.odell.glazedlists.event

import ca.odell.glazedlists.BasicEventList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test

internal class ListEventAssemblerModernizationTest {
    @Test
    fun bulkChangesRetainTheirTypesIndicesAndUnknownValues() {
        assertRange(ListEvent.INSERT, 2..4) { assembler -> assembler.elementsInserted(2, 4) }
        assertRange(ListEvent.UPDATE, 3..5) { assembler -> assembler.elementsUpdated(3, 5) }
        assertRange(ListEvent.DELETE, 1..3) { assembler -> assembler.elementsDeleted(1, 3) }
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

    private data class Change(val type: Int, val index: Int, val oldValue: Any?, val newValue: Any?)
}
