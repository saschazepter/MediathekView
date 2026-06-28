package mediathek.gui.actions

import mediathek.config.Daten
import mediathek.gui.duplicates.statistics.DuplicateStatisticsDialog
import java.awt.Frame
import java.awt.event.ActionEvent
import javax.swing.AbstractAction

class ShowDuplicateStatisticsAction(
    private val owner: Frame,
    private val daten: Daten,
) : AbstractAction() {
    init {
        putValue(NAME, "Film-Statistik anzeigen")
    }

    override fun actionPerformed(event: ActionEvent?) {
        DuplicateStatisticsDialog(owner, daten, this).isVisible = true
    }
}
