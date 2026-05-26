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
import mediathek.daten.DatenFilm;
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
import mediathek.gui.tabs.tab_film.filter.FilmFilterController;
import mediathek.gui.tabs.tab_film.filter.SwingFilterDialog;
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.*;
import mediathek.tool.table.MVFilmTable;
import net.engio.mbassy.listener.Handler;
import org.jdesktop.swingx.VerticalLayout;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Optional;

public class GuiFilme extends AGuiTabPanel {

    public static final String NAME = "Filme";
    public static final boolean[] VISIBLE_COLUMNS = new boolean[DatenFilm.MAX_ELEM];
    private final FilmActionHost filmActionHost = new FilmActionHostAdapter(
            this::saveFilm,
            this::getSelFilme,
            this::updateBookmarkListAndRefresh,
            this::getCurrentlySelectedFilm,
            this::toggleFilterDialogVisibility);
    public final PlayFilmAction playFilmAction = new PlayFilmAction(this);
    public final SaveFilmAction saveFilmAction = new SaveFilmAction(filmActionHost);
    public final CopyUrlToClipboardAction copyHqUrlToClipboardAction = new CopyUrlToClipboardAction(filmActionHost, FilmResolution.Enum.HIGH_QUALITY);
    public final CopyUrlToClipboardAction copyNormalUrlToClipboardAction = new CopyUrlToClipboardAction(filmActionHost, FilmResolution.Enum.NORMAL);
    public final SwingFilterDialog swingFilterDialog;
    public final ToggleFilterDialogVisibilityAction toggleFilterDialogVisibilityAction = new ToggleFilterDialogVisibilityAction(filmActionHost);
    protected final JTabbedPane psetButtonsTab = new JTabbedPane();
    protected final SearchField searchField;
    protected final DeleteBookmarksAction deleteBookmarksAction = new DeleteBookmarksAction(MediathekGui.ui());
    private final FilterConfiguration filterConfiguration = new FilterConfiguration();
    private final BookmarkStartupReloadCoordinator bookmarkStartupReloadCoordinator = new BookmarkStartupReloadCoordinator();
    private final BookmarkAddFilmAction bookmarkAddFilmAction = new BookmarkAddFilmAction(filmActionHost);
    private final BookmarkRemoveFilmAction bookmarkRemoveFilmAction = new BookmarkRemoveFilmAction(filmActionHost);
    private final ManageBookmarkAction manageBookmarkAction = new ManageBookmarkAction(MediathekGui.ui());
    private final MarkFilmAsSeenAction markFilmAsSeenAction = new MarkFilmAsSeenAction();
    private final MarkFilmAsUnseenAction markFilmAsUnseenAction = new MarkFilmAsUnseenAction();
    private final DownloadSubtitleAction downloadSubtitleAction = new DownloadSubtitleAction(this);
    private FilmUiActions filmUiActions;
    private final JScrollPane filmListScrollPane = new JScrollPane();
    private final JCheckBoxMenuItem cbkShowDescription = new JCheckBoxMenuItem("Beschreibung anzeigen");
    private final JCheckBoxMenuItem cbShowButtons = new JCheckBoxMenuItem("Buttons anzeigen");
    private final NonRepeatingTimer reloadTableDataTimer;
    private final FilmFilterController filterController;
    protected final FilterSelectionComboBoxModel filterSelectionComboBoxModel;
    private final FilmToolBar filmToolBar;
    private final FilmBookmarkController bookmarkController;
    private final FilmBookmarkController.Host bookmarkHost;
    protected PsetButtonsPanel psetButtonsPanel;
    private boolean stopBeob;
    private MVFilmTable tabelle;
    private final FilmLifecycleController lifecycleController;
    private final FilmLifecycleController.Host lifecycleHost;
    private final FilmViewController viewController;
    private final FilmViewController.Host viewHost = new FilmViewHostAdapter(
            this,
            psetButtonsTab,
            () -> psetButtonsPanel,
            panel -> psetButtonsPanel = panel,
            () -> cbShowButtons,
            () -> cbkShowDescription,
            () -> filmUiActions,
            this::makeDescriptionTabVisible);
    private final FilmSelectionController selectionController;
    private final FilmSelectionController.Host selectionHost;
    private final FilmTableReloader tableReloader;
    private final FilmTableReloader.Host tableReloadHost;
    private final SearchField.Host searchFieldHost;
    private final TableContextMenuHandler.Host tableContextMenuHost;
    private final FilmTableInstaller.Host tableInstallerHost;
    private final FilmTableInstaller tableInstaller;

