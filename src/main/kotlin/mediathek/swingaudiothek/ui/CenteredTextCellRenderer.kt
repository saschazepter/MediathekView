package mediathek.swingaudiothek.ui

import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

class CenteredTextCellRenderer : DefaultTableCellRenderer() {
    init {
        horizontalAlignment = SwingConstants.CENTER
    }
}
