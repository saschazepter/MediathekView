package ca.odell.glazedlists.impl

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.FunctionList
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.AbstractMap

internal class MapEntryMigrationBehaviorTest {
    @Test
    fun functionListMapEntriesKeepStandardMapEntryEquality() {
        val source = BasicEventList<String>().apply { addAll(listOf("a", "bb")) }
        val map = FunctionListMap(source, FunctionList.Function(String::length))
        val entry = map.entries.first { it.key == 1 }

        assertTrue(entry == AbstractMap.SimpleEntry(1, "a"))
        assertEquals(AbstractMap.SimpleEntry(1, "a").hashCode(), entry.hashCode())
        assertFalse(entry.equals("not an entry"))
    }

    @Test
    fun groupingMultiMapEntriesKeepStandardMapEntryEquality() {
        val source = BasicEventList<String>().apply { addAll(listOf("a1", "a2", "b1")) }
        val map = GroupingListMultiMap(
            source,
            FunctionList.Function { it.substring(0, 1) },
            Comparator.naturalOrder<String>(),
        )
        val entry = map.entries.first { it.key == "a" }
        val expected = AbstractMap.SimpleEntry("a", listOf("a1", "a2"))

        assertTrue(entry == expected)
        assertEquals(expected.hashCode(), entry.hashCode())
        assertFalse(entry.equals("not an entry"))
    }
}