    public GuiFilme(Daten aDaten, MediathekGui mediathekGui) {
        daten = aDaten;
        this.mediathekGui = mediathekGui;
        descriptionPanel = new FilmDescriptionPanel();
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
        bookmarkHost = new FilmBookmarkHostAdapter(mediathekGui, this::repaint);
        selectionHost = new FilmSelectionHostAdapter(
                () -> tabelle,
                () -> tabelle,
                this,
                mediathekGui,
                () -> daten,
                filterConfiguration::isShowHighQualityOnly);
        searchFieldHost = new SearchFieldHostAdapter(
                mediathekGui,
                this::loadTable,
                this::loadTable);
        filmUiActions = new FilmUiActions(
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
        tableContextMenuHost = new TableContextMenuHostAdapter(
                () -> tabelle,
                this::getCurrentlySelectedFilm,
                this::getFilm,
                () -> playFilmAction.actionPerformed(null),
                () -> saveFilm(null),
                this::playerStarten,
                suspended -> stopBeob = suspended,
                mediathekGui,
                () -> filmUiActions);
        tableInstallerHost = new FilmTableInstallerHostAdapter(
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
                this::updateFilmData,
                () -> stopBeob);
        tableInstaller = new FilmTableInstaller(tableInstallerHost);
        bookmarkController = new FilmBookmarkController(bookmarkHost);
        viewController = new FilmViewController(viewHost);
        selectionController = new FilmSelectionController(selectionHost);

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
        setupDescriptionTab(tabelle, cbkShowDescription, ApplicationConfiguration.FILM_SHOW_DESCRIPTION, this::getCurrentlySelectedFilm);
        viewController.setupPsetButtonsTab();

        filmToolBar = new FilmToolBar(filterSelectionComboBoxModel,
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

        tableReloadHost = new FilmTableReloadHostAdapter(
                () -> tabelle,
                () -> new SearchFieldData(searchField.getText(), searchField.getSearchMode()),
                filterController,
                () -> daten.getDecoratedPool(),
                suspended -> stopBeob = suspended,
                this::updateStartInfoProperty,
                this::updateFilmData);
        tableReloader = new FilmTableReloader(tableReloadHost);
        reloadTableDataTimer = new NonRepeatingTimer(_ -> loadTable());
        lifecycleHost = new FilmLifecycleHostAdapter(
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

    public void disposePanel() {
        lifecycleController.disposePanel();
    }

    public FilterConfiguration getFilterConfiguration() {
        return filterConfiguration;
    }

    public @NonNull String getCurrentZeitraumFilterValue() {
        return filterController.state().getZeitraum();
    }

    private void updateBookmarkListAndRefresh(List<DatenFilm> films) {
        bookmarkController.updateBookmarkListAndRefresh(films);
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

    /**
     * Show description panel based on settings.
     */
    protected void makeButtonsTabVisible(boolean visible) {
        viewController.makeButtonsTabVisible(visible);
    }

    @Override
    public void installMenuEntries(JMenu menu) {
        viewController.installMenuEntries(menu);
    }

    private void onComponentShown() {
        updateFilmData();
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
     * Handle single or multi film downloads.
     * @param pSet used for downloads or null.
     */
    private synchronized void saveFilm(@Nullable DatenPset pSet) {
        selectionController.saveFilm(pSet);
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

    public void playerStarten(DatenPset pSet) {
        selectionController.startFilm(pSet);
    }

    /**
     * Return the film object from a table row. As this can also be null we will return an Optional to
     * prevent NPEs inside the caller.
     *
     * @param zeileTabelle table row.
     * @return Optional object to a film object.
     */
    private Optional<DatenFilm> getFilm(final int zeileTabelle) {
        return selectionController.getFilm(zeileTabelle);
    }

    @Override
    public Optional<DatenFilm> getCurrentlySelectedFilm() {
        return selectionController.getCurrentlySelectedFilm();
    }

    @Override
    protected List<DatenFilm> getSelFilme() {
        return selectionController.getSelectedFilms();
    }

    /**
     * Update Film Information and description panel with updated film...
     */
    private void updateFilmData() {
        selectionController.updateFilmData();
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

    static class NonRepeatingTimer extends Timer {
        public NonRepeatingTimer(ActionListener listener) {
            super(250, listener);

            setRepeats(false);
            setCoalesce(true);
        }
    }

}
