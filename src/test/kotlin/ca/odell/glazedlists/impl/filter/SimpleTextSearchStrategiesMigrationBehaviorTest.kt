package ca.odell.glazedlists.impl.filter

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.regex.PatternSyntaxException

internal class SimpleTextSearchStrategiesMigrationBehaviorTest {
    @Test
    fun singleCharacterStrategyKeepsValidationCaseAndMappingBehavior() {
        val strategy = SingleCharacterCaseInsensitiveTextSearchStrategy()

        assertThrows(IllegalStateException::class.java) { strategy.indexOf("text") }
        assertThrows(IllegalArgumentException::class.java) { strategy.setSubtext(null) }
        assertThrows(IllegalArgumentException::class.java) { strategy.setSubtext("") }
        assertThrows(IllegalArgumentException::class.java) { strategy.setSubtext("ab") }

        strategy.setSubtext("E")
        assertEquals(1, strategy.indexOf("text"))
        assertEquals(1, strategy.indexOf("TEXT"))
        assertEquals(-1, strategy.indexOf("alpha"))

        val characterMap = CharArray('é'.code + 1) { index -> index.toChar() }
        characterMap['é'.code] = 'e'
        strategy.setCharacterMap(characterMap)
        assertEquals(1, strategy.indexOf("résumé"))
    }

    @Test
    fun regularExpressionStrategyKeepsFullMatchAndFailureBehavior() {
        val strategy = RegularExpressionTextSearchStrategy()

        assertThrows(NullPointerException::class.java) { strategy.indexOf("news") }
        assertThrows(NullPointerException::class.java) { strategy.setSubtext(null) }
        assertThrows(PatternSyntaxException::class.java) { strategy.setSubtext("[") }

        strategy.setSubtext("n.*s")
        assertEquals(0, strategy.indexOf("news"))
        assertEquals(-1, strategy.indexOf("evening news"))
        assertEquals(-1, strategy.indexOf("weather"))
    }
}
