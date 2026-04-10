package mediathek.gui.tabs.tab_downloads

import mediathek.swing.IconUtils
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JLabel
import javax.swing.JToolBar

class DownloadsDisplayFilterToolBar : JToolBar() {
    val displayCategoriesComboBox = JComboBox<String>()
    val viewComboBox = JComboBox<String>()
    val clearButton = JButton()

    init {
        isFloatable = true
        name = "Anzeige"

        displayCategoriesComboBox.putClientProperty("JComponent.roundRect", true)
        displayCategoriesComboBox.prototypeDisplayValue = "nur Downloads"
        displayCategoriesComboBox.maximumSize = displayCategoriesComboBox.preferredSize

        viewComboBox.putClientProperty("JComponent.roundRect", true)
        viewComboBox.prototypeDisplayValue = "nur abgeschlossene"
        viewComboBox.maximumSize = viewComboBox.preferredSize

        clearButton.icon = IconUtils.of(FontAwesomeSolid.BROOM)
        clearButton.toolTipText = "Filter zurücksetzen"

        add(JLabel("Typ:"))
        add(displayCategoriesComboBox)
        addSeparator()
        add(JLabel("Status:"))
        add(viewComboBox)
        addSeparator()
        add(clearButton)
    }
}
