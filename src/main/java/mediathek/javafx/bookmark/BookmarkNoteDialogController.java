package mediathek.javafx.bookmark;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;


/**
 * Dialog to set expiry date and notes for bookmarked movies
 * <p>
 * includes search for movies's expiry date on webpage
 *
 * @author Klaus Wich <klaus.wich@aim.com>
 */

public class BookmarkNoteDialogController implements Initializable {
    /**
     * Try to retrieve the expiry date from the associated webpage
     */
    private final DateTimeFormatter dateformatter;
    @FXML
    protected Button SaveButton;
    @FXML
    protected Button CancelButton;
    protected Stage dlgstage;
    protected boolean datachanged;
    @FXML
    private DatePicker fxDate;
    @FXML
    private TextArea fxNote;
    @FXML
    private ProgressIndicator fxProgress;
    @FXML
    private Label fxStatus;
    @FXML
    private Label fxExpiry;
    private BookmarkData data;

    public BookmarkNoteDialogController() {
        dateformatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    }

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
        fxDate.setOnKeyTyped((var _) -> handleChange());
        fxDate.setOnMouseClicked((var _) -> handleChange());
        fxDate.getEditor().setOnKeyTyped((var _) -> handleChange());

        ButtonBar.setButtonData(CancelButton, ButtonBar.ButtonData.CANCEL_CLOSE);
        ButtonBar.setButtonData(SaveButton, ButtonBar.ButtonData.OK_DONE);
    }

    @FXML
    protected void handleCancel() {
        datachanged = false;
        dlgstage.hide();
    }

    @FXML
    protected void handleSave() {
        if (!fxNote.getText().equals(data.getNote())) {
            data.setNote(fxNote.getText());
            datachanged = true;
        }

        dlgstage.hide();
    }

    @FXML
    protected void handleChange() {
        boolean isok = Verify();
        SaveButton.setDisable(!isok);
        int idx = fxDate.getEditor().getStyleClass().indexOf("Invalid");
        if (isok && idx > -1) {
            fxDate.getEditor().getStyleClass().remove("Invalid");
        } else if (!isok && idx == -1) {
            fxDate.getEditor().getStyleClass().add("Invalid");
        }
    }

    public final boolean SetandShow(Stage dlgstage, BookmarkData data) {
        this.dlgstage = dlgstage;
        this.data = data;
        this.dlgstage.setTitle(data.getNote() != null ? "Anmerkungen ändern" : "Neue Anmerkungen");
        fxNote.setText(data.getNote() != null ? data.getNote() : "");
        if (data.isLiveStream()) { // For live stream disable expiry handling
            fxExpiry.setDisable(true);
            fxDate.setDisable(true);
        }
        handleChange();
        // Display the Dialog and wait
        this.dlgstage.showAndWait();
        return datachanged;
    }

    protected boolean Verify() {
        boolean rc = true;
        // Check date format:
        String dv = getDateValue();
        if (dv != null) {
            try {
                LocalDate.parse(dv, dateformatter);
            } catch (Exception e) {
                rc = false;
            }
        }
        return rc;
    }

    /**
     * Get date value or null
     *
     * @return String
     */
    private String getDateValue() {
        String dv = fxDate.getEditor().getText();
        if (dv != null && dv.isEmpty()) {
            dv = null;
        }
        return dv;
    }

}
