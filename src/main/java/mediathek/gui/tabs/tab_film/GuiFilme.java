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
import mediathek.daten.DatenPset;
import mediathek.daten.FilmResolution;
import mediathek.daten.IndexedFilmList;
import mediathek.gui.actions.DeleteBookmarksAction;
import mediathek.gui.actions.ManageBookmarkAction;
import mediathek.gui.actions.PlayFilmAction;
import mediathek.gui.bookmark.BookmarkDialog;
import mediathek.gui.messages.*;
import mediathek.gui.messages.history.DownloadHistoryChangedEvent;
import mediathek.gui.tabs.AGuiTabPanel;
import mediathek.gui.tabs.MarkFilmAsSeenAction;
import mediathek.gui.tabs.MarkFilmAsUnseenAction;
import mediathek.gui.tabs.tab_film.filter.FilmFilterController;
import mediathek.gui.tabs.tab_film.filter.SwingFilterDialog;
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.*;
import mediathek.tool.table.MVFilmTable;
import net.engio.mbassy.listener.Handler;
import org.jdesktop.swingx.VerticalLayout;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.function.Consumer;

public class GuiFilme extends AGuiTabPanel {

    public static final String NAME = "Filme";
    private final PlayFilmAction playFilmAction;
    private final SaveFilmAction saveFilmAction;
    private final CopyUrlToClipboardAction copyHqUrlToClipboardAction;
    private final CopyUrlToClipboardAction copyNormalUrlToClipboardAction;
    private final SwingFilterDialog swingFilterDialog;
    private final ToggleFilterDialogVisibilityAction toggleFilterDialogVisibilityAction;
    private final JTabbedPane psetButtonsTab = new JTabbedPane();
    private final SearchField searchField;
    private final DeleteBookmarksAction deleteBookmarksAction = new DeleteBookmarksAction(MediathekGui.ui());
    private final FilterConfiguration filterConfiguration = new FilterConfiguration();
    private final NonRepeatingTimer reloadTableDataTimer;
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
        descriptionPanel = new FilmDescriptionPanel();
        var selectionHost = new FilmSelectionHostAdapter(
                () -> tabelle,
                () -> tabelle,
                this,
                mediathekGui,
                () -> daten,
                filterConfiguration::isShowHighQualityOnly);
        selectionController = new FilmSelectionController(selectionHost);
        var bookmarkHost = new FilmBookmarkHostAdapter(mediathekGui, this::repaint);
        bookmarkController = new FilmBookmarkController(bookmarkHost);
        Consumer<DatenPset> saveSelectedFilm = pSet -> {
            synchronized (this) {
                selectionController.saveFilm(pSet);
            }
        };
        var filmActionHost = new FilmActionHostAdapter(
                saveSelectedFilm,
                selectionController::getSelectedFilms,
                bookmarkController::updateBookmarkListAndRefresh,
                selectionController::getCurrentlySelectedFilm,
                this::toggleFilterDialogVisibility);
        var filmActions = createFilmActions(filmActionHost);
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
        filterController = new FilmFilterController(
                filterConfiguration,
                new FilmFilterDataProviderAdapter(() -> daten),
                new FilmFilterReloadRequesterAdapter(
                        this::requestTableReload,
                        this::requestZeitraumReload));
        filterSelectionComboBoxModel = new FilterSelectionComboBoxModel(
                filterController::currentFilter,
                filterController::availableFilters,
                filterController::isFilterLocked,
                filterController.selectionObserverRegistry());
        var searchFieldHost = new SearchFieldHostAdapter(
                mediathekGui,
                this::loadTable,
                this::loadTable);
        var filmUiActions = filmActions.filmUiActions();
        var viewAndTableSetup = createViewAndTableSetup(
                cbShowButtons,
                cbkShowDescription,
                filmListScrollPane,
                filmActionHost,
                filmUiActions,
                saveSelectedFilm);
        tableInstaller = viewAndTableSetup.tableInstaller();
        viewController = viewAndTableSetup.viewController();

