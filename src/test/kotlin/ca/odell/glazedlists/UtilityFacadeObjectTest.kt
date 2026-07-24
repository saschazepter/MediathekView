package ca.odell.glazedlists

import ca.odell.glazedlists.impl.GlazedListsImpl
import ca.odell.glazedlists.impl.SortIconFactory
import ca.odell.glazedlists.impl.filter.TextMatchers
import ca.odell.glazedlists.matchers.Matchers
import ca.odell.glazedlists.swing.GlazedListsSwing
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

internal class UtilityFacadeObjectTest {
    @Test
    fun utilityFacadesAreKotlinObjectsWithoutCompanionShells() {
        val facadeTypes = listOf(
            GlazedLists::class.java,
            GlazedListsSwing::class.java,
            Matchers::class.java,
            Sequencers::class.java,
            GlazedListsImpl::class.java,
            SortIconFactory::class.java,
            TextMatchers::class.java,
        )

        facadeTypes.forEach { facadeType ->
            val instanceField = facadeType.getDeclaredField("INSTANCE")

            assertTrue(Modifier.isFinal(facadeType.modifiers), facadeType.name)
            assertTrue(Modifier.isPublic(instanceField.modifiers), facadeType.name)
            assertTrue(Modifier.isStatic(instanceField.modifiers), facadeType.name)
            assertTrue(Modifier.isFinal(instanceField.modifiers), facadeType.name)
            assertNotNull(instanceField.get(null), facadeType.name)
            assertFalse(
                facadeType.declaredFields.any { it.name == "Companion" },
                facadeType.name,
            )
        }
    }
}
