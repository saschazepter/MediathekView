package mediathek.gui.actions

import mediathek.config.Daten
import mediathek.gui.abo.ManageAboDialog
import mediathek.swing.IconUtils
import org.kordamp.ikonli.materialdesign2.MaterialDesignD
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.JFrame

class ManageAboAction(
    private val parent: JFrame,
    private val daten: Daten,
) : AbstractAction() {
    private var dialog: ManageAboDialog? = null

    fun closeDialog() {
        dialog?.dispose()
    }

    override fun actionPerformed(e: ActionEvent?) {
        dialog = ManageAboDialog(parent, daten)
        dialog!!.isVisible = true
        dialog = null
    }

    init {
        putValue(NAME, "Abos verwalten...")
        putValue(SMALL_ICON, IconUtils.windowBarSpecificToolbarIcon(MaterialDesignD.DATABASE))
        putValue(SHORT_DESCRIPTION, "Abos verwalten")
    }
}
