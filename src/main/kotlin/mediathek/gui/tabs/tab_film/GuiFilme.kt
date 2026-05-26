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

package mediathek.gui.tabs.tab_film

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import mediathek.config.Daten
import mediathek.gui.actions.DeleteBookmarksAction
import mediathek.gui.actions.PlayFilmAction
import mediathek.gui.bookmark.BookmarkDialog
import mediathek.gui.messages.BookmarkRefreshCompletedEvent
import mediathek.gui.messages.ButtonStartEvent
import mediathek.gui.messages.ReloadTableDataEvent
import mediathek.gui.messages.StartEvent
import mediathek.gui.messages.TableModelChangeEvent
import mediathek.gui.messages.UpdateStatusBarLeftDisplayEvent
import mediathek.gui.messages.history.DownloadHistoryChangedEvent
import mediathek.gui.tabs.DescriptionTabController
import mediathek.gui.tabs.tab_film.actions.CopyUrlToClipboardAction
import mediathek.gui.tabs.tab_film.actions.FilmActionSetup
import mediathek.gui.tabs.tab_film.actions.SaveFilmAction
import mediathek.gui.tabs.tab_film.actions.ToggleFilterDialogVisibilityAction
import mediathek.gui.tabs.tab_film.bookmark.FilmBookmarkController
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.gui.tabs.tab_film.filter.FilmFilterSetup
import mediathek.gui.tabs.tab_film.filter.SwingFilterDialog
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel
import mediathek.gui.tabs.tab_film.lifecycle.BookmarkStartupReloadCoordinator
import mediathek.gui.tabs.tab_film.lifecycle.FilmLifecycleController
import mediathek.gui.tabs.tab_film.lifecycle.FilmRuntimeSetup
import mediathek.gui.tabs.tab_film.search.SearchField
import mediathek.gui.tabs.tab_film.search.SearchFieldHostAdapter
import mediathek.gui.tabs.tab_film.selection.FilmControllerSetup
import mediathek.gui.tabs.tab_film.selection.FilmSelectionController
import mediathek.gui.tabs.tab_film.table.FilmTableInstaller
import mediathek.gui.tabs.tab_film.table.FilmTableReloader
import mediathek.gui.tabs.tab_film.view.FilmUiSetup
import mediathek.gui.tabs.tab_film.view.FilmViewAndTableSetup
import mediathek.gui.tabs.tab_film.view.FilmViewController
import mediathek.mainwindow.MediathekGui
import mediathek.tool.FilterConfiguration
import mediathek.tool.MessageBus
import mediathek.tool.table.MVFilmTable
import net.engio.mbassy.listener.Handler
import kotlin.time.Duration.Companion.milliseconds
import javax.swing.Action
import javax.swing.JCheckBoxMenuItem
import javax.swing.JMenu
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane
import javax.swing.JTable

