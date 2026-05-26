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
    private val searchField: SearchField,
    private val filmToolBar: FilmToolBar,
    private val swingFilterDialog: SwingFilterDialog,
) {
    fun searchField(): SearchField = searchField
    fun filmToolBar(): FilmToolBar = filmToolBar
    fun swingFilterDialog(): SwingFilterDialog = swingFilterDialog

    companion object {
        @JvmStatic
        fun create(
            hostPanel: JPanel,
            daten: Daten,
            mediathekGui: MediathekGui,
            filterSelectionComboBoxModel: FilterSelectionComboBoxModel,
            filterController: FilmFilterController,
            filmListScrollPane: JScrollPane,
            showDescriptionMenuItem: JCheckBoxMenuItem,
            searchFieldHost: SearchField.Host,
            bookmarkAddFilmAction: BookmarkAddFilmAction,
            bookmarkRemoveFilmAction: BookmarkRemoveFilmAction,
            deleteBookmarksAction: DeleteBookmarksAction,
            manageBookmarkAction: ManageBookmarkAction,
            playFilmAction: PlayFilmAction,
            saveFilmAction: SaveFilmAction,
            toggleFilterDialogVisibilityAction: ToggleFilterDialogVisibilityAction,
            descriptionTabController: DescriptionTabController,
            psetButtonsTab: JTabbedPane,
            table: Supplier<JTable>,
            setupFilmListTable: Runnable,
            setupFilmSelectionPropertyListener: Runnable,
            viewController: FilmViewController,
            selectedFilm: Supplier<Optional<DatenFilm>>,
        ): FilmUiSetup {
            hostPanel.layout = BorderLayout()
            hostPanel.add(filmListScrollPane, BorderLayout.CENTER)
            val extensionArea = JPanel(VerticalLayout())
            hostPanel.add(extensionArea, BorderLayout.SOUTH)

            val searchField = if (daten.listeFilmeNachBlackList is IndexedFilmList) {
                LuceneSearchField(searchFieldHost)
            } else {
                RegularSearchField(searchFieldHost)
            }

            extensionArea.add(descriptionTabController.tabbedPane)
            extensionArea.add(psetButtonsTab)

            setupFilmListTable.run()
            setupFilmSelectionPropertyListener.run()
            viewController.setupShowFilmDescriptionMenuItem()
            descriptionTabController.install(
                table.get(),
                showDescriptionMenuItem,
                ApplicationConfiguration.FILM_SHOW_DESCRIPTION,
                selectedFilm,
            )
            viewController.setupPsetButtonsTab()

            val filmToolBar = FilmToolBar(
                filterSelectionComboBoxModel,
                bookmarkAddFilmAction,
                bookmarkRemoveFilmAction,
                deleteBookmarksAction,
                manageBookmarkAction,
                playFilmAction,
                saveFilmAction,
                searchField,
                toggleFilterDialogVisibilityAction,
            )
            hostPanel.add(filmToolBar, BorderLayout.NORTH)

            val swingFilterDialog = SwingFilterDialog(
                mediathekGui,
                filterSelectionComboBoxModel,
                filmToolBar.toggleFilterDialogVisibilityButton,
                filterController,
            )

            return FilmUiSetup(searchField, filmToolBar, swingFilterDialog)
        }
    }
}
