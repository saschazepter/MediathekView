package ca.odell.glazedlists.impl.adt.barcode2

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class TreeMigrationBehaviorTest {
    @Test
    fun repeatedColorsAndIndentedNodesKeepTheirRepresentations() {
        val coder = ListToByteCoder(listOf("R", "B"))
        val tree = FourColorTree<String>(coder)
        val allColors = (1 or 2).toByte()

        tree.add(0, allColors, 1, "red", 3)
        tree.add(3, allColors, 2, "blue", 2)

        assertEquals("RRRBB", tree.asSequenceOfColors())
        assertEquals(3, tree.size(1))
        assertEquals(2, tree.size(2))
        assertEquals("R [3]: red\n   B [2]: blue\n", tree.toString())
    }
}
