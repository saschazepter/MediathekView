package mediathek.javafx.bookmark;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.net.URL;
import java.util.ResourceBundle;


/**
 * Dialog to set expiry date and notes for bookmarked movies
 * <p>
 * includes search for movies's expiry date on webpage
 *
 * @author Klaus Wich <klaus.wich@aim.com>
 */

public class BookmarkNoteDialogController implements Initializable {
    @FXML
    protected Button SaveButton;
    @FXML
    protected Button CancelButton;
    protected Stage dlgstage;
    protected boolean datachanged;
    @FXML
    private TextArea fxNote;
    @FXML
    private ProgressIndicator fxProgress;
    @FXML
    private Label fxStatus;
    private BookmarkData data;

    @Override
    public void initialize(URL arg0, ResourceBundle arg1) {
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

    public final boolean SetandShow(Stage dlgstage, BookmarkData data) {
        this.dlgstage = dlgstage;
        this.data = data;
        this.dlgstage.setTitle(data.getNote() != null ? "Anmerkungen ändern" : "Neue Anmerkungen");
        fxNote.setText(data.getNote() != null ? data.getNote() : "");

        SaveButton.setDisable(false);
        // Display the Dialog and wait
        this.dlgstage.showAndWait();
        return datachanged;
    }
}
