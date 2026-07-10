package mediathek.gui.dialogEinstellungen

import mediathek.tool.ReplaceEntry
import mediathek.tool.ReplacementRules
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*
import javax.swing.JButton
import javax.swing.SwingUtilities

internal class PanelDateinamenTest {
    @Test
    fun plusButtonKeepsNewReplacementRuleWhenAnotherRuleWasSelected() {
        val rules = ReplacementRules()
        rules.initDefaults()

        onEdt {
            val panel = PanelDateinamen(rules) { Optional.of(ReplaceEntry("alpha", "beta")) }
            plusButton(panel).doClick()
        }

        assertEquals(listOf(" ", "alpha"), rules.entries().map { it.from })
        assertEquals(listOf("_", "beta"), rules.entries().map { it.to })
    }

    @Test
    fun plusButtonDoesNotChangeRulesWhenDialogIsCancelled() {
        val rules = ReplacementRules()
        rules.initDefaults()

        onEdt {
            val panel = PanelDateinamen(rules) { Optional.empty() }
            plusButton(panel).doClick()
        }

        assertEquals(listOf(ReplaceEntry(" ", "_")), rules.entries())
    }

    private fun plusButton(panel: PanelDateinamen): JButton {
        val field = PanelDateinamen::class.java.getDeclaredField("jButtonPlus")
        field.isAccessible = true
        return field.get(panel) as JButton
    }

    private fun onEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
