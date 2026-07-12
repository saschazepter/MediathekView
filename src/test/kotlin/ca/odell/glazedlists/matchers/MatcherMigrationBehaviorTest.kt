package ca.odell.glazedlists.matchers

import ca.odell.glazedlists.FunctionList
import ca.odell.glazedlists.TextFilterator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class MatcherMigrationBehaviorTest {
    @Test
    fun searchFieldEqualityStillDependsOnlyOnItsName() {
        val firstFilterator = TextFilterator<String> { baseList, element -> baseList.add(element) }
        val secondFilterator = TextFilterator<String> { baseList, element -> baseList.add(element.uppercase()) }
        val first = SearchEngineTextMatcherEditor.Field("title", firstFilterator)
        val sameName = SearchEngineTextMatcherEditor.Field("title", secondFilterator)
        val differentName = SearchEngineTextMatcherEditor.Field("topic", firstFilterator)

        assertEquals(first, sameName)
        assertEquals(first.hashCode(), sameName.hashCode())
        assertNotEquals(first, differentName)
        assertEquals("title", first.name)
        assertSame(firstFilterator, first.textFilterator)
    }

    @Test
    fun setMatcherKeepsItsOwnCopyOfTheMatchSet() {
        val editor = SetMatcherEditor.create<String, String>(SetMatcherEditor.Mode.WHITELIST_EMPTY_MATCH_NONE) { it }
        val selected = mutableSetOf("one")

        editor.setMatchSet(selected)
        selected.clear()

        assertTrue(editor.matcher.matches("one"))
        assertFalse(editor.matcher.matches("two"))
    }

    @Test
    fun thresholdMatcherKeepsAllComparisonOperationsAndExtraction() {
        val editor = ThresholdMatcherEditor<String, Int>(
            3,
            ThresholdMatcherEditor.GREATER_THAN_OR_EQUAL,
            Comparator.naturalOrder(),
            FunctionList.Function(String::length),
        )

        assertFalse(editor.matcher.matches("ab"))
        assertTrue(editor.matcher.matches("abc"))
        assertTrue(editor.matcher.matches("abcd"))

        editor.matchOperation = ThresholdMatcherEditor.LESS_THAN

        assertTrue(editor.matcher.matches("ab"))
        assertFalse(editor.matcher.matches("abc"))
    }
}
