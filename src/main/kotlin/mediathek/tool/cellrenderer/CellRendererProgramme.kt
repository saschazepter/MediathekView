package mediathek.tool.cellrenderer

import mediathek.tool.SVGIconUtilities
import java.awt.Component
import javax.swing.JTable
import javax.swing.SwingConstants
import javax.swing.table.DefaultTableCellRenderer

class CellRendererProgramme : DefaultTableCellRenderer() {
    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        icon = null
        super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)
        horizontalAlignment = SwingConstants.CENTER
        icon = if (text == true.toString()) checkIcon else null
        text = ""
        return this
    }

    private companion object {
        private val checkIcon = SVGIconUtilities.createSVGIcon("icons/fontawesome/check.svg")
    }
}
