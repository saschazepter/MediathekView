package ca.odell.glazedlists.event

import ca.odell.glazedlists.BasicEventList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test

internal class SequenceDependenciesEventPublisherTest {
    @Test
    fun removingUnknownListenerRemainsNoOp() {
        val publisher = SequenceDependenciesEventPublisher()

        assertDoesNotThrow {
            publisher.removeListener(Any(), Any())
        }
    }

    @Test
    fun repeatedUpdatesRetainOriginalAndLatestValues() {
        val source = BasicEventList<String>().apply { add("latest") }
        val assembler = ListEventAssembler(source, source.publisher)
        val previousValues = mutableListOf<String>()
        assembler.addListEventListener { event ->
            while (event.next()) previousValues += event.oldValue
        }

        assembler.beginEvent()
        assembler.elementUpdated(0, "original", "middle")
        assembler.elementUpdated(0, "middle", "latest")
        assembler.commitEvent()

        assertEquals(listOf("original"), previousValues)
        assertEquals("latest", source.single())
    }
}
