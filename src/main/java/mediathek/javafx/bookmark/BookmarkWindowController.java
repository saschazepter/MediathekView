package mediathek.javafx.bookmark;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.input.ContextMenuEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.text.Font;
import javafx.stage.Modality;
import javafx.stage.Stage;
import jiconfont.icons.font_awesome.FontAwesome;
import jiconfont.javafx.IconNode;
import mediathek.config.Daten;
import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.DatenDownload;
import mediathek.daten.DatenFilm;
import mediathek.gui.actions.UrlHyperlinkAction;
import mediathek.gui.dialog.DialogAddDownload;
import mediathek.gui.messages.ReloadTableDataEvent;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.ApplicationConfiguration;
import mediathek.tool.MessageBus;
import mediathek.tool.timer.TimerPool;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.sync.LockMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import static javafx.scene.input.MouseButton.PRIMARY;


/**
 * Bookmark window controller:
 * Create and display bookmark window
 *
 * @author Klaus Wich <klaus.wich@aim.com>
 */
public class BookmarkWindowController implements Initializable {

  private static final Logger logger = LogManager.getLogger();

  static {
    Font.loadFont(BookmarkWindowController.class.getResourceAsStream("/mediathek/res/programm/fxml/fontawesome-webfont.ttf"), 16);
  }

  private final BookmarkDataList listeBookmarkList;
  private FilterState filterState = FilterState.UNDEFINED;
  private Stage stage;
  private FilteredList<BookmarkData> filteredBookmarkList;
  private MenuItem playitem;
  private MenuItem loaditem;
  private MenuItem deleteitem;
  private MenuItem viewitem;
  private MenuItem webitem;
  private MenuItem ccopyitem;
  private MenuItem edititem;
  private ContextMenu cellContextMenu;
  private double divposition;
  private boolean listUpdated; // indicates new updates to bookmarklist
  private ScheduledFuture<?> SaveBookmarkTask; // Future task to save
  @FXML
  private Button btnSaveList;
  @FXML
  private Button btnDeleteEntry;
  @FXML
  private Button btnMarkViewed;
  @FXML
  private ToggleButton btnShowDetails;
  @FXML
  private Button btnFilter;
  @FXML
  private Button btnEditNote;
  @FXML
  private TableView<BookmarkData> tbBookmarks;
  @FXML
  private TableColumn<BookmarkData, String> colSender;
  @FXML
  private TableColumn<BookmarkData, String> colTheme;
  @FXML
  private TableColumn<BookmarkData, String> colTitle;
  @FXML
  private TableColumn<BookmarkData, String> colDuration;
  @FXML
  private TableColumn<BookmarkData, String> colRunDate;
  @FXML
  private TableColumn<BookmarkData, String> colBtnPlay;
  @FXML
  private TableColumn<BookmarkData, String> colBtnDownload;
  @FXML
  private TableColumn<BookmarkData, String> colNote;
  @FXML
  private Label lblCount;
  @FXML
  private Label lblSeen;
  @FXML
  private Label lblFilter;
  @FXML
  private TextArea taDescription;
  @FXML
  private SplitPane spSplitPane;
  @FXML
  private Hyperlink hyperLink;

  public BookmarkWindowController() {
    listeBookmarkList = Daten.getInstance().getListeBookmarkList();
    listUpdated = false;
  }

  private static void setStageSize(Stage window) {
    Configuration config = ApplicationConfiguration.getConfiguration();
    try {
      config.lock(LockMode.READ);
      window.setWidth(config.getInt(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".width", 640));
      window.setHeight(config.getInt(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".heigth", 480));
      window.setX(config.getInt(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".location.x", 0));
      window.setY(config.getInt(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".location.y", 0));
    }
    finally {
      config.unlock(LockMode.READ);
    }
  }

  /**
   * Invoke on the JavaFx thread and wait for it to return. Be very careful
   * with this because this can cause deadlocks.
   */
  private void invokeInFxThreadAndWait(final Runnable run) {
    if (Platform.isFxApplicationThread()) {
      run.run();
      return;
    }

    try {
      FutureTask<Void> future = new FutureTask<>(run, null);
      Platform.runLater(future);
      future.get();
    } catch (ExecutionException | InterruptedException e) {
      logger.error("invokeInFxThreadAndWait() failed", e);
    }
  }

