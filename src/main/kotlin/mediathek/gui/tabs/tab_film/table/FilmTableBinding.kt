/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package mediathek.gui.tabs.tab_film.table

import ca.odell.glazedlists.*
import ca.odell.glazedlists.gui.AbstractTableComparatorChooser
import ca.odell.glazedlists.gui.AdvancedTableFormat
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.swing.AdvancedTableModel
import ca.odell.glazedlists.swing.TableComparatorChooser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import kotlinx.coroutines.withContext
import mediathek.daten.DatenFilm
import mediathek.tool.models.FilmColumn
import mediathek.swing.table.GlazedSortKeysPersister
import mediathek.tool.withReadLock
import java.util.Collections
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicLong
import javax.swing.JTable
import javax.swing.ListSelectionModel
import javax.swing.DefaultListSelectionModel
import javax.swing.SwingUtilities
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableModel

interface FilmTableModelBinding {
    val table: JTable
    val rowCount: Int

    fun filmAtViewRow(viewRow: Int): DatenFilm?
    fun selectedFilms(): List<DatenFilm>
    suspend fun replaceFilms(films: Collection<DatenFilm>)
    fun removeFilms(films: Collection<DatenFilm>): Boolean
    fun rowsChanged(films: Collection<DatenFilm>)
    fun restoreLegacySort(column: Int, descending: Boolean)
    fun clearSorting()
    fun saveState()
    fun dispose()
}

