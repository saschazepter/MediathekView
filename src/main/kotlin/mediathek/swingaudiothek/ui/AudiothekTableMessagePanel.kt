package mediathek.swingaudiothek.ui

import java.awt.BorderLayout
import java.awt.Font
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants

class AudiothekTableMessagePanel : JPanel(BorderLayout()) {
    private val messageLabel = JLabel("", SwingConstants.CENTER)

    init {
        messageLabel.font = messageLabel.font.deriveFont(Font.BOLD, 16f)
        add(messageLabel, BorderLayout.CENTER)
    }

    fun setMessage(text: String) {
        messageLabel.text = text
    }
}
