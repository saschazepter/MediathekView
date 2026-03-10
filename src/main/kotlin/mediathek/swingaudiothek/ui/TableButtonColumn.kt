package mediathek.swingaudiothek.ui

import java.awt.Component
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class TableButtonColumn(
    private val table: JTable,
    private val label: String,
    private val icon: Icon? = null,
    private val onClick: (modelRow: Int) -> Unit
) : AbstractCellEditor(), TableCellRenderer, TableCellEditor {
    private val renderButton = JButton(label)
    private val editorButton = JButton(label)
    private val renderPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0))
    private val editorPanel = JPanel(FlowLayout(FlowLayout.CENTER, 0, 0))
    private var currentModelRow = -1

    init {
        renderPanel.add(renderButton)
        editorPanel.add(editorButton)
        renderButton.icon = icon
        editorButton.icon = icon
        if (icon != null) {
            renderButton.text = ""
            editorButton.text = ""
            configureIconButton(renderButton)
            configureIconButton(editorButton)
        }

        editorButton.addActionListener {
            val row = currentModelRow
            fireEditingStopped()
            if (row >= 0) {
                onClick(row)
            }
        }
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        renderButton.text = if (icon == null) value?.toString() ?: label else ""
        if (isSelected) {
            renderPanel.foreground = table.selectionForeground
            renderPanel.background = table.selectionBackground
        } else {
            renderPanel.foreground = table.foreground
            renderPanel.background = table.background
        }
        return renderPanel
    }

    override fun getTableCellEditorComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        row: Int,
        column: Int
    ): Component {
        currentModelRow = table.convertRowIndexToModel(row)
        editorButton.text = if (icon == null) value?.toString() ?: label else ""
        editorPanel.foreground = table.selectionForeground
        editorPanel.background = table.selectionBackground
        return editorPanel
    }

    override fun getCellEditorValue(): Any = label

    private fun configureIconButton(button: JButton) {
        button.isBorderPainted = false
        button.isContentAreaFilled = false
        button.isFocusPainted = false
        button.isOpaque = false
    }
}
