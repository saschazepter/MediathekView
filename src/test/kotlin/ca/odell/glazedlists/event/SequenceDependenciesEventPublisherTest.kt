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

    @Test
    fun secondaryListenerAndCleanupFailuresAreSuppressedInDeliveryOrder() {
        val publisher = SequenceDependenciesEventPublisher()
        val subject = Any()
        val firstFailure = IllegalStateException("first listener")
        val secondFailure = IllegalArgumentException("second listener")
        val cleanupFailure = UnsupportedOperationException("cleanup")
        val format = object : SequenceDependenciesEventPublisher.EventFormat<Any, Consumer<String>, String> {
            override fun fire(subject: Any, event: String, listener: Consumer<String>) = listener.accept(event)

            override fun postEvent(subject: Any) = throw cleanupFailure

            override fun isStale(subject: Any, listener: Consumer<String>): Boolean = false
        }
        publisher.addListener(subject, Consumer { throw firstFailure }, format)
        publisher.addListener(subject, Consumer { throw secondFailure }, format)

        val thrown = assertThrows(IllegalStateException::class.java) {
            publisher.fireEvent(subject, "event", format)
        }

        assertSame(firstFailure, thrown)
        assertArrayEquals(arrayOf(secondFailure, cleanupFailure), thrown.suppressed)
    }

    @Test
    fun repeatedFailureInstanceIsNotSuppressedOnItself() {
        val publisher = SequenceDependenciesEventPublisher()
        val subject = Any()
        val failure = IllegalStateException("shared")
        val format = consumerFormat<Any>()
        publisher.addListener(subject, Consumer { throw failure }, format)
        publisher.addListener(subject, Consumer { throw failure }, format)

        val thrown = assertThrows(IllegalStateException::class.java) {
            publisher.fireEvent(subject, "event", format)
        }

        assertSame(failure, thrown)
        assertArrayEquals(emptyArray<Throwable>(), thrown.suppressed)
    }

    @Test
    fun equalSubjectsRemainDistinct() {
        val publisher = SequenceDependenciesEventPublisher()
        val firstSubject = EqualSubject("same")
        val secondSubject = EqualSubject("same")
        val firstEvents = mutableListOf<String>()
        val secondEvents = mutableListOf<String>()
        val format = consumerFormat<EqualSubject>()
        publisher.addListener(firstSubject, Consumer(firstEvents::add), format)
        publisher.addListener(secondSubject, Consumer(secondEvents::add), format)

        publisher.fireEvent(firstSubject, "first", format)
        publisher.fireEvent(secondSubject, "second", format)

        assertEquals(listOf("first"), firstEvents)
        assertEquals(listOf("second"), secondEvents)
    }

    @Test
    fun dependencyOrderIsPreservedDuringReentrantPublication() {
        val publisher = SequenceDependenciesEventPublisher()
        val upstreamSubject = Any()
        val downstreamSubject = Any()
        val delivered = mutableListOf<String>()
        val format = consumerFormat<Any>()
        val downstreamListener = Consumer<String> { delivered += it }
        val upstreamListener = Consumer<String> {
            delivered += it
            publisher.fireEvent(downstreamSubject, "downstream", format)
        }
        publisher.addListener(downstreamSubject, downstreamListener, format)
        publisher.addListener(upstreamSubject, upstreamListener, format)
        publisher.setRelatedListener(downstreamSubject, upstreamListener)

        publisher.fireEvent(upstreamSubject, "upstream", format)

        assertEquals(listOf("upstream", "downstream"), delivered)
    }

    @Test
    fun listenerCyclesAreRejected() {
        val publisher = SequenceDependenciesEventPublisher()
        val first = Any()
        val second = Any()
        publisher.setRelatedListener(first, second)

        assertThrows(IllegalStateException::class.java) {
            publisher.setRelatedListener(second, first)
        }
    }

    private fun <S> consumerFormat() =
        object : SequenceDependenciesEventPublisher.EventFormat<S, Consumer<String>, String> {
            override fun fire(subject: S, event: String, listener: Consumer<String>) = listener.accept(event)

            override fun postEvent(subject: S) = Unit

            override fun isStale(subject: S, listener: Consumer<String>): Boolean = false
        }

    private data class EqualSubject(val value: String)
}
