package mediathek.gui.actions

import mediathek.config.Daten
import mediathek.gui.statistics.FilmStatisticsDialog
import java.awt.Frame
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

class ShowFilmStatisticsAction(
    private val owner: Frame,
    private val daten: Daten,
) : AbstractAction() {
    init {
        putValue(NAME, "Filmlisten-Statistik anzeigen...")
    }

    override fun actionPerformed(event: ActionEvent?) {
        FilmStatisticsDialog(owner, daten, this).isVisible = true
    }
}
