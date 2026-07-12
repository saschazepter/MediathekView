package ca.odell.glazedlists.impl.adt.barcode2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
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

    @Test
    fun colorsAreCopiedAndRemainUnmodifiable() {
        val source = mutableListOf<String?>("red", null)
        val coder = ListToByteCoder(source)

        source[0] = "changed"

        assertEquals("red", coder.byteToColor(1))
        assertNull(coder.byteToColor(2))
        assertThrows(UnsupportedOperationException::class.java) {
            (coder.colors as MutableList<String?>).add("blue")
        }
    }
}
