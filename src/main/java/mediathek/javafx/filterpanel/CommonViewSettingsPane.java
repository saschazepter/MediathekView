package mediathek.javafx.filterpanel;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.layout.*;
import mediathek.gui.messages.TableModelChangeEvent;
import mediathek.tool.MessageBus;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.SystemUtils;

public class CommonViewSettingsPane extends VBox {
    public final ThemaComboBox themaComboBox = new ThemaComboBox();
    public final FilmLenghtSliderNode filmLengthSliderNode = new FilmLenghtSliderNode();

    private Pane createThemaBox() {
        var hbox = new HBox();
        hbox.setSpacing(4d);
        HBox.setHgrow(themaComboBox, Priority.ALWAYS);
        var borderPane = new BorderPane();
        var themaLabel = new Label("Thema:");
        themaLabel.setMinWidth(USE_PREF_SIZE);
        // font size is greater on tested ubuntu linux :(
        if (SystemUtils.IS_OS_LINUX)
            themaLabel.setPrefWidth(50d);
        else
            themaLabel.setPrefWidth(45d);
        borderPane.setCenter(themaLabel);
        hbox.getChildren().addAll(borderPane, themaComboBox);

        return hbox;
    }

    public CommonViewSettingsPane() {
        setPadding(new Insets(5, 5, 5, 5));
        setSpacing(4d);

        getChildren().addAll(
                createThemaBox(),
                filmLengthSliderNode);


        MessageBus.getMessageBus().subscribe(this);
    }

    /**
     * Prevent user from changing filter settings while the swing table model gets updated.
     *
     * @param evt the model event
     */
    @Handler
    private void handleTableModelChangeEvent(TableModelChangeEvent evt) {
        Platform.runLater(
                () -> {
                    final boolean disable = evt.active;
                    themaComboBox.setDisable(disable);
                    filmLengthSliderNode.setDisable(disable);
                });
    }
}
