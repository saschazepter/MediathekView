package ca.odell.glazedlists.event

import ca.odell.glazedlists.BasicEventList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.function.Consumer

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

    @Test
    fun publisherRecoversAfterListenerThrowsError() {
        val publisher = SequenceDependenciesEventPublisher()
        val subject = Any()
        val delivered = mutableListOf<String>()
        var failNext = true
        val listener = Consumer<String> { event ->
            if (failNext) {
                failNext = false
                throw AssertionError("expected test failure")
            }
            delivered += event
        }
        val format = object : SequenceDependenciesEventPublisher.EventFormat<Any, Consumer<String>, String> {
            override fun fire(subject: Any, event: String, listener: Consumer<String>) = listener.accept(event)

            override fun postEvent(subject: Any) = Unit

            override fun isStale(subject: Any, listener: Consumer<String>): Boolean = false
        }
        publisher.addListener(subject, listener, format)

        assertThrows(AssertionError::class.java) {
            publisher.fireEvent(subject, "first", format)
        }
        assertDoesNotThrow {
            publisher.fireEvent(subject, "second", format)
        }

        assertEquals(listOf("second"), delivered)
    }
}
