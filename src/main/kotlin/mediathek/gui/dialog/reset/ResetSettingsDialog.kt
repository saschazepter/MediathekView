package mediathek.gui.dialog.reset

import mediathek.gui.dialog.StandardCloseDialog
import java.awt.Frame
import javax.swing.JComponent

class ResetSettingsDialog(owner: Frame?) : StandardCloseDialog(owner, "Programm zurücksetzen", true) {
    init {
        isResizable = false
    }

    override fun createContentPanel(): JComponent = ResetSettingsPanel(null)
}
