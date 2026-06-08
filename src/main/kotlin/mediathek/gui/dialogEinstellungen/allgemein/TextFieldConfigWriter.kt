package mediathek.gui.dialogEinstellungen.allgemein

import mediathek.tool.ApplicationConfiguration
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import javax.swing.JTextField

class TextFieldConfigWriter(
    private val control: JTextField,
    private val configPropertyKey: String,
) : ActionListener {
    override fun actionPerformed(e: ActionEvent) {
        ApplicationConfiguration.getConfiguration().setProperty(configPropertyKey, control.text)
    }
}
