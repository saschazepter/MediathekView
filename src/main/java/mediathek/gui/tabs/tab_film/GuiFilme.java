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

package mediathek.gui.tabs.tab_film;

import mediathek.config.Daten;
import mediathek.gui.actions.DeleteBookmarksAction;
import mediathek.gui.actions.ManageBookmarkAction;
import mediathek.gui.actions.PlayFilmAction;
import mediathek.gui.bookmark.BookmarkDialog;
import mediathek.gui.messages.*;
import mediathek.gui.messages.history.DownloadHistoryChangedEvent;
import mediathek.gui.tabs.DescriptionTabController;
import mediathek.gui.tabs.tab_film.actions.*;
import mediathek.gui.tabs.tab_film.bookmark.FilmBookmarkController;
import mediathek.gui.tabs.tab_film.bookmark.FilmBookmarkHostAdapter;
import mediathek.gui.tabs.tab_film.lifecycle.*;
import mediathek.gui.tabs.tab_film.search.*;
import mediathek.gui.tabs.tab_film.selection.*;
import mediathek.gui.tabs.tab_film.table.*;
import mediathek.gui.tabs.tab_film.view.*;
import mediathek.gui.tabs.tab_film.filter.FilmFilterController;
import mediathek.gui.tabs.tab_film.filter.FilmFilterSetup;
import mediathek.gui.tabs.tab_film.filter.SwingFilterDialog;
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.*;
import mediathek.tool.table.MVFilmTable;
import net.engio.mbassy.listener.Handler;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class GuiFilme extends JPanel {

    public static final String NAME = "Filme";
    private final Daten daten;
    private final MediathekGui mediathekGui;
    private final PlayFilmAction playFilmAction;
    private final SaveFilmAction saveFilmAction;
    private final CopyUrlToClipboardAction copyHqUrlToClipboardAction;
    private final CopyUrlToClipboardAction copyNormalUrlToClipboardAction;
    private final SwingFilterDialog swingFilterDialog;
    private final ToggleFilterDialogVisibilityAction toggleFilterDialogVisibilityAction;
    private final JTabbedPane psetButtonsTab = new JTabbedPane();
    private final DescriptionTabController descriptionTabController = new DescriptionTabController();
    private final SearchField searchField;
    private final DeleteBookmarksAction deleteBookmarksAction = new DeleteBookmarksAction(MediathekGui.ui());
    private final FilterConfiguration filterConfiguration = new FilterConfiguration();
    private final Timer reloadTableDataTimer;
    private final FilmFilterController filterController;
    private final FilterSelectionComboBoxModel filterSelectionComboBoxModel;
    private final FilmBookmarkController bookmarkController;
    private PsetButtonsPanel psetButtonsPanel;
    private boolean stopBeob;
    private MVFilmTable tabelle;
    private final FilmLifecycleController lifecycleController;
    private final FilmViewController viewController;
    private final FilmSelectionController selectionController;
    private final FilmTableReloader tableReloader;
    private final FilmTableInstaller tableInstaller;

    public GuiFilme(Daten aDaten, MediathekGui mediathekGui) {
        daten = aDaten;
        this.mediathekGui = mediathekGui;
        var controllerSetup = FilmControllerSetup.create(
                () -> tabelle,
                () -> tabelle,
                this,
                mediathekGui,
                () -> daten,
                filterConfiguration::isShowHighQualityOnly,
                this,
                this::repaint,
                this::toggleFilterDialogVisibility);
        selectionController = controllerSetup.selectionController();
        bookmarkController = controllerSetup.bookmarkController();
        var saveSelectedFilm = controllerSetup.saveSelectedFilm();
        var filmActionHost = controllerSetup.filmActionHost();
        var filmActions = FilmActionSetup.create(
                filmActionHost,
                selectionController::startFilm,
                mediathekGui,
                deleteBookmarksAction,
                selectionController::getSelectedFilms,
                selectionController::getCurrentlySelectedFilm);
        playFilmAction = filmActions.playFilmAction();
        saveFilmAction = filmActions.saveFilmAction();
        copyHqUrlToClipboardAction = filmActions.copyHqUrlToClipboardAction();
        copyNormalUrlToClipboardAction = filmActions.copyNormalUrlToClipboardAction();
        toggleFilterDialogVisibilityAction = filmActions.toggleFilterDialogVisibilityAction();
        var bookmarkStartupReloadCoordinator = new BookmarkStartupReloadCoordinator();
        var bookmarkAddFilmAction = filmActions.bookmarkAddFilmAction();
        var bookmarkRemoveFilmAction = filmActions.bookmarkRemoveFilmAction();
        var manageBookmarkAction = filmActions.manageBookmarkAction();
        var filmListScrollPane = new JScrollPane();
        var cbkShowDescription = new JCheckBoxMenuItem("Beschreibung anzeigen");
        var cbShowButtons = new JCheckBoxMenuItem("Buttons anzeigen");
        var filterSetup = new FilmFilterSetup(
                filterConfiguration,
                () -> daten,
                this::requestTableReload,
                this::requestZeitraumReload);
        filterController = filterSetup.getFilterController();
        filterSelectionComboBoxModel = filterSetup.getFilterSelectionComboBoxModel();
        var searchFieldHost = new SearchFieldHostAdapter(
                mediathekGui,
                this::loadTable,
                this::loadTable);
        var filmUiActions = filmActions.filmUiActions();
        var viewAndTableSetup = FilmViewAndTableSetup.create(
                psetButtonsTab,
                () -> psetButtonsPanel,
                panel -> psetButtonsPanel = panel,
                () -> cbShowButtons,
                () -> cbkShowDescription,
                filmListScrollPane,
                this,
                () -> tabelle,
                () -> tabelle,
                table -> tabelle = table,
                filmActionHost,
                () -> filmUiActions,
                selectionController::getCurrentlySelectedFilm,
                selectionController::getFilm,
                () -> playFilmAction.actionPerformed(null),
                () -> saveSelectedFilm.accept(null),
                selectionController::startFilm,
                suspended -> stopBeob = suspended,
                mediathekGui,
                () -> updateSelectedListItemsCount(tabelle),
                this::onComponentShown,
                selectionController::updateFilmData,
                () -> stopBeob,
                descriptionTabController);
        tableInstaller = viewAndTableSetup.tableInstaller();
        viewController = viewAndTableSetup.viewController();

        var filmUiSetup = createFilmUi(
                filmListScrollPane,
                cbkShowDescription,
                searchFieldHost,
                bookmarkAddFilmAction,
                bookmarkRemoveFilmAction,
                manageBookmarkAction);
        searchField = filmUiSetup.searchField();
        var filmToolBar = filmUiSetup.filmToolBar();
        swingFilterDialog = filmUiSetup.swingFilterDialog();

        tableInstaller.setupTable();

        var runtimeSetup = FilmRuntimeSetup.create(
                this,
                daten,
                () -> tabelle,
                filterConfiguration,
                bookmarkStartupReloadCoordinator,
                () -> swingFilterDialog,
                () -> filmToolBar,
                () -> searchField,
                () -> filmUiActions,
                filterController,
                suspended -> stopBeob = suspended,
                this::updateStartInfoProperty,
                selectionController::updateFilmData,
                this::requestTableReload,
                this::tabelleSpeichern,
                filterSelectionComboBoxModel::close,
                NonRepeatingTimer::new);
        tableReloader = runtimeSetup.tableReloader();
        reloadTableDataTimer = runtimeSetup.reloadTableDataTimer();
        lifecycleController = runtimeSetup.lifecycleController();
        lifecycleController.start();

    }

    private FilmUiSetup createFilmUi(
            JScrollPane filmListScrollPane,
            JCheckBoxMenuItem cbkShowDescription,
            SearchField.Host searchFieldHost,
            BookmarkAddFilmAction bookmarkAddFilmAction,
            BookmarkRemoveFilmAction bookmarkRemoveFilmAction,
            ManageBookmarkAction manageBookmarkAction) {
        return FilmUiSetup.create(
                this,
                daten,
                mediathekGui,
                filterSelectionComboBoxModel,
                filterController,
                filmListScrollPane,
                cbkShowDescription,
                searchFieldHost,
                bookmarkAddFilmAction,
                bookmarkRemoveFilmAction,
                deleteBookmarksAction,
                manageBookmarkAction,
                playFilmAction,
                saveFilmAction,
                toggleFilterDialogVisibilityAction,
                descriptionTabController,
                psetButtonsTab,
                () -> tabelle,
                tableInstaller::setupFilmListTable,
                tableInstaller::setupFilmSelectionPropertyListener,
                viewController,
                selectionController::getCurrentlySelectedFilm);
    }

    private void toggleFilterDialogVisibility() {
        var visible = swingFilterDialog.isVisible();
        visible = !visible;
        swingFilterDialog.setVisible(visible);
    }

    private void requestTableReload() {
        if (!reloadTableDataTimer.isRunning())
            reloadTableDataTimer.start();
        else
            reloadTableDataTimer.restart();
    }

    private void requestZeitraumReload() {
        daten.getListeBlacklist().filterListe();
        requestTableReload();
    }

    public Action copyHqUrlToClipboardAction() {
        return copyHqUrlToClipboardAction;
    }

    public Action copyNormalUrlToClipboardAction() {
        return copyNormalUrlToClipboardAction;
    }

    public Action toggleFilterDialogVisibilityAction() {
        return toggleFilterDialogVisibilityAction;
    }

    public void resetFilterDialogPosition() {
        swingFilterDialog.setLocation(100, 100);
    }

    public void disposePanel() {
        lifecycleController.disposePanel();
    }

    public @NonNull String getCurrentZeitraumFilterValue() {
        return filterController.state().getZeitraum();
    }

    @Handler
    public void handleTableModelChange(TableModelChangeEvent e) {
        lifecycleController.handleTableModelChange(e);
    }

    public void tabelleSpeichern() {
        tableInstaller.writeTableConfigurationData();
    }

    public void installViewMenuEntry(JMenu jMenuAnsicht) {
        viewController.installViewMenuEntry(jMenuAnsicht);
    }

    public void installMenuEntries(JMenu menu) {
        viewController.installMenuEntries(menu);
    }

    private void onComponentShown() {
        selectionController.updateFilmData();
        updateStartInfoProperty();
    }

    private void updateSelectedListItemsCount(JTable table) {
        mediathekGui.selectedListItemsProperty.setSelectedItems(table.getSelectedRowCount());
    }

    private void updateStartInfoProperty() {
        MessageBus.getMessageBus().publishAsync(new UpdateStatusBarLeftDisplayEvent());
    }

    public int getTableRowCount() {
        return selectionController.getTableRowCount();
    }

    @Handler
    private void handleDownloadHistoryChangedEvent(DownloadHistoryChangedEvent e) {
        lifecycleController.handleDownloadHistoryChangedEvent(e);
    }

    @Handler
    private void handleButtonStart(ButtonStartEvent e) {
        lifecycleController.handleButtonStart(e);
    }

    @Handler
    private void handleStartEvent(StartEvent msg) {
        lifecycleController.handleStartEvent(msg);
    }

    /**
     * If necessary instantiate and show the bookmark window
     */
    public void showManageBookmarkWindow() {
        bookmarkController.showManageBookmarkWindow();
    }

    public BookmarkDialog getBookmarkDialog() {
        return bookmarkController.getBookmarkDialog();
    }

    /**
     * Update table data when receiving ReloadTableDataEvent or subclasses of it.
     * @param e event
     */
    @Handler
    private void handleReloadTableDataEvent(ReloadTableDataEvent e) {
        lifecycleController.handleReloadTableDataEvent(e);
    }

    @Handler
    private void handleBookmarkRefreshCompletedEvent(BookmarkRefreshCompletedEvent e) {
        lifecycleController.handleBookmarkRefreshCompletedEvent(e);
    }

    private void loadTable() {
        tableReloader.loadTable();
    }

    private void loadTable(boolean from_search_field) {
        tableReloader.loadTable(from_search_field);
    }

    static class NonRepeatingTimer extends Timer {
        public NonRepeatingTimer(ActionListener listener) {
            super(250, listener);

            setRepeats(false);
            setCoalesce(true);
        }
    }

}
