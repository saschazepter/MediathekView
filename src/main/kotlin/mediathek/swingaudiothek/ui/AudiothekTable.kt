/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.swingaudiothek.ui

import mediathek.swingaudiothek.model.AudioEntry
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener
import javax.swing.table.TableCellRenderer
import javax.swing.table.TableColumn

class AudiothekTable(
    private val onOpenAudio: (AudioEntry) -> Unit,
    private val onDownload: (AudioEntry) -> Unit
) : JTable(AudioTableModel()) {
    private val audioTableModel = model as AudioTableModel
    private val sorter = TriStateTableRowSorter(audioTableModel)
    private val allColumns = linkedMapOf<Int, TableColumn>()
    private val lastKnownViewIndexes = mutableMapOf<Int, Int>()
    private val centeredTextCellRenderer = CenteredTextCellRenderer()
    private val buttonHeaderRenderer = TableCellRenderer { table, _, _, _, _, column ->
        val component = table.tableHeader.defaultRenderer
            .getTableCellRendererComponent(table, "", false, false, -1, column)
        if (component is JLabel) {
            component.text = ""
            component.icon = null
            component.disabledIcon = null
            component.horizontalAlignment = SwingConstants.CENTER
        }
        component
    }
    private val luceneIndex = AudiothekLuceneIndex()
    private val customColumnWidths = buildMap {
        AudioTableColumn.searchableColumns.forEach { definition ->
            definition.preferredWidth?.let { put(definition.modelIndex, it) }
        }
    }.toMutableMap()
    private var allEntries: List<AudioEntry> = emptyList()
    private var currentFilterQuery = ""

    init {
        autoCreateRowSorter = false
        rowSorter = sorter
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION)
        fillsViewportHeight = true
        rowHeight = 24
        tableHeader.font = tableHeader.font.deriveFont(Font.BOLD)

        configureSorting()
        configureColumns()
        installHeaderPopup()
        installRowPopup()
        installColumnTracking()
    }

    fun setRows(entries: List<AudioEntry>) {
        allEntries = entries
        luceneIndex.replaceEntries(entries)
        applyFilter(currentFilterQuery)
    }

    fun selectedEntry(): AudioEntry? {
        val viewRow = selectedRow
        if (viewRow < 0) {
            return null
        }
        return audioTableModel.getEntry(convertRowIndexToModel(viewRow))
    }

    fun addEntrySelectionListener(listener: ListSelectionListener) {
        selectionModel.addListSelectionListener(listener)
    }

    fun applyFilter(query: String) {
        currentFilterQuery = query
        val normalized = query.trim()
        if (normalized.isEmpty()) {
            audioTableModel.setRows(allEntries)
            return
        }
        audioTableModel.setRows(luceneIndex.search(normalized, visibleSearchFields()))
    }

    fun selectFirstRow() {
        if (rowCount > 0) {
            setRowSelectionInterval(0, 0)
        }
    }

    private fun configureSorting() {
        sorter.setSortable(AudioTableColumn.PLAY.modelIndex, false)
        sorter.setSortable(AudioTableColumn.DOWNLOAD.modelIndex, false)
        sorter.setComparator(AudioTableColumn.DATE.modelIndex) { left, right ->
            compareNullable(parseDate(left), parseDate(right))
        }
        sorter.setComparator(AudioTableColumn.TIME.modelIndex) { left, right ->
            compareNullable(parseTime(left), parseTime(right))
        }
        sorter.setComparator(AudioTableColumn.DURATION.modelIndex) { left, right ->
            compareNullable(parseInt(left), parseInt(right))
        }
        sorter.setComparator(AudioTableColumn.SIZE.modelIndex) { left, right ->
            compareNullable(parseInt(left), parseInt(right))
        }
    }

    private fun configureColumns() {
        for (index in 0 until columnModel.columnCount) {
            val column = columnModel.getColumn(index)
            allColumns[column.modelIndex] = column
            val definition = AudioTableColumn.entries[column.modelIndex]
            if (definition.toggleable) {
                lastKnownViewIndexes[column.modelIndex] = index
            }
        }

        AudioTableColumn.entries.forEach { definition ->
            val column = columnModel.getColumn(definition.modelIndex)
            if (definition.centered) {
                column.cellRenderer = centeredTextCellRenderer
            }
            applyDataColumnWidth(column)
        }

        configureButtonColumn(AudioTableColumn.PLAY, FontIcon.of(FontAwesomeSolid.PLAY, 14)) { modelRow ->
            audioTableModel.getEntry(modelRow)?.let(onOpenAudio)
        }
        configureButtonColumn(AudioTableColumn.DOWNLOAD, FontIcon.of(FontAwesomeSolid.CLOUD_DOWNLOAD_ALT, 14)) { modelRow ->
            audioTableModel.getEntry(modelRow)?.let(onDownload)
        }
    }

    private fun configureButtonColumn(columnDefinition: AudioTableColumn, icon: Icon? = null, onClick: (Int) -> Unit) {
        val buttonColumn = TableButtonColumn(columnDefinition.name, icon, onClick)
        val column = columnModel.getColumn(columnDefinition.modelIndex)
        column.cellRenderer = buttonColumn
        column.cellEditor = buttonColumn
        column.headerRenderer = buttonHeaderRenderer
        val width = columnDefinition.preferredWidth ?: if (icon == null) 52 else 32
        applyFixedWidth(column, width)
    }

    private fun applyDataColumnWidth(column: TableColumn) {
        val width = customColumnWidths[column.modelIndex]
            ?: AudioTableColumn.entries[column.modelIndex].preferredWidth
            ?: return
        column.preferredWidth = width
    }

    private fun applyFixedWidth(column: TableColumn, width: Int) {
        column.minWidth = width
        column.maxWidth = width
        column.preferredWidth = width
    }

    private fun installHeaderPopup() {
        tableHeader.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) = maybeShowHeaderPopup(event)
            override fun mouseReleased(event: MouseEvent) = maybeShowHeaderPopup(event)
        })
    }

    private fun installRowPopup() {
        addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) = maybeOpenRow(event)
            override fun mousePressed(event: MouseEvent) = maybeShowRowPopup(event)
            override fun mouseReleased(event: MouseEvent) = maybeShowRowPopup(event)
        })
    }

    private fun installColumnTracking() {
        columnModel.addColumnModelListener(object : TableColumnModelListener {
            override fun columnAdded(e: TableColumnModelEvent?) = Unit
            override fun columnRemoved(e: TableColumnModelEvent?) = Unit
            override fun columnSelectionChanged(e: javax.swing.event.ListSelectionEvent?) = Unit
            override fun columnMarginChanged(e: javax.swing.event.ChangeEvent?) = Unit

            override fun columnMoved(e: TableColumnModelEvent?) {
                if (e != null && e.fromIndex != e.toIndex) {
                    updateVisibleColumnPositions()
                }
            }
        })
    }

    private fun maybeShowHeaderPopup(event: MouseEvent) {
        if (!event.isPopupTrigger) {
            return
        }

        val popup = JPopupMenu()
        for (definition in AudioTableColumn.searchableColumns) {
            val modelIndex = definition.modelIndex
            val item = JCheckBoxMenuItem(definition.title, isColumnVisible(modelIndex))
            item.addActionListener { toggleColumn(modelIndex, item.isSelected) }
            popup.add(item)
        }
        popup.show(event.component, event.x, event.y)
    }

    private fun maybeShowRowPopup(event: MouseEvent) {
        if (!event.isPopupTrigger) {
            return
        }

        val viewRow = rowAtPoint(event.point)
        if (viewRow < 0) {
            return
        }

        setRowSelectionInterval(viewRow, viewRow)
        val modelRow = convertRowIndexToModel(viewRow)
        val entry = audioTableModel.getEntry(modelRow) ?: return

        JPopupMenu().apply {
            add(createRowActionItem("Abspielen", FontAwesomeSolid.PLAY, entry, onOpenAudio))
            add(createRowActionItem("Download", FontAwesomeSolid.CLOUD_DOWNLOAD_ALT, entry, onDownload))
            show(event.component, event.x, event.y)
        }
    }

    private fun maybeOpenRow(event: MouseEvent) {
        if (!SwingUtilities.isLeftMouseButton(event) || event.clickCount != 2) {
            return
        }

        val viewRow = rowAtPoint(event.point)
        if (viewRow < 0) {
            return
        }

        setRowSelectionInterval(viewRow, viewRow)
        selectedEntry()?.let(onOpenAudio)
    }

    private fun createRowActionItem(
        title: String,
        iconLiteral: FontAwesomeSolid,
        entry: AudioEntry,
        action: (AudioEntry) -> Unit
    ): JMenuItem {
        return JMenuItem(title, FontIcon.of(iconLiteral, 14)).apply {
            addActionListener { action(entry) }
        }
    }

    private fun isColumnVisible(modelIndex: Int): Boolean {
        return (0 until columnModel.columnCount).any { columnModel.getColumn(it).modelIndex == modelIndex }
    }

    private fun toggleColumn(modelIndex: Int, visible: Boolean) {
        val definition = AudioTableColumn.entries.getOrNull(modelIndex) ?: return
        if (!definition.toggleable) {
            return
        }
        val currentlyVisible = isColumnVisible(modelIndex)
        if (visible == currentlyVisible) {
            return
        }

        if (visible) {
            val column = allColumns[modelIndex] ?: return
            columnModel.addColumn(column)
            applyDataColumnWidth(column)
            val newViewIndex = columnModel.columnCount - 1
            val targetViewIndex = lastKnownViewIndexes[modelIndex]
                ?.coerceIn(0, newViewIndex)
                ?: newViewIndex
            if (targetViewIndex in 0..newViewIndex && targetViewIndex != newViewIndex) {
                columnModel.moveColumn(newViewIndex, targetViewIndex)
            }
        } else {
            val viewIndex = (0 until columnModel.columnCount)
                .firstOrNull { columnModel.getColumn(it).modelIndex == modelIndex } ?: return
            lastKnownViewIndexes[modelIndex] = viewIndex
            columnModel.removeColumn(columnModel.getColumn(viewIndex))
        }
        updateVisibleColumnPositions()
        applyFilter(currentFilterQuery)
    }

    private fun updateVisibleColumnPositions() {
        for (viewIndex in 0 until columnModel.columnCount) {
            val modelIndex = columnModel.getColumn(viewIndex).modelIndex
            if (AudioTableColumn.entries[modelIndex].toggleable) {
                lastKnownViewIndexes[modelIndex] = viewIndex
            }
        }
    }

    private fun visibleSearchFields(): List<String> {
        return (0 until columnModel.columnCount)
            .map { columnModel.getColumn(it).modelIndex }
            .mapNotNull { AudioTableColumn.entries[it].searchField }
    }

    private fun parseDate(value: Any?): LocalDate? =
        value?.toString()
            ?.takeIf(String::isNotBlank)
            ?.let { LocalDate.parse(it, DATE_FORMAT) }

    private fun parseTime(value: Any?): LocalTime? =
        value?.toString()
            ?.takeIf(String::isNotBlank)
            ?.let { LocalTime.parse(it, TIME_FORMAT) }

    private fun parseInt(value: Any?): Int? =
        value?.toString()
            ?.takeIf(String::isNotBlank)
            ?.toIntOrNull()

    private fun <T : Comparable<T>> compareNullable(left: T?, right: T?): Int = when {
        left == null && right == null -> 0
        left == null -> 1
        right == null -> -1
        else -> left.compareTo(right)
    }

    companion object {
        private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        private val TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
