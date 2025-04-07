package mediathek.javafx.filterpanel;

import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import mediathek.gui.messages.TableModelChangeEvent;
import mediathek.tool.MessageBus;
import net.engio.mbassy.listener.Handler;
import org.apache.commons.lang3.SystemUtils;

public class CommonViewSettingsPane extends VBox {
    public final ThemaComboBox themaComboBox = new ThemaComboBox();
    public final FilmLenghtSliderNode filmLengthSliderNode = new FilmLenghtSliderNode();
    public final SenderBoxNode senderCheckList = new SenderBoxNode();
    public final CheckBox cbDontShowAudioVersions = new CheckBox("Hörfassungen ausblenden");
    public final CheckBox cbDontShowGebaerdensprache = new CheckBox("Gebärdensprache nicht anzeigen");
    public final CheckBox cbDontShowDuplicates = new CheckBox("Duplikate nicht anzeigen");
    public final CheckBox cbDontShowTrailers = new CheckBox("Trailer/Teaser/Vorschau nicht anzeigen");
    public final CheckBox cbShowUnseenOnly = new CheckBox("Gesehene Filme nicht anzeigen");
    public final CheckBox cbDontShowAbos = new CheckBox("Abos nicht anzeigen");
    public final CheckBox cbShowOnlyLivestreams = new CheckBox("Nur Livestream anzeigen");

    private Pane createSenderList() {
        senderCheckList.setPrefHeight(150d);
        senderCheckList.setMinHeight(100d);
        VBox.setVgrow(senderCheckList, Priority.ALWAYS);
        var titledPane = new VBox();
        titledPane.getChildren().addAll(new Label("Sender:"),
                senderCheckList);
        VBox.setVgrow(titledPane, Priority.ALWAYS);

        return titledPane;
    }

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
                cbShowOnlyLivestreams,
                new Separator(),
                cbShowUnseenOnly,
                cbDontShowAbos,
                cbDontShowGebaerdensprache,
                cbDontShowTrailers,
                cbDontShowAudioVersions,
                cbDontShowDuplicates,
                new Separator(),
                createSenderList(),
                new Separator(),
                createThemaBox(),
                new Separator(),
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
                    cbShowOnlyLivestreams.setDisable(disable);
                    cbShowUnseenOnly.setDisable(disable);
                    cbDontShowAbos.setDisable(disable);
                    cbDontShowGebaerdensprache.setDisable(disable);
                    cbDontShowTrailers.setDisable(disable);
                    cbDontShowAudioVersions.setDisable(disable);
                    cbDontShowDuplicates.setDisable(disable);
                    senderCheckList.setDisable(disable);
                    themaComboBox.setDisable(disable);
                    filmLengthSliderNode.setDisable(disable);
                });
    }
}
