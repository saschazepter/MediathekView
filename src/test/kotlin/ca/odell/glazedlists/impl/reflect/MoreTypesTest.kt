package ca.odell.glazedlists.impl.reflect

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class MoreTypesTest {
    @Test
    fun modernTypeDispatchPreservesHashAndStringRepresentations() {
        val parameterizedType = Fixture::class.java.getDeclaredField("list").genericType
        val genericArrayType = Fixture::class.java.getDeclaredField("array").genericType

        assertEquals(parameterizedType.hashCode(), MoreTypes.hashCode(parameterizedType))
        assertEquals("java.util.List<java.lang.String>", MoreTypes.toString(parameterizedType))
        assertEquals("java.util.List<java.lang.String>[]", MoreTypes.toString(genericArrayType))
        assertEquals(String::class.java.hashCode(), MoreTypes.hashCode(String::class.java))
        assertEquals(0, MoreTypes.hashCode(null))
    }

    private class Fixture {
        lateinit var list: List<String>
        lateinit var array: Array<List<String>>
    }
}
