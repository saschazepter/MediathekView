package mediathek.swingaudiothek.ui

import javax.swing.RowSorter
import javax.swing.SortOrder
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter

class TriStateTableRowSorter<M : TableModel>(model: M) : TableRowSorter<M>(model) {
    override fun toggleSortOrder(column: Int) {
        val primaryKey = sortKeys.firstOrNull()
        if (primaryKey?.column == column && primaryKey.sortOrder == SortOrder.DESCENDING) {
            sortKeys = emptyList()
            return
        }

        val nextOrder = if (primaryKey?.column == column && primaryKey.sortOrder == SortOrder.ASCENDING) {
            SortOrder.DESCENDING
        } else {
            SortOrder.ASCENDING
        }
        sortKeys = listOf(RowSorter.SortKey(column, nextOrder))
    }
}
