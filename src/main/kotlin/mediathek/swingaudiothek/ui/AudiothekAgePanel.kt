package mediathek.swingaudiothek.ui

import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import java.awt.Cursor
import java.awt.FlowLayout
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.JLabel
import javax.swing.JPanel

class AudiothekAgePanel : JPanel(FlowLayout(FlowLayout.LEFT, 4, 0)) {
    private val ageLabel = JLabel("")
    private val reloadLabel = JLabel()
    private var reloadAction: (() -> Unit)? = null

    init {
        isOpaque = false

        reloadLabel.icon = FontIcon.of(FontAwesomeSolid.DOWNLOAD, 14)
        reloadLabel.toolTipText = "Neu laden"
        reloadLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
        reloadLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                reloadAction?.invoke()
            }
        })

        add(ageLabel)
        add(reloadLabel)
    }

    fun setAge(text: String) {
        ageLabel.text = text
    }

    fun addReloadListener(action: () -> Unit) {
        reloadAction = action
    }
}
