package mediathek.swingaudiothek.ui

import mediathek.swingaudiothek.model.AudioEntry
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import java.awt.Font
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.event.ListSelectionListener
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener
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

    fun setDataColumnWidth(modelIndex: Int, width: Int) {
        val columnDefinition = AudioTableColumn.entries.getOrNull(modelIndex) ?: return
        if (!columnDefinition.toggleable || width <= 0) {
            return
        }
        customColumnWidths[modelIndex] = width
        allColumns[modelIndex]?.let(::applyDataColumnWidth)
        val viewIndex = (0 until columnModel.columnCount)
            .firstOrNull { columnModel.getColumn(it).modelIndex == modelIndex } ?: return
        applyDataColumnWidth(columnModel.getColumn(viewIndex))
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
        val buttonColumn = TableButtonColumn(this, columnDefinition.name, icon, onClick)
        val column = columnModel.getColumn(columnDefinition.modelIndex)
        column.cellRenderer = buttonColumn
        column.cellEditor = buttonColumn
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
}