        setLayout(new BorderLayout());
        add(filmListScrollPane, BorderLayout.CENTER);
        var extensionArea = new JPanel(new VerticalLayout());
        add(extensionArea, BorderLayout.SOUTH);

        if (daten.getListeFilmeNachBlackList() instanceof IndexedFilmList)
            searchField = new LuceneSearchField(searchFieldHost);
        else
            searchField = new RegularSearchField(searchFieldHost);

        // add film description panel
        extensionArea.add(descriptionTab);
        extensionArea.add(psetButtonsTab);

        tableInstaller.setupFilmListTable();
        tableInstaller.setupFilmSelectionPropertyListener();
        setupDescriptionTab(
                tabelle,
                cbkShowDescription,
                ApplicationConfiguration.FILM_SHOW_DESCRIPTION,
                selectionController::getCurrentlySelectedFilm);
        viewController.setupPsetButtonsTab();

        var filmToolBar = new FilmToolBar(filterSelectionComboBoxModel,
                bookmarkAddFilmAction,
                bookmarkRemoveFilmAction,
                deleteBookmarksAction,
                manageBookmarkAction,
                playFilmAction,
                saveFilmAction,
                searchField,
                toggleFilterDialogVisibilityAction);
        add(filmToolBar, BorderLayout.NORTH);

        swingFilterDialog = new SwingFilterDialog(mediathekGui, filterSelectionComboBoxModel,
                filmToolBar.getToggleFilterDialogVisibilityButton(),
                filterController);

        tableInstaller.setupTable();

