package mediathek.gui.tabs.tab_downloads

import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JToolBar

class DownloadsDisplayFilterToolBar : JToolBar() {
    val displayCategoriesComboBox = JComboBox<String>()
    val viewComboBox = JComboBox<String>()

    init {
        isFloatable = true
        name = "Anzeige"

        displayCategoriesComboBox.putClientProperty("JComponent.roundRect", true)
        displayCategoriesComboBox.prototypeDisplayValue = "nur Downloads"
        displayCategoriesComboBox.maximumSize = displayCategoriesComboBox.preferredSize

        viewComboBox.putClientProperty("JComponent.roundRect", true)
        viewComboBox.prototypeDisplayValue = "nur abgeschlossene"
        viewComboBox.maximumSize = viewComboBox.preferredSize

        add(JLabel("Typ:"))
        add(displayCategoriesComboBox)
        addSeparator()
        add(JLabel("Status:"))
        add(viewComboBox)
    }
}