@OptIn(ExperimentalCoroutinesApi::class)
class FilmTableBinding(
    override val table: JTable,
) : FilmTableModelBinding {
    private val sortControlSource = BasicEventList<DatenFilm>()
    private val sortedFilms = SortedList(sortControlSource, null)
    private val tableFormat = FilmTableFormat()
    private val tableModel = SnapshotFilmTableModel(tableFormat)
    private val selectionModel = DefaultListSelectionModel()
    private val comparatorChooser: TableComparatorChooser<DatenFilm>
    private val sortPersister: GlazedSortKeysPersister<DatenFilm>
    private val modelDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val modelScope = CoroutineScope(SupervisorJob() + modelDispatcher)
    private val updateGeneration = AtomicLong()
    private var sourceFilms: List<DatenFilm> = emptyList()
    private val excludedFilms = Collections.newSetFromMap(IdentityHashMap<DatenFilm, Boolean>())

    @Volatile
    private var disposed = false

    init {
        table.autoCreateRowSorter = false
        table.rowSorter = null
        table.model = tableModel
        selectionModel.selectionMode = ListSelectionModel.MULTIPLE_INTERVAL_SELECTION
        table.selectionModel = selectionModel
        comparatorChooser = TableComparatorChooser.install(
            table,
            sortedFilms,
            AbstractTableComparatorChooser.SINGLE_COLUMN,
            tableFormat,
        )
        NON_SORTABLE_COLUMNS.forEach { column ->
            comparatorChooser.getComparatorsForColumn(column.index).clear()
        }
        sortPersister = GlazedSortKeysPersister(SORT_CONFIG_PREFIX, comparatorChooser)
        sortPersister.restoreSortState()
        comparatorChooser.addSortActionListener {
            sortPersister.saveSortState()
            scheduleResort()
        }
    }

    override val rowCount: Int
        get() = tableModel.rowCount

    override fun filmAtViewRow(viewRow: Int): DatenFilm? =
        viewRow.takeIf { it in 0 until tableModel.rowCount }?.let(tableModel::getElementAt)

    override fun selectedFilms(): List<DatenFilm> = table.selectedRows
        .asSequence()
        .mapNotNull(::filmAtViewRow)
        .toList()

    override suspend fun replaceFilms(films: Collection<DatenFilm>) {
        if (disposed) {
            return
        }

        val replacement = films as? List<DatenFilm> ?: films.toList()
        val selection = withContext(Dispatchers.Swing) { captureSelection() }
        val requestedGeneration = updateGeneration.incrementAndGet()
        var selectionRows = IntArray(0)
        var displayedFilms: List<DatenFilm> = emptyList()
        withContext(modelDispatcher) {
            if (requestedGeneration != updateGeneration.get()) {
                return@withContext
            }
            excludedFilms.clear()
            sourceFilms = replacement
            displayedFilms = prepareDisplayedFilms()
            if (disposed || requestedGeneration != updateGeneration.get()) {
                return@withContext
            }
            selectionRows = findSelectedRows(displayedFilms, selection)
        }
        withContext(Dispatchers.Swing) {
            if (!disposed && requestedGeneration == updateGeneration.get()) {
                tableModel.replaceElements(displayedFilms)
                restoreSelection(selection, selectionRows)
            }
        }
    }

    override fun removeFilms(films: Collection<DatenFilm>): Boolean {
        if (disposed || films.isEmpty()) {
            return false
        }

        val filmsToRemove = films.toList()
        val selection = captureSelection()
        val requestedGeneration = updateGeneration.incrementAndGet()
        modelScope.launch {
            excludedFilms.addAll(filmsToRemove)
            val displayedFilms = prepareDisplayedFilms()
            if (disposed || requestedGeneration != updateGeneration.get()) {
                return@launch
            }
            val selectionRows = findSelectedRows(displayedFilms, selection)
            withContext(Dispatchers.Swing) {
                if (!disposed && requestedGeneration == updateGeneration.get()) {
                    tableModel.replaceElements(displayedFilms)
                    restoreSelection(selection, selectionRows)
                }
            }
        }
        return true
    }

    override fun rowsChanged(films: Collection<DatenFilm>) {
        if (disposed || films.isEmpty()) {
            return
        }

        table.repaint()
    }

    override fun restoreLegacySort(column: Int, descending: Boolean) {
        if (column !in FilmColumn.entries.indices || FilmColumn.fromIndex(column) in NON_SORTABLE_COLUMNS) {
            return
        }
        comparatorChooser.clearComparator()
        comparatorChooser.appendComparator(column, 0, descending)
        sortPersister.saveSortState()
    }

    override fun clearSorting() {
        comparatorChooser.clearComparator()
        sortPersister.saveSortState()
    }

    override fun saveState() {
        if (!disposed) {
            sortPersister.saveSortState()
        }
    }

    override fun dispose() {
        if (disposed) {
            return
        }
        disposed = true
        updateGeneration.incrementAndGet()
        modelScope.cancel()
        runOnEdtAndWait {
            comparatorChooser.dispose()
            table.clearSelection()
            table.selectionModel = DefaultListSelectionModel()
            table.rowSorter = null
            table.model = DefaultTableModel()
            tableModel.dispose()
        }
        // A cancelled background preparation may still be unwinding, so comparator state is left intact for GC.
    }

    private fun captureSelection(): SelectionSnapshot = SelectionSnapshot(
        films = selectedFilms(),
        anchorRow = table.selectionModel.anchorSelectionIndex.takeIf { it >= 0 } ?: table.selectedRow,
    )

    private fun restoreSelection(snapshot: SelectionSnapshot, selectedRows: IntArray) {
        table.clearSelection()
        var firstSelectedRow = -1
        selectionModel.valueIsAdjusting = true
        try {
            for (row in selectedRows) {
                if (row in 0 until tableModel.rowCount) {
                    table.addRowSelectionInterval(row, row)
                    if (firstSelectedRow == -1) {
                        firstSelectedRow = row
                    }
                }
            }
            if (firstSelectedRow == -1 && tableModel.rowCount > 0) {
                firstSelectedRow = snapshot.anchorRow.coerceAtLeast(0).coerceAtMost(tableModel.rowCount - 1)
                table.selectionModel.setSelectionInterval(firstSelectedRow, firstSelectedRow)
            }
        } finally {
            selectionModel.valueIsAdjusting = false
        }

        if (firstSelectedRow >= 0) {
            table.scrollRectToVisible(table.getCellRect(firstSelectedRow, 0, true))
            table.requestFocusInWindow()
        }
    }

    private fun scheduleResort() {
        if (disposed) {
            return
        }
        val selection = captureSelection()
        val requestedGeneration = updateGeneration.incrementAndGet()
        modelScope.launch {
            val displayedFilms = prepareDisplayedFilms()
            if (disposed || requestedGeneration != updateGeneration.get()) {
                return@launch
            }
            val selectionRows = findSelectedRows(displayedFilms, selection)
            withContext(Dispatchers.Swing) {
                if (!disposed && requestedGeneration == updateGeneration.get()) {
                    tableModel.replaceElements(displayedFilms)
                    restoreSelection(selection, selectionRows)
                }
            }
        }
    }

    private fun prepareDisplayedFilms(): List<DatenFilm> {
        val comparator = sortedFilms.withReadLock { sortedFilms.comparator }
        if (excludedFilms.isEmpty() && comparator == null) {
            return sourceFilms
        }

        val result = if (excludedFilms.isEmpty()) {
            ArrayList(sourceFilms)
        } else {
            sourceFilms.filterTo(ArrayList(sourceFilms.size)) { it !in excludedFilms }
        }
        if (comparator != null) {
            result.sortWith(comparator)
        }
        return result
    }

    private fun findSelectedRows(films: List<DatenFilm>, selection: SelectionSnapshot): IntArray {
        if (selection.films.isEmpty()) {
            return IntArray(0)
        }

        val selectedReferences = Collections.newSetFromMap(IdentityHashMap<DatenFilm, Boolean>()).apply {
            addAll(selection.films)
        }
        val matchedReferences = Collections.newSetFromMap(IdentityHashMap<DatenFilm, Boolean>())
        val selectedRows = ArrayList<Int>(selectedReferences.size)
        films.forEachIndexed { index, film ->
            if (film in selectedReferences) {
                selectedRows.add(index)
                matchedReferences.add(film)
            }
        }

        if (matchedReferences.size == selectedReferences.size) {
            return selectedRows.toIntArray()
        }

        val missingIdentitiesByUrl = selection.films
            .asSequence()
            .filterNot { it in matchedReferences }
            .map(DatenFilm::filmIdentity)
            .groupBy(DatenFilm.FilmIdentity::normalQualityUrl)
        films.forEachIndexed { index, film ->
            if (film in matchedReferences) {
                return@forEachIndexed
            }
            val candidates = missingIdentitiesByUrl[film.urlNormalQuality] ?: return@forEachIndexed
            if (candidates.any { identity -> film.matches(identity) }) {
                selectedRows.add(index)
            }
        }
        selectedRows.sort()
        return selectedRows.toIntArray()
    }

    private fun DatenFilm.matches(identity: DatenFilm.FilmIdentity): Boolean =
        sender == identity.sender &&
                thema == identity.thema &&
                urlNormalQuality == identity.normalQualityUrl &&
                websiteUrl == identity.websiteUrl

    private fun runOnEdtAndWait(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }

    private data class SelectionSnapshot(
        val films: List<DatenFilm>,
        val anchorRow: Int,
    )

    private class SnapshotFilmTableModel(
        private var format: AdvancedTableFormat<DatenFilm>,
    ) : AbstractTableModel(), AdvancedTableModel<DatenFilm> {
        private var elements: List<DatenFilm> = emptyList()

        override fun getTableFormat(): TableFormat<in DatenFilm> = format

        override fun setTableFormat(tableFormat: TableFormat<in DatenFilm>) {
            require(tableFormat is AdvancedTableFormat<*>)
            @Suppress("UNCHECKED_CAST")
            format = tableFormat as AdvancedTableFormat<DatenFilm>
            fireTableStructureChanged()
        }

        override fun getElementAt(index: Int): DatenFilm = elements[index]

        override fun getRowCount(): Int = elements.size

        override fun getColumnCount(): Int = format.columnCount

        override fun getColumnName(column: Int): String = format.getColumnName(column)

        override fun getColumnClass(columnIndex: Int): Class<*> = format.getColumnClass(columnIndex)

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? =
            format.getColumnValue(elements[rowIndex], columnIndex)

        fun replaceElements(replacement: List<DatenFilm>) {
            check(SwingUtilities.isEventDispatchThread())
            elements = replacement
            fireTableDataChanged()
        }

        override fun dispose() {
            elements = emptyList()
        }
    }

    private companion object {
        private const val SORT_CONFIG_PREFIX = "film"
        private val NON_SORTABLE_COLUMNS = setOf(
            FilmColumn.PLAY,
            FilmColumn.SAVE,
            FilmColumn.BOOKMARK,
            FilmColumn.GEO,
        )
    }
}
