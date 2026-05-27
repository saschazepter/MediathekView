package mediathek.tool.cellrenderer

import mediathek.daten.DatenProg
import mediathek.tool.SVGIconUtilities
import org.apache.logging.log4j.LogManager
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
        try {
            val modelColumn = table.convertColumnIndexToModel(column)
            if (modelColumn == DatenProg.PROGRAMM_RESTART || modelColumn == DatenProg.PROGRAMM_DOWNLOADMANAGER) {
                horizontalAlignment = SwingConstants.CENTER
                icon = if (text == true.toString()) checkIcon else null
                text = ""
            }
        } catch (ex: Exception) {
            logger.error("getTableCellRendererComponent", ex)
        }
        return this
    }

    private companion object {
        private val checkIcon = SVGIconUtilities.createSVGIcon("icons/fontawesome/check.svg")
        private val logger = LogManager.getLogger()
    }
}
