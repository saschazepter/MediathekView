package mediathek.swingaudiothek

import com.formdev.flatlaf.themes.FlatMacLightLaf
import mediathek.swingaudiothek.data.AudioRepository
import mediathek.swingaudiothek.ui.MainFrame
import java.awt.EventQueue
import javax.swing.JFrame

fun main() {
    EventQueue.invokeLater {
        runCatching {
            FlatMacLightLaf.setup()
        }

        val frame = MainFrame(AudioRepository())
        frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        frame.isVisible = true
    }
}
