package ca.odell.glazedlists

import ca.odell.glazedlists.impl.filter.SearchTerm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SearchTermTest {
    @Test
    fun replacementPreservesMetadataButNotScratchState() {
        val original = SearchTerm<Any>("old", true, true, null)
        original.fieldFilterStrings += "temporary"

        val replacement = original.newSearchTerm("new")

        assertEquals("new", replacement.text)
        assertTrue(replacement.isNegated)
        assertTrue(replacement.isRequired)
        assertTrue(replacement.fieldFilterStrings.isEmpty())
    }

    @Test
    fun equalityIgnoresReusableFilterStrings() {
        val first = SearchTerm<Any>("term")
        val second = SearchTerm<Any>("term")
        first.fieldFilterStrings += "extracted"

        assertEquals(first, second)
        assertEquals(first.hashCode(), second.hashCode())
        assertNotEquals(first, SearchTerm<Any>("other"))
    }

    @Test
    fun constrainmentAndRelaxationPreservePositiveAndNegativeSemantics() {
        val broad = SearchTerm<Any>("cat")
        val narrow = SearchTerm<Any>("catalog")
        assertTrue(narrow.isConstrainment(broad))
        assertTrue(broad.isRelaxation(narrow))

        val negatedBroad = SearchTerm<Any>("cat", true, false, null)
        val negatedNarrow = SearchTerm<Any>("catalog", true, false, null)
        assertTrue(negatedBroad.isConstrainment(negatedNarrow))
        assertFalse(broad.isConstrainment(negatedBroad))
        assertFalse(broad.isConstrainment(SearchTerm<Any>("cat")))
    }
}
