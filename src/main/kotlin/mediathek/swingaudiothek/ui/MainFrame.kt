package mediathek.swingaudiothek.ui

import mediathek.swingaudiothek.data.AudioRepository
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import javax.swing.JFrame
import javax.swing.JTabbedPane

class MainFrame(
    repository: AudioRepository
) : JFrame("Swing Audiothek") {
    private val audiothekPanel = AudiothekPanel(repository)

    init {
        defaultCloseOperation = DO_NOTHING_ON_CLOSE
        minimumSize = Dimension(1100, 720)
        preferredSize = Dimension(1360, 820)

        val tabbedPane = JTabbedPane()
        tabbedPane.addTab("Audiothek", audiothekPanel)

        layout = BorderLayout()
        add(tabbedPane, BorderLayout.CENTER)

        addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                audiothekPanel.disposePanel()
                dispose()
            }
        })

        pack()
        setLocationRelativeTo(null)
    }
}