  /**
   * Marks all selected films as seen if unseen (at least one in selection)
   * or unseen if all are seen
   */
  @FXML
  private void btnMarkEntryAsViewed(Event e) {
    ObservableList<BookmarkData> selections = tbBookmarks.getSelectionModel().getSelectedItems();
    if (!selections.isEmpty()) {
      boolean hasUnSeen = isUnSeenSelected(); // true if unseen in selection
      List<DatenFilm> filmlist = new ArrayList<>();
      selections.forEach((data) -> {
        data.setSeen(hasUnSeen);
        DatenFilm film = data.getDatenFilm();
        if (film != null) {
          filmlist.add(film);
        }
      });

      try (SeenHistoryController history = new SeenHistoryController()) {
        if (hasUnSeen) {
          history.markSeen(filmlist);
        }
        else {
          history.markUnseen(filmlist);
        }
      }

      setSeenButtonState(hasUnSeen, selections);
       // reselect to trigger updates:
      tbBookmarks.getSelectionModel().clearSelection();
      selections.forEach((data) -> tbBookmarks.getSelectionModel().select(data));
    }
  }

  @FXML
  private void btnSaveBookMarkList(Event e) {
    cancelBookmarkSave();
    saveBookMarkList();
  }

  @FXML
  private void btnDeleteEntry(Event e) {
    var selModel = tbBookmarks.getSelectionModel();
    var items = selModel.getSelectedItems();

    if (!items.isEmpty()) {
      listeBookmarkList.deleteEntries(items);
      updateDisplay();
      selModel.clearSelection();

      MessageBus.getMessageBus().publishAsync(new ReloadTableDataEvent());
    }
  }

  @FXML
  private void btnEditNote(Event e) {
    try {
      BookmarkNoteDialogController noteDialogController = new BookmarkNoteDialogController();
      FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/mediathek/res/programm/fxml/bookmarkNoteDialog.fxml"));
      fxmlLoader.setController(noteDialogController);

      var noteDialog = new Stage();
      noteDialog.initModality(Modality.WINDOW_MODAL);
      noteDialog.initOwner(stage);
      noteDialog.getIcons().add(new Image("/mediathek/res/MediathekView.png"));
      noteDialog.setScene(new Scene(fxmlLoader.load()));
      if (noteDialogController.setAndShow(noteDialog, tbBookmarks.getSelectionModel().getSelectedItem())) {
        listUpdated = true;
        refresh();
      }
    }
    catch (IOException ex) {
      logger.error("Can't find/load the FXML description!", ex);
    }
  }

  @FXML
  private void hyperLinkSelected(Event e) {
    String url = tbBookmarks.getSelectionModel().getSelectedItem().getWebUrl();
    if (url != null) {
      try {
          UrlHyperlinkAction.openURL(url);
      }
      catch (URISyntaxException ex) {
        logger.error("Hyperlink Syntax exception", ex);
      }
    }
  }

