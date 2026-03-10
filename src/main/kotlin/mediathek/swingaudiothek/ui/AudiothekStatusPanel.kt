package mediathek.swingaudiothek.ui

import org.jdesktop.swingx.JXStatusBar
import javax.swing.JLabel
import javax.swing.SwingConstants

class AudiothekStatusPanel : JXStatusBar() {
    private val sourceLabel = JLabel("Stand: -")
    private val agePanel = AudiothekAgePanel()
    private val countLabel = JLabel("0 Einträge", SwingConstants.RIGHT)

    init {
        add(sourceLabel, Constraint(Constraint.ResizeBehavior.FILL))
        add(agePanel)
        add(countLabel)
    }

    fun setStand(text: String) {
        sourceLabel.text = text
    }

    fun setAge(text: String) {
        agePanel.setAge(text)
    }

    fun addReloadListener(action: () -> Unit) {
        agePanel.addReloadListener(action)
    }

    fun setReloadEnabled(enabled: Boolean) {
        agePanel.setReloadEnabled(enabled)
    }

    fun setCount(text: String) {
        countLabel.text = text
    }
}