class GuiFilme(
    aDaten: Daten,
    private val mediathekGui: MediathekGui,
) : JPanel() {
    private val daten: Daten = aDaten
    private val playFilmActionValue: PlayFilmAction
    private val saveFilmActionValue: SaveFilmAction
    private val copyHqUrlToClipboardActionValue: CopyUrlToClipboardAction
    private val copyNormalUrlToClipboardActionValue: CopyUrlToClipboardAction
    private val swingFilterDialog: SwingFilterDialog
    private val toggleFilterDialogVisibilityActionValue: ToggleFilterDialogVisibilityAction
    private val psetButtonsTab = JTabbedPane()
    private val descriptionTabController = DescriptionTabController()
    private val searchField: SearchField
    private val deleteBookmarksAction = DeleteBookmarksAction(MediathekGui.ui())
    private val filterConfiguration = FilterConfiguration()
    private val reloadTableScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    private val filterController: FilmFilterController
    private val filterSelectionComboBoxModel: FilterSelectionComboBoxModel
    private val bookmarkController: FilmBookmarkController
    private var psetButtonsPanel: PsetButtonsPanel? = null
    private var stopBeob = false
    private var tabelle: MVFilmTable? = null
    private val lifecycleController: FilmLifecycleController
    private val viewController: FilmViewController
    private val selectionController: FilmSelectionController
    private val tableReloader: FilmTableReloader
    private val tableInstaller: FilmTableInstaller
    private var reloadTableDataJob: Job? = null

    private val currentTable: MVFilmTable
        get() = tabelle ?: error("Film table has not been initialized")

    init {
        val controllerSetup = FilmControllerSetup.create(
            { currentTable },
            { tabelle },
            this,
            mediathekGui,
            { daten },
            { filterConfiguration.isShowHighQualityOnly },
            this,
            ::repaint,
            ::toggleFilterDialogVisibility,
        )
        selectionController = controllerSetup.selectionController
        bookmarkController = controllerSetup.bookmarkController
        val saveSelectedFilm = controllerSetup.saveSelectedFilm
        val filmActionHost = controllerSetup.filmActionHost
        val filmActions = FilmActionSetup.create(
            filmActionHost,
            selectionController::startFilm,
            mediathekGui,
            deleteBookmarksAction,
            selectionController::getSelectedFilms,
            selectionController::getCurrentlySelectedFilm,
        )
        playFilmActionValue = filmActions.playFilmAction
        saveFilmActionValue = filmActions.saveFilmAction
        copyHqUrlToClipboardActionValue = filmActions.copyHqUrlToClipboardAction
        copyNormalUrlToClipboardActionValue = filmActions.copyNormalUrlToClipboardAction
        toggleFilterDialogVisibilityActionValue = filmActions.toggleFilterDialogVisibilityAction
        val bookmarkStartupReloadCoordinator = BookmarkStartupReloadCoordinator()
        val bookmarkAddFilmAction = filmActions.bookmarkAddFilmAction
        val bookmarkRemoveFilmAction = filmActions.bookmarkRemoveFilmAction
        val manageBookmarkAction = filmActions.manageBookmarkAction
        val filmListScrollPane = JScrollPane()
        val cbkShowDescription = JCheckBoxMenuItem("Beschreibung anzeigen")
        val cbShowButtons = JCheckBoxMenuItem("Buttons anzeigen")
        val filterSetup = FilmFilterSetup(
            filterConfiguration,
            { daten },
            ::requestTableReload,
            ::requestZeitraumReload,
        )
        filterController = filterSetup.filterController
        filterSelectionComboBoxModel = filterSetup.filterSelectionComboBoxModel
        val searchFieldHost = SearchFieldHostAdapter(
            mediathekGui,
            ::loadTable,
            ::loadTable,
        )
        val filmUiActions = filmActions.filmUiActions
        val viewAndTableSetup = FilmViewAndTableSetup.create(
            FilmViewAndTableSetup.ViewDependencies(
                psetButtonsTab,
                { psetButtonsPanel },
                { panel -> psetButtonsPanel = panel },
                { cbShowButtons },
                { cbkShowDescription },
                descriptionTabController,
            ),
            FilmViewAndTableSetup.TableDependencies(
                filmListScrollPane,
                this,
                { currentTable },
                { tabelle },
                { table -> tabelle = table },
            ),
            FilmViewAndTableSetup.ActionDependencies(
                filmActionHost,
                { filmUiActions },
                selectionController::getCurrentlySelectedFilm,
                selectionController::getFilm,
                { playFilmActionValue.actionPerformed(null) },
                { saveSelectedFilm.accept(null) },
                selectionController::startFilm,
                { suspended -> stopBeob = suspended },
            ),
            FilmViewAndTableSetup.RuntimeHooks(
                mediathekGui,
                { updateSelectedListItemsCount(currentTable) },
                ::onComponentShown,
                selectionController::updateFilmData,
                { stopBeob },
            ),
        )
        tableInstaller = viewAndTableSetup.tableInstaller
        viewController = viewAndTableSetup.viewController

        val filmUiSetup = FilmUiSetup.create(
            FilmUiSetup.LayoutDependencies(
                this,
                filmListScrollPane,
                cbkShowDescription,
                descriptionTabController,
                psetButtonsTab,
            ),
            FilmUiSetup.SearchDependencies(daten, searchFieldHost),
            FilmUiSetup.FilterDependencies(
                mediathekGui,
                filterSelectionComboBoxModel,
                filterController,
            ),
            FilmUiSetup.ToolBarActions(
                bookmarkAddFilmAction,
                bookmarkRemoveFilmAction,
                deleteBookmarksAction,
                manageBookmarkAction,
                playFilmActionValue,
                saveFilmActionValue,
                toggleFilterDialogVisibilityActionValue,
            ),
            FilmUiSetup.TableHooks(
                { currentTable },
                tableInstaller::setupFilmListTable,
                tableInstaller::setupFilmSelectionPropertyListener,
                viewController,
                selectionController::getCurrentlySelectedFilm,
            ),
        )
        searchField = filmUiSetup.searchField
        val filmToolBar = filmUiSetup.filmToolBar
        swingFilterDialog = filmUiSetup.swingFilterDialog

        tableInstaller.setupTable()

        val runtimeSetup = FilmRuntimeSetup.create(
            this,
            daten,
            { currentTable },
            filterConfiguration,
            bookmarkStartupReloadCoordinator,
            { swingFilterDialog },
            { filmToolBar },
            { searchField },
            { filmUiActions },
            filterController,
            { suspended -> stopBeob = suspended },
            ::updateStartInfoProperty,
            selectionController::updateFilmData,
            ::requestTableReload,
            ::tabelleSpeichern,
            filterSelectionComboBoxModel::close,
        )
        tableReloader = runtimeSetup.tableReloader
        lifecycleController = runtimeSetup.lifecycleController
        lifecycleController.start()
    }

    private fun toggleFilterDialogVisibility() {
        swingFilterDialog.isVisible = !swingFilterDialog.isVisible
    }

    private fun requestTableReload() {
        reloadTableDataJob?.cancel()
        reloadTableDataJob = reloadTableScope.launch {
            delay(RELOAD_TABLE_DATA_DELAY)
            tableReloader.loadTable()
        }
    }

    private fun requestZeitraumReload() {
        daten.listeBlacklist.filterListe()
        requestTableReload()
    }

    fun copyHqUrlToClipboardAction(): Action = copyHqUrlToClipboardActionValue

    fun copyNormalUrlToClipboardAction(): Action = copyNormalUrlToClipboardActionValue

    fun toggleFilterDialogVisibilityAction(): Action = toggleFilterDialogVisibilityActionValue

    fun resetFilterDialogPosition() {
        swingFilterDialog.setLocation(100, 100)
    }

    fun disposePanel() {
        reloadTableScope.cancel()
        lifecycleController.disposePanel()
    }

    val currentZeitraumFilterValue: String
        get() = filterController.state().zeitraum

    @Handler
    fun handleTableModelChange(event: TableModelChangeEvent) {
        lifecycleController.handleTableModelChange(event)
    }

    fun tabelleSpeichern() {
        tableInstaller.writeTableConfigurationData()
    }

    fun installViewMenuEntry(jMenuAnsicht: JMenu) {
        viewController.installViewMenuEntry(jMenuAnsicht)
    }

    fun installMenuEntries(menu: JMenu) {
        viewController.installMenuEntries(menu)
    }

    private fun onComponentShown() {
        selectionController.updateFilmData()
        updateStartInfoProperty()
    }

    private fun updateSelectedListItemsCount(table: JTable) {
        mediathekGui.selectedListItemsProperty.setSelectedItems(table.selectedRowCount.toLong())
    }

    private fun updateStartInfoProperty() {
        MessageBus.messageBus.publishAsync(UpdateStatusBarLeftDisplayEvent())
    }

    val tableRowCount: Int
        get() = selectionController.getTableRowCount()

    @Handler
    private fun handleDownloadHistoryChangedEvent(event: DownloadHistoryChangedEvent) {
        lifecycleController.handleDownloadHistoryChangedEvent(event)
    }

    @Handler
    private fun handleButtonStart(event: ButtonStartEvent) {
        lifecycleController.handleButtonStart(event)
    }

    @Handler
    private fun handleStartEvent(message: StartEvent) {
        lifecycleController.handleStartEvent(message)
    }

    fun showManageBookmarkWindow() {
        bookmarkController.showManageBookmarkWindow()
    }

    fun getBookmarkDialog(): BookmarkDialog? = bookmarkController.getBookmarkDialog()

    @Handler
    private fun handleReloadTableDataEvent(event: ReloadTableDataEvent) {
        lifecycleController.handleReloadTableDataEvent(event)
    }

    @Handler
    private fun handleBookmarkRefreshCompletedEvent(event: BookmarkRefreshCompletedEvent) {
        lifecycleController.handleBookmarkRefreshCompletedEvent(event)
    }

    private fun loadTable() {
        tableReloader.loadTable()
    }

    private fun loadTable(fromSearchField: Boolean) {
        tableReloader.loadTable(fromSearchField)
    }

    companion object {
        const val NAME = "Filme"
        private val RELOAD_TABLE_DATA_DELAY = 250.milliseconds
    }
}
