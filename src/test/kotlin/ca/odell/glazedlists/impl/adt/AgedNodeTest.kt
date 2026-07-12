package ca.odell.glazedlists.impl.adt

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors

internal class AgedNodeTest {
    @Test
    fun concurrentTimestampAllocationRemainsUnique() {
        val timestamps = Executors.newVirtualThreadPerTaskExecutor().use { executor ->
            List(200) {
                executor.submit<Long> { AgedNode(null, it).timestamp }
            }.map { it.get() }
        }

        assertEquals(timestamps.size, timestamps.toSet().size)
    }

    @Test
    fun readingValueStillAdvancesTimestamp() {
        val node = AgedNode(null, "value")
        val initialTimestamp = node.timestamp

        assertEquals("value", node.value)
        assertTrue(node.timestamp > initialTimestamp)
    }
}
