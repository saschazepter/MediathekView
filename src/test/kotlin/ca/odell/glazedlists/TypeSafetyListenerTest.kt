package ca.odell.glazedlists

import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

internal class TypeSafetyListenerTest {
    @Test
    fun acceptsConfiguredTypesAndNullWhenExplicitlyAllowed() {
        val source = BasicEventList<Any?>()
        GlazedLists.typeSafetyListener(source, linkedSetOf<Class<*>?>(String::class.java, null))

        assertDoesNotThrow {
            source += "allowed"
            source += null
        }
        assertThrows(IllegalArgumentException::class.java) { source += 1 }
    }

    @Test
    fun rejectsNullWhenItIsNotConfigured() {
        val source = BasicEventList<Any?>()
        GlazedLists.typeSafetyListener(source, setOf<Class<*>>(String::class.java))

        assertThrows(IllegalArgumentException::class.java) { source += null }
    }
}