  @SuppressWarnings("unchecked")
  private void copy2Clipboard(Event e) {
    TablePosition<BookmarkData, String> pos = tbBookmarks.getSelectionModel().getSelectedCells().getFirst();
    BookmarkData item = tbBookmarks.getItems().get(pos.getRow());
    String data = pos.getTableColumn().getCellObservableValue(item).getValue();
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(data), null);
  }

  private void setupTableColumns() {
    // connect columns with underlying data
    colSender.setCellValueFactory(param -> {
      var film = param.getValue().getDatenFilm();
      if (film != null) {
        return new SimpleStringProperty(film.getSender());
      }
      else
        return null;
    });
    colTheme.setCellValueFactory(param -> {
      var film = param.getValue().getDatenFilm();
      if (film != null) {
        return new SimpleStringProperty(film.getThema());
      }
      else
        return null;
    });
    colTitle.setCellValueFactory(param -> {
      var film = param.getValue().getDatenFilm();
      if (film != null) {
        return new SimpleStringProperty(film.getTitle());
      }
      else
        return new SimpleStringProperty("Kein Film gefunden!");
    });
    colDuration.setCellValueFactory(param -> {
      var film = param.getValue().getDatenFilm();
      if (film != null) {
        return new SimpleStringProperty(film.getFilmLengthAsString());
      }
      else
        return null;
    });
    colRunDate.setCellValueFactory(param -> {
      var film = param.getValue().getDatenFilm();
      if (film != null) {
        return new SimpleStringProperty(film.getSendeDatum());
      }
      else
        return null;
    });
    colRunDate.setComparator(new BookmarkDateComparator());
    colNote.setCellValueFactory(new PropertyValueFactory<>("note"));

    // add button to play URL:
    colBtnPlay.setCellFactory((final var _) -> new TableCell<>() {
      @Override
      public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getTableView().getItems().get(getIndex()).isNotInFilmList()) {
          setGraphic(null);
        } else {
          setGraphic(new IconNode(FontAwesome.PLAY));
          this.setOnMouseClicked(_ -> playAction(getTableView().getItems().get(getIndex())));
        }
      }
    });

    // add button to download URL:
    colBtnDownload.setCellFactory((final var _) -> new TableCell<>() {
      @Override
      public void updateItem(String item, boolean empty) {
        super.updateItem(item, empty);
        if (empty || getTableView().getItems().get(getIndex()).isNotInFilmList()) {
          setGraphic(null);
        } else {
          setGraphic(new IconNode(FontAwesome.DOWNLOAD));
          this.setOnMouseClicked(_ -> loadAction(getTableView().getItems().get(getIndex())));
        }
      }
    });
  }

  private void setupTableView() {
    // create filtered and sortable list
    var observableList = listeBookmarkList.getObervableList();
    filteredBookmarkList = new FilteredList<>(observableList, _ -> true);
    SortedList<BookmarkData> sortedBookmarkList = new SortedList<>(filteredBookmarkList);
    sortedBookmarkList.comparatorProperty().bind(tbBookmarks.comparatorProperty());

    observableList.addListener((ListChangeListener.Change<? extends BookmarkData> c) -> {
      while (c.next()) {
        if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
          listUpdated = true;
          break;
        }
      }
      tbBookmarks.refresh();
      invokeInFxThreadAndWait(this::updateDisplay);
    });

    tbBookmarks.setItems(sortedBookmarkList);
    tbBookmarks.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

    // Add listener to set button and context item state depending on selection
    tbBookmarks.getSelectionModel().selectedIndexProperty().addListener((_, _, newSelection) -> {
      boolean disable = newSelection == null || newSelection.intValue() == -1;
      var selModel = tbBookmarks.getSelectionModel();
      var items = selModel.getSelectedItems();
      boolean multipleSelected = items.size() > 1;

      btnDeleteEntry.setDisable(disable);
      btnMarkViewed.setDisable(disable || onlyLifeStreamSelected());
      disable = disable || multipleSelected; // allow only for single selection
      btnEditNote.setDisable(disable);
      playitem.setDisable(disable);
      edititem.setDisable(disable);
      loaditem.setDisable(disable);
      viewitem.setDisable(onlyLifeStreamSelected());
      webitem.setDisable(disable || selModel.getSelectedItem().getWebUrl() == null);
      ccopyitem.setDisable(disable);

      // Update buttons: Check if not seen in selection and adapt button text
      boolean setViewed = isUnSeenSelected();
      setSeenButtonState(setViewed, items);
      deleteitem.setText(String.format("Film%s aus der Merkliste entfernen",(multipleSelected ? "e" : "")));
      // change description
      updateDescriptionArea();
    });

    tbBookmarks.getSortOrder().addListener((ListChangeListener.Change<? extends TableColumn<BookmarkData,?>> _) -> {
      tbBookmarks.getSelectionModel().clearSelection(); // clear selection after sort
    });
  }

  @Override
  public void initialize(URL arg0, ResourceBundle arg1) {
    restoreTableStateAndContextMenu();
    setupTableColumns();
    setupTableView();

    btnFilterAction (null);
    var config = ApplicationConfiguration.getConfiguration();
    btnShowDetails.setSelected(config.getBoolean(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".details", true));
    divposition = config.getDouble(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".divider", spSplitPane.getDividerPositions()[0]);
    btnShowDetailsAction(null);
    updateDescriptionArea();

    setupColumnContextMenu();

    setButtonFontIcons();
  }

  private void setButtonFontIcons() {
    try {
      btnDeleteEntry.setGraphic(new IconNode(FontAwesome.TRASH));
      btnMarkViewed.setGraphic(new IconNode(FontAwesome.EYE));
      btnEditNote.setGraphic(new IconNode(FontAwesome.PENCIL));
      btnSaveList.setGraphic(new IconNode(FontAwesome.FLOPPY_O));
      btnShowDetails.setGraphic(new IconNode(FontAwesome.INFO_CIRCLE));
      btnFilter.setGraphic(new IconNode(FontAwesome.FILTER));
    }
    catch (Exception e) {
      logger.error("Could not load fontawesome font", e);
    }
  }

  private void setupColumnContextMenu() {
    tbBookmarks.setTableMenuButtonVisible(true);
    // setup column context menu
    new TableViewColumnContextMenuHelper(tbBookmarks) {
      @Override protected CustomMenuItem createColumnCustomMenuItem(
              final ContextMenu contextMenu, final TableColumn<?, ?> column) {
        final CheckBox checkBox;
        var columnText = column.getText();
        if (!columnText.isEmpty())
          checkBox = new CheckBox(columnText);
        else {
          checkBox = new CheckBox(" ");
          Node icon = switch (column.getId()) {
            case "colBtnPlay" -> new IconNode(FontAwesome.PLAY);
            case "colBtnDownload" -> new IconNode(FontAwesome.DOWNLOAD);
            default -> throw new IllegalStateException("unknown id");
          };
          checkBox.setGraphic(icon);
        }
        // the context menu item's state controls its bound column's visibility
        checkBox.selectedProperty().bindBidirectional(column.visibleProperty());

        var customMenuItem = new CustomMenuItem(checkBox);
        customMenuItem.setOnAction(event -> {
          checkBox.setSelected(!checkBox.isSelected());
          event.consume();
        });
        customMenuItem.setHideOnClick(false); // set to false so the context menu stays visible after click
        return customMenuItem;
      }
    };
  }

  private void updateDescriptionArea() {
    boolean showurl = false;
    var model = tbBookmarks.getSelectionModel();

    taDescription.setText(model.getSelectedItems().size() == 1 ? model.getSelectedItem().getExtendedDescription() : "");
    if (model.getSelectedItems().size() == 1) {
      String url = model.getSelectedItem().getWebUrl();
      if (url != null && !url.isEmpty()) {
        hyperLink.setTooltip(new Tooltip(url));
        hyperLink.setVisited(false);
        showurl = true;
      }
    }

    hyperLink.setVisible(showurl);
  }

  private void setSeenButtonState(boolean setViewed, @NotNull ObservableList<BookmarkData> items) {
    var multipleSelected = items.size() > 1;

    btnMarkViewed.setGraphic(new IconNode(setViewed ? FontAwesome.EYE: FontAwesome.EYE_SLASH));
    String text = String.format("Film%s als %sgesehen markieren", (multipleSelected ? "e" : ""), (setViewed ? "" : "un"));
    btnMarkViewed.setTooltip(new Tooltip(text));
    viewitem.setText(text);
    viewitem.setGraphic(new IconNode(setViewed ? FontAwesome.EYE: FontAwesome.EYE_SLASH));
  }

 /**
  * Restore table state and context menues from stored settings
  */
  private void restoreTableStateAndContextMenu() {
    // create cell ContextMenu
    cellContextMenu = new ContextMenu();
    // - create items
    playitem = new MenuItem("Film abspielen");
    playitem.setOnAction((ActionEvent _) -> playAction(tbBookmarks.getSelectionModel().getSelectedItem()));
    playitem.setGraphic(new IconNode(FontAwesome.PLAY));

    loaditem = new MenuItem("Film aufzeichnen");
    loaditem.setOnAction((ActionEvent _) -> loadAction(tbBookmarks.getSelectionModel().getSelectedItem()));
    loaditem.setGraphic(new IconNode(FontAwesome.DOWNLOAD));

    viewitem = new MenuItem();
    viewitem.setOnAction(this::btnMarkEntryAsViewed);

    edititem = new MenuItem("Anmerkungen bearbeiten");
    edititem.setOnAction(this::btnEditNote);
    edititem.setGraphic(new IconNode(FontAwesome.PENCIL));

    deleteitem = new MenuItem();
    deleteitem.setOnAction(this::btnDeleteEntry);
    deleteitem.setGraphic(new IconNode(FontAwesome.MINUS));

    webitem = new MenuItem("Film Webseite öffnen");
    webitem.setOnAction(this::hyperLinkSelected);

    ccopyitem = new MenuItem("Zellinhalt in die Ablage kopieren");
    ccopyitem.setOnAction(this::copy2Clipboard);

    // - add menue items to Cell ContextMenu
    cellContextMenu.getItems().addAll(playitem, loaditem, viewitem, new SeparatorMenuItem(), edititem, deleteitem,
                                      new SeparatorMenuItem(), webitem, ccopyitem);

    // Restore column width, state and sequence
    Configuration config = ApplicationConfiguration.getConfiguration();
    try {
      config.lock(LockMode.READ);
      String colbase = ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".columns.";
      int entries = config.getInt(colbase + "no", 0);
      if (entries > 0) {
        List<TableColumn<BookmarkData, ?>> collist = new ArrayList<>(tbBookmarks.getColumns());
        tbBookmarks.getColumns().clear();
        for (int i = 1; i <= entries; i++) {
          String colref = colbase + "col" + i;
          String colid = config.getString(colref + ".id");
          int colsize = config.getInt(colref + ".size", 20);
          boolean colvisible = config.getBoolean(colref + ".visible", true);
          for (var column : collist) {
            if (column.getId().equals(colid)) {
              column.setPrefWidth(colsize);
              column.setVisible(colvisible);
              tbBookmarks.getColumns().add(column);
              break;
            }
          }
        }
      }
    }
    finally {
      config.unlock(LockMode.READ);
    }
  }

  @FXML
  private void btnShowDetailsAction(ActionEvent event) {
    double newposition;
    if (btnShowDetails.isSelected()) {
      newposition = divposition;
    }
    else {
      divposition = spSplitPane.getDividerPositions()[0];
      newposition = 1.0;
    }
    spSplitPane.setDividerPositions(newposition);
  }

  @FXML
  private void btnFilterAction(ActionEvent e) {
    filterState = filterState.next();
    switch (filterState) {
      case ALL -> filteredBookmarkList.setPredicate(_ -> true);
      case UNSEEN -> filteredBookmarkList.setPredicate(BookmarkData::getNotSeen);
      case SEEN -> filteredBookmarkList.setPredicate(BookmarkData::getSeen);
    }

    btnFilter.setTooltip(new Tooltip(filterState.tooltipText()));
    lblFilter.setText(filterState.messageText());
    lblSeen.setDisable(filterState.buttonState());
    refresh();
  }

  @FXML
  @SuppressWarnings("unchecked")
  private void tbviewOnContextRequested(ContextMenuEvent event) {
    if (!tbBookmarks.getSelectionModel().getSelectedItems().isEmpty()) { // Do not show row context menu if nothing is selected
      if (!ccopyitem.isDisable()) { // adapt copy content to column
        TablePosition<BookmarkData, String> pos = tbBookmarks.getSelectionModel().getSelectedCells().getFirst();
        BookmarkData item = tbBookmarks.getItems().get(pos.getRow());
        String sdata = pos.getTableColumn() != null ? pos.getTableColumn().getCellObservableValue(item).getValue() : "";
        ccopyitem.setDisable(sdata == null || sdata.isBlank()); // Disable if cell is empty:
        ccopyitem.setText((pos.getTableColumn() != null ? pos.getTableColumn().getText(): "Text" ) +  " kopieren");
      }
      cellContextMenu.show(tbBookmarks, event.getScreenX(), event.getScreenY());
    }
  }

  @FXML
  private void tbviewMouseClick(MouseEvent e) {
    if (e.getButton() == PRIMARY) {
      if (cellContextMenu.isShowing())
        cellContextMenu.hide();

      if (e.getClickCount() > 1 && tbBookmarks.getSelectionModel().getSelectedItems().size() == 1) {
        btnEditNote(null);
      }
    }
  }

  /**
   * Display Window on screen
   * During first call a new window is created, for successive calls the existing window is reused
   */
  public void show() {
    invokeInFxThreadAndWait(() -> {
      if (stage == null) {
        stage = new Stage();
        setStageSize(stage); // restore size
        setStageEvents();
        stage.setTitle("Merkliste verwalten");
        stage.getIcons().add(new Image("/mediathek/res/MediathekView.png"));
        try {
          FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/mediathek/res/programm/fxml/bookmarkWindow.fxml"));
          fxmlLoader.setController(this);
          Scene scene = new Scene(fxmlLoader.load());
          stage.setScene(scene);
          scene.getStylesheets().add(getClass().getResource("/mediathek/res/css/bookmarkWindow.css").toExternalForm());
        }
        catch (IOException e) {
          logger.error("Can't find/load the FXML description!", e);
          stage = null;
        }
      }
      else {
        tbBookmarks.getSelectionModel().clearSelection();
        if (stage.isIconified()) {
          stage.setIconified(false);
        }
        stage.toFront();
        stage.requestFocus();
      }

      if (stage != null) {
        stage.show();
        refresh();
      }
    });
  }

  private void refresh() {
    if (stage.isShowing()) {
      tbBookmarks.refresh();
      updateDisplay();
    }
  }

  private void scheduleBookmarkSave() {
    cancelBookmarkSave();
    SaveBookmarkTask = TimerPool.getTimerPool().schedule(() -> {
              saveBookMarkList();
              SaveBookmarkTask = null;
            },
            30,
            TimeUnit.SECONDS);
  }

  private void updateDisplay() {
    lblCount.setText(String.format("Einträge: %d / %d", filteredBookmarkList.size(), listeBookmarkList.getNbOfEntries()));
    lblSeen.setText(String.format("Gesehen: %d", listeBookmarkList.getSeenNbOfEntries()));
    btnSaveList.setDisable(!listUpdated);
    if (listUpdated) {
      scheduleBookmarkSave();
    }
  }

  /**
   * Save Bookmark list to backup storage
   */
  private void saveBookMarkList() {
    if (listUpdated) {
      listeBookmarkList.saveToFile();
      btnSaveList.setDisable(true);
    }
    listUpdated = false;
  }

  private void playAction(BookmarkData data) {
    data.getDatenFilmOptional().ifPresent(film -> {
      Daten.getInstance().getStarterClass().urlMitProgrammStarten(Daten.listePset.getPsetAbspielen(), film, "");
      tbBookmarks.getSelectionModel().clearSelection(); // re-select to trigger UI update
      tbBookmarks.getSelectionModel().select(data);
    });
  }

  /**
   * Trigger Download of movie   (mirror fucntionality of FilmGUI)
   * @param data movie object to be used for download
   * Note: Due to mixture of JavaFX and Swing the windows do not arrange properly,
   *       workaround is to hide bookmark window during processing
   */
  private void loadAction(@NotNull BookmarkData data) {
    Optional<DatenFilm> datenFilm = Optional.ofNullable(data.getDatenFilm());
    final var daten = Daten.getInstance();

    refresh();
    datenFilm.ifPresent(film -> {
      DatenDownload previouslyCreatedDownload = daten.getListeDownloads().getDownloadUrlFilm(film.getUrlNormalQuality());
      if (previouslyCreatedDownload == null) {
        createDownload(film);
      }
      else
      {
        ButtonType yes = new ButtonType("Ja", ButtonBar.ButtonData.OK_DONE);
        ButtonType no = new ButtonType("Nein", ButtonBar.ButtonData.CANCEL_CLOSE);
        Alert alert = new Alert(Alert.AlertType.WARNING,
                "Ein Download für den Film existiert bereits.\nNochmal anlegen?",
                yes, no);
        alert.initOwner(stage);
        alert.setTitle("Merkliste");
        alert.showAndWait().filter(response -> response == ButtonType.OK)
                .ifPresent(_ -> createDownload(film));
      }
    });
  }

  private void createDownload(DatenFilm film) {
    stage.hide();

    SwingUtilities.invokeLater(() -> { // swing dialogs must be called from EDT!!
      final var pSet = Daten.listePset.getListeSpeichern().getFirst();
      var dlg = new DialogAddDownload(MediathekGui.ui(), film, pSet, Optional.empty());
      dlg.setVisible(true);
      showStage();
    });
  }

  private void showStage() {
    //this may be called from swing EDT!
    Platform.runLater(() -> {
      stage.show();
      stage.toFront();
      stage.requestFocus();
    });
  }

  /**
   * Save Window settings to application configuration
   */
  public void saveSettings() {
    Configuration config = ApplicationConfiguration.getConfiguration();
    // - Window size and position:
    try {
      config.lock(LockMode.WRITE);
      config.setProperty(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".width", (int)stage.getWidth());
      config.setProperty(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".heigth", (int)stage.getHeight());
      config.setProperty(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".location.x", (int)stage.getX());
      config.setProperty(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".location.y", (int)stage.getY());
      // - Column state, width and order:
      String colbase = ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".columns";
      config.setProperty(colbase + ".no", tbBookmarks.getColumns().size());
      int k = 0;
      for (var column: tbBookmarks.getColumns()) {
        k++;
        String colref = colbase + ".col" + k;
        config.setProperty(colref + ".id", column.getId());
        config.setProperty(colref + ".size", (int)column.getWidth());
        config.setProperty(colref + ".visible", column.isVisible());
      }
      // - Button States:
      config.setProperty(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".details", btnShowDetails.isSelected());
      config.setProperty(ApplicationConfiguration.APPLICATION_UI_BOOKMARKLIST + ".divider", btnShowDetails.isSelected()
        ? spSplitPane.getDividerPositions()[0] : divposition);
    }
    catch(Exception e) {
      logger.error("Save Config exception: ", e);
    }
    finally {
      config.unlock(LockMode.WRITE);
    }
  }

  private void setStageEvents() {
    stage.setOnHiding(_ -> {
      if (listUpdated) { // Save pending changes on hiding
        cancelBookmarkSave();
        saveBookMarkList();
        listUpdated = false;
      }
    });
  }

  /**
   * Returns true if the current table selection contains unseen items
   * @return boolean
   *
   * Note: Lifestreams are ignored
   */
  private boolean isUnSeenSelected() {
    boolean unSeen = false;
    for (BookmarkData data: tbBookmarks.getSelectionModel().getSelectedItems()) {
      if (!data.getSeen() && !data.isLiveStream()) {
        unSeen = true;
        break;
      }
    }
    return unSeen;
  }

  /**
   * Returns true if the current table selection contains only livestreams
   * @return boolean
   */
  private boolean onlyLifeStreamSelected() {
    boolean lifestream = true;
    for (BookmarkData data: tbBookmarks.getSelectionModel().getSelectedItems()) {
      if (!data.isLiveStream()) {
        lifestream = false;
        break;
      }
    }
    return lifestream;
  }

  /**
   * Cancel a waiting bookmark save task
   */
  private void cancelBookmarkSave() {
    if (SaveBookmarkTask != null) {
      SaveBookmarkTask.cancel(false);
      SaveBookmarkTask = null;
    }
  }
}