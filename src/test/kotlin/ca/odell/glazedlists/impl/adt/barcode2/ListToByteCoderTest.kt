package ca.odell.glazedlists.impl.adt.barcode2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class ListToByteCoderTest {
    @Test
    fun singleColorBitsMapToTheirIndices() {
        listOf<Byte>(1, 2, 4, 8, 16, 32, 64).forEachIndexed { index, color ->
            assertEquals(index, ListToByteCoder.colorAsIndex(color))
        }
    }

    @Test
    fun combinedOrMissingColorBitsAreRejected() {
        listOf<Byte>(0, 3, -128).forEach { color ->
            assertThrows(IllegalArgumentException::class.java) {
                ListToByteCoder.colorAsIndex(color)
            }
        }
    }
}
