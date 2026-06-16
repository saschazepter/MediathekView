package mediathek.gui.dialogEinstellungen.blacklist

import mediathek.daten.DatenFilm
import mediathek.daten.blacklist.BlacklistRule
import mediathek.daten.blacklist.ListeBlacklist
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BlacklistRuleTableModelTest {
    @Test
    fun firstColumnShowsActiveStateAsBoolean() {
        val blacklist = ListeBlacklist()
        blacklist.addWithoutNotification(BlacklistRule("ARD", active = false))
        val model = BlacklistRuleTableModel(blacklist)

        assertEquals("aktiv", model.getColumnName(0))
        assertEquals(Boolean::class.javaObjectType, model.getColumnClass(0))
        assertEquals(false, model.getValueAt(0, 0))
    }

    @Test
    fun potentialCountsIncludeInactiveRules() {
        val blacklist = ListeBlacklist()
        blacklist.addWithoutNotification(BlacklistRule("ARD", titel = "tagesschau", active = false))
        val model = BlacklistRuleTableModel(blacklist)

        model.applyFilteredCounts(
            model.calculateFilteredCounts(
                listOf(film(sender = "ARD", title = "Tagesschau um acht"))
            )
        )

        assertEquals(1, model.getValueAt(0, 5))
    }

    private fun film(sender: String, title: String): DatenFilm =
        DatenFilm().apply {
            this.sender = sender
            this.title = title
        }
}
