package ca.odell.glazedlists.event

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class EventInterfacesBehaviorTest {
    @Test
    fun publisherContractRetainsNullableObjectParameters() {
        val publisher = RecordingPublisher()

        publisher.setRelatedSubject("listener", null)
        publisher.clearRelatedSubject(null)
        publisher.setRelatedListener(null, "related")
        publisher.clearRelatedListener("subject", null)

        assertEquals(
            listOf(
                listOf("setSubject", "listener", null),
                listOf("clearSubject", null),
                listOf("setListener", null, "related"),
                listOf("clearListener", "subject", null),
            ),
            publisher.calls,
        )
    }

    private class RecordingPublisher : ListEventPublisher {
        val calls = mutableListOf<List<Any?>>()

        override fun setRelatedSubject(listener: Any?, relatedSubject: Any?) {
            calls += listOf("setSubject", listener, relatedSubject)
        }

        override fun clearRelatedSubject(listener: Any?) {
            calls += listOf("clearSubject", listener)
        }

        override fun setRelatedListener(subject: Any?, relatedListener: Any?) {
            calls += listOf("setListener", subject, relatedListener)
        }

        override fun clearRelatedListener(subject: Any?, relatedListener: Any?) {
            calls += listOf("clearListener", subject, relatedListener)
        }
    }
}
