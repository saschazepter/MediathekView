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

package mediathek.gui.tabs.tab_film.view

import mediathek.config.Daten
import mediathek.daten.DatenFilm
import mediathek.daten.IndexedFilmList
import mediathek.gui.actions.DeleteBookmarksAction
import mediathek.gui.actions.ManageBookmarkAction
import mediathek.gui.actions.PlayFilmAction
import mediathek.gui.tabs.DescriptionTabController
import mediathek.gui.tabs.tab_film.FilmToolBar
import mediathek.gui.tabs.tab_film.actions.BookmarkAddFilmAction
import mediathek.gui.tabs.tab_film.actions.BookmarkRemoveFilmAction
import mediathek.gui.tabs.tab_film.actions.SaveFilmAction
import mediathek.gui.tabs.tab_film.actions.ToggleFilterDialogVisibilityAction
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.gui.tabs.tab_film.filter.SwingFilterDialog
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel
import mediathek.gui.tabs.tab_film.search.LuceneSearchField
import mediathek.gui.tabs.tab_film.search.RegularSearchField
import mediathek.gui.tabs.tab_film.search.SearchField
import mediathek.mainwindow.MediathekGui
import mediathek.tool.ApplicationConfiguration
import org.jdesktop.swingx.VerticalLayout
import java.awt.BorderLayout
import java.util.*
import java.util.function.Supplier
import javax.swing.JCheckBoxMenuItem
import javax.swing.JTable
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.JTabbedPane

class FilmUiSetup(
    val searchField: SearchField,
    val filmToolBar: FilmToolBar,
    val swingFilterDialog: SwingFilterDialog,
) {
    data class LayoutDependencies(
        val hostPanel: JPanel,
        val filmListScrollPane: JScrollPane,
        val showDescriptionMenuItem: JCheckBoxMenuItem,
        val descriptionTabController: DescriptionTabController,
        val psetButtonsTab: JTabbedPane,
    )

    data class SearchDependencies(
        val daten: Daten,
        val searchFieldHost: SearchField.Host,
    )

    data class FilterDependencies(
        val mediathekGui: MediathekGui,
        val filterSelectionComboBoxModel: FilterSelectionComboBoxModel,
        val filterController: FilmFilterController,
    )

    data class ToolBarActions(
        val bookmarkAddFilmAction: BookmarkAddFilmAction,
        val bookmarkRemoveFilmAction: BookmarkRemoveFilmAction,
        val deleteBookmarksAction: DeleteBookmarksAction,
        val manageBookmarkAction: ManageBookmarkAction,
        val playFilmAction: PlayFilmAction,
        val saveFilmAction: SaveFilmAction,
        val toggleFilterDialogVisibilityAction: ToggleFilterDialogVisibilityAction,
    )

    data class TableHooks(
        val table: () -> JTable,
        val setupFilmListTable: () -> Unit,
        val setupFilmSelectionPropertyListener: () -> Unit,
        val viewController: FilmViewController,
        val selectedFilm: () -> Optional<DatenFilm>,
    )

    companion object {
        fun create(
            layout: LayoutDependencies,
            search: SearchDependencies,
            filter: FilterDependencies,
            actions: ToolBarActions,
            tableHooks: TableHooks,
        ): FilmUiSetup {
            layout.hostPanel.layout = BorderLayout()
            layout.hostPanel.add(layout.filmListScrollPane, BorderLayout.CENTER)
            val extensionArea = JPanel(VerticalLayout())
            layout.hostPanel.add(extensionArea, BorderLayout.SOUTH)

            val searchField = if (search.daten.listeFilmeNachBlackList is IndexedFilmList) {
                LuceneSearchField(search.searchFieldHost)
            } else {
                RegularSearchField(search.searchFieldHost)
            }

            extensionArea.add(layout.descriptionTabController.tabbedPane)
            extensionArea.add(layout.psetButtonsTab)

            tableHooks.setupFilmListTable()
            tableHooks.setupFilmSelectionPropertyListener()
            tableHooks.viewController.setupShowFilmDescriptionMenuItem()
            layout.descriptionTabController.install(
                tableHooks.table(),
                layout.showDescriptionMenuItem,
                ApplicationConfiguration.FILM_SHOW_DESCRIPTION,
                Supplier { tableHooks.selectedFilm() },
            )
            tableHooks.viewController.setupPsetButtonsTab()

            val filmToolBar = FilmToolBar(
                filter.filterSelectionComboBoxModel,
                actions.bookmarkAddFilmAction,
                actions.bookmarkRemoveFilmAction,
                actions.deleteBookmarksAction,
                actions.manageBookmarkAction,
                actions.playFilmAction,
                actions.saveFilmAction,
                searchField,
                actions.toggleFilterDialogVisibilityAction,
            )
            layout.hostPanel.add(filmToolBar, BorderLayout.NORTH)

            val swingFilterDialog = SwingFilterDialog(
                filter.mediathekGui,
                filter.filterSelectionComboBoxModel,
                filmToolBar.toggleFilterDialogVisibilityButton,
                filter.filterController,
            )

            return FilmUiSetup(searchField, filmToolBar, swingFilterDialog)
        }
    }
}