        var tableReloadHost = new FilmTableReloadHostAdapter(
                () -> tabelle,
                () -> new SearchFieldData(searchField.getText(), searchField.getSearchMode()),
                filterController,
                () -> daten.getDecoratedPool(),
                suspended -> stopBeob = suspended,
                this::updateStartInfoProperty,
                selectionController::updateFilmData);
        tableReloader = new FilmTableReloader(tableReloadHost);
        reloadTableDataTimer = new NonRepeatingTimer(_ -> loadTable());
        var lifecycleHost = new FilmLifecycleHostAdapter(
                this,
                daten,
                () -> tabelle,
                filterConfiguration,
                bookmarkStartupReloadCoordinator,
                () -> swingFilterDialog,
                () -> filmToolBar,
                () -> searchField,
                () -> filmUiActions,
                this::requestTableReload,
                this::updateStartInfoProperty,
                this::tabelleSpeichern,
                filterSelectionComboBoxModel::close);
        lifecycleController = new FilmLifecycleController(lifecycleHost);
        lifecycleController.start();

    }

    private FilmViewAndTableSetup createViewAndTableSetup(
            JCheckBoxMenuItem cbShowButtons,
            JCheckBoxMenuItem cbkShowDescription,
            JScrollPane filmListScrollPane,
            FilmActionHost filmActionHost,
            FilmUiActions filmUiActions,
            Consumer<DatenPset> saveSelectedFilm) {
        var viewHost = new FilmViewHostAdapter(
                psetButtonsTab,
                () -> psetButtonsPanel,
                panel -> psetButtonsPanel = panel,
                () -> cbShowButtons,
                () -> cbkShowDescription,
                () -> filmUiActions,
                this::makeDescriptionTabVisible,
                selectionController::startFilm);
        var tableContextMenuHost = new TableContextMenuHostAdapter(
                () -> tabelle,
                selectionController::getCurrentlySelectedFilm,
                selectionController::getFilm,
                () -> playFilmAction.actionPerformed(null),
                () -> saveSelectedFilm.accept(null),
                selectionController::startFilm,
                suspended -> stopBeob = suspended,
                mediathekGui,
                () -> filmUiActions);
        var tableInstallerHost = new FilmTableInstallerHostAdapter(
                () -> tabelle,
                () -> tabelle,
                table -> tabelle = table,
                filmListScrollPane,
                this,
                () -> tableContextMenuHost,
                filmActionHost,
                () -> filmUiActions,
                () -> updateSelectedListItemsCount(tabelle),
                this::onComponentShown,
                selectionController::updateFilmData,
                () -> stopBeob);

        return new FilmViewAndTableSetup(
                new FilmTableInstaller(tableInstallerHost),
                new FilmViewController(viewHost));
    }

    private FilmActionSetup createFilmActions(FilmActionHost filmActionHost) {
        var playFilmAction = new PlayFilmAction(selectionController::startFilm);
        var saveFilmAction = new SaveFilmAction(filmActionHost);
        var copyHqUrlToClipboardAction =
                new CopyUrlToClipboardAction(filmActionHost, FilmResolution.Enum.HIGH_QUALITY);
        var copyNormalUrlToClipboardAction =
                new CopyUrlToClipboardAction(filmActionHost, FilmResolution.Enum.NORMAL);
        var toggleFilterDialogVisibilityAction = new ToggleFilterDialogVisibilityAction(filmActionHost);
        var bookmarkAddFilmAction = new BookmarkAddFilmAction(filmActionHost);
        var bookmarkRemoveFilmAction = new BookmarkRemoveFilmAction(filmActionHost);
        var manageBookmarkAction = new ManageBookmarkAction(MediathekGui.ui());
        var markFilmAsSeenAction = new MarkFilmAsSeenAction(selectionController::getSelectedFilms);
        var markFilmAsUnseenAction = new MarkFilmAsUnseenAction(selectionController::getSelectedFilms);
        var downloadSubtitleAction = new DownloadSubtitleAction(selectionController::getCurrentlySelectedFilm);
        var filmUiActions = new FilmUiActions(
                playFilmAction,
                saveFilmAction,
                bookmarkAddFilmAction,
                bookmarkRemoveFilmAction,
                deleteBookmarksAction,
                manageBookmarkAction,
                copyNormalUrlToClipboardAction,
                copyHqUrlToClipboardAction,
                markFilmAsSeenAction,
                markFilmAsUnseenAction,
                mediathekGui.toggleBlacklistAction,
                mediathekGui.editBlacklistAction,
                mediathekGui.showFilmInformationAction,
                downloadSubtitleAction);

        return new FilmActionSetup(
                playFilmAction,
                saveFilmAction,
                copyHqUrlToClipboardAction,
                copyNormalUrlToClipboardAction,
                toggleFilterDialogVisibilityAction,
                bookmarkAddFilmAction,
                bookmarkRemoveFilmAction,
                manageBookmarkAction,
                filmUiActions);
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

    @Override
    public void tabelleSpeichern() {
        tableInstaller.writeTableConfigurationData();
    }

    public void installViewMenuEntry(JMenu jMenuAnsicht) {
        viewController.installViewMenuEntry(jMenuAnsicht);
    }

    @Override
    public void installMenuEntries(JMenu menu) {
        viewController.installMenuEntries(menu);
    }

    private void onComponentShown() {
        selectionController.updateFilmData();
        updateStartInfoProperty();
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

    @Override
    protected void setupShowFilmDescriptionMenuItem() {
        viewController.setupShowFilmDescriptionMenuItem();
    }

    private void loadTable() {
        tableReloader.loadTable();
    }

    private void loadTable(boolean from_search_field) {
        tableReloader.loadTable(from_search_field);
    }

    private record FilmActionSetup(
            PlayFilmAction playFilmAction,
            SaveFilmAction saveFilmAction,
            CopyUrlToClipboardAction copyHqUrlToClipboardAction,
            CopyUrlToClipboardAction copyNormalUrlToClipboardAction,
            ToggleFilterDialogVisibilityAction toggleFilterDialogVisibilityAction,
            BookmarkAddFilmAction bookmarkAddFilmAction,
            BookmarkRemoveFilmAction bookmarkRemoveFilmAction,
            ManageBookmarkAction manageBookmarkAction,
            FilmUiActions filmUiActions) {
    }

    private record FilmViewAndTableSetup(
            FilmTableInstaller tableInstaller,
            FilmViewController viewController) {
    }

    static class NonRepeatingTimer extends Timer {
        public NonRepeatingTimer(ActionListener listener) {
            super(250, listener);

            setRepeats(false);
            setCoalesce(true);
        }
    }

}
