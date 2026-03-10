package mediathek.swingaudiothek.ui

import mediathek.swingaudiothek.model.AudioEntry
import javax.swing.table.AbstractTableModel

class AudioTableModel : AbstractTableModel() {
    private val rows = mutableListOf<AudioEntry>()

    fun setRows(entries: List<AudioEntry>) {
        rows.clear()
        rows.addAll(entries)
        fireTableDataChanged()
    }

    fun getEntry(rowIndex: Int): AudioEntry? = rows.getOrNull(rowIndex)

    override fun getRowCount(): Int = rows.size

    override fun getColumnCount(): Int = AudioTableColumn.entries.size

    override fun getColumnName(column: Int): String = AudioTableColumn.entries[column].title

    override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean =
        AudioTableColumn.entries[columnIndex].editable

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val entry = rows[rowIndex]
        return AudioTableColumn.entries[columnIndex].valueProvider(entry)
    }
}
