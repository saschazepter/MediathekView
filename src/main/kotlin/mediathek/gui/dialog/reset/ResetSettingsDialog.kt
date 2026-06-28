package mediathek.gui.dialog.reset

import mediathek.config.Daten
import mediathek.gui.dialog.StandardCloseDialog
import mediathek.mainwindow.SettingsResetHost
import javax.swing.JComponent

class ResetSettingsDialog(
    private val host: SettingsResetHost,
    private val daten: Daten,
) : StandardCloseDialog(host.ownerFrame(), "Programm zurücksetzen", true) {
    init {
        isResizable = false
    }

    override fun createContentPanel(): JComponent = ResetSettingsPanel(host, daten)
}
