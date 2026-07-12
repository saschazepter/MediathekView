package ca.odell.glazedlists

import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.impl.beans.BeanTableFormat
import ca.odell.glazedlists.impl.sort.ComparatorChain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

internal class JavaMigrationBehaviorTest {
    @Test
    fun tableFormatPreservesNullableCellValuesForKotlinCallers() {
        val format: TableFormat<String> = object : TableFormat<String> {
            override fun getColumnCount(): Int = 1

            override fun getColumnName(column: Int): String = "Optional value"

            override fun getColumnValue(baseObject: String, column: Int): Any? = null
        }

        val value: Any? = format.getColumnValue("row", 0)

        assertNull(value)
    }

    @Test
    fun comparatorChainKeepsOrderingCopyAndEqualityContracts() {
        val byLength = compareBy<String> { it.length }
        val alphabetically = Comparator.naturalOrder<String>()
        val source = mutableListOf(byLength, alphabetically)
        val chain = ComparatorChain(source)

        source.clear()

        assertEquals(-1, chain.compare("b", "aa"))
        assertEquals(-1, chain.compare("a", "b"))
        assertEquals(2, chain.comparators.size)
        assertEquals(chain, ComparatorChain(listOf(byLength, alphabetically)))
        assertNotEquals(chain, ComparatorChain(listOf(alphabetically)))
        assertEquals(0, chain.hashCode())
    }

    @Test
    fun functionAndPopularityListsKeepTheirMappedAndRankedResults() {
        val mappedSource = BasicEventList<String>().apply { addAll(listOf("a", "bbbb")) }
        val mapped = FunctionList(mappedSource, String::length)

        assertEquals(listOf(1, 4), mapped.toList())
        mappedSource[0] = "ccc"
        assertEquals(listOf(3, 4), mapped.toList())

        val popularitySource = BasicEventList<String>().apply {
            addAll(listOf("a", "b", "a", "c", "a", "b"))
        }
        val popularity = PopularityList.create(popularitySource)

        assertEquals(listOf("a", "b", "c"), popularity.toList())
        popularitySource.addAll(listOf("c", "c", "c"))
        assertEquals(listOf("c", "a", "b"), popularity.toList())
    }

    @Test
    fun beanTableFormatStillBoxesPrimitivesAndKeepsReferenceTypes() {
        val format = BeanTableFormat(
            SampleBean::class.java,
            arrayOf("count", "label"),
            arrayOf("Count", "Label"),
        )

        assertEquals(Int::class.javaObjectType, format.getColumnClass(0))
        assertEquals(String::class.java, format.getColumnClass(1))
        assertEquals(7, format.getColumnValue(SampleBean(7, "seven"), 0))
        assertEquals("seven", format.getColumnValue(SampleBean(7, "seven"), 1))
    }

    @Test
    fun thresholdListKeepsInclusiveOrderingAtIntegerExtremes() {
        val source = BasicEventList<Int>().apply {
            addAll(listOf(Int.MAX_VALUE, 0, Int.MIN_VALUE, -1))
        }
        val threshold = ThresholdList(source, ThresholdList.Evaluator<Int> { it })

        threshold.lowerThreshold = -1
        threshold.upperThreshold = Int.MAX_VALUE

        assertEquals(listOf(-1, 0, Int.MAX_VALUE), threshold.toList())

        threshold.setHeadRange(1, 2)

        assertEquals(listOf(-1, 0), threshold.toList())
    }

    class SampleBean(val count: Int, val label: String)
}
