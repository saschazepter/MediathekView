package mediathek.javafx.filterpanel;

import com.formdev.flatlaf.FlatLaf;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.scene.Scene;
import mediathek.config.Daten;
import mediathek.filmeSuchen.ListenerFilmeLaden;
import mediathek.filmeSuchen.ListenerFilmeLadenEvent;
import mediathek.gui.messages.DarkModeChangeEvent;
import mediathek.gui.messages.TableModelChangeEvent;
import mediathek.tool.ApplicationConfiguration;
import mediathek.tool.MessageBus;
import net.engio.mbassy.listener.Handler;

import javax.swing.*;
import java.awt.*;

public class OldSwingJavaFxFilterDialog extends JDialog {
    private final JFXPanel fxPanel = new JFXPanel();
    private Scene scene;

    public OldSwingJavaFxFilterDialog(Frame owner, CommonViewSettingsPane commonViewSettingsPane) {
        super(owner);

        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setTitle("JavaFX/Swing Filter");
        setType(Type.UTILITY);
        setContentPane(fxPanel);
        Platform.runLater(() -> {
            scene = new Scene(commonViewSettingsPane);
            setupJavaFxDarkMode();
            fxPanel.setScene(scene);
            SwingUtilities.invokeLater(() -> {
                pack();
                restoreDialogVisibility();
            });
        });

        MessageBus.getMessageBus().subscribe(this);

        Daten.getInstance().getFilmeLaden().addAdListener(new ListenerFilmeLaden() {
            @Override
            public void start(ListenerFilmeLadenEvent event) {
                final boolean enabled = false;
                setEnabled(enabled);
                fxPanel.setEnabled(enabled);
            }

            @Override
            public void fertig(ListenerFilmeLadenEvent event) {
                final boolean enabled = true;
                setEnabled(enabled);
                fxPanel.setEnabled(enabled);
            }
        });
    }

    private void setupJavaFxDarkMode() {
        if (FlatLaf.isLafDark()) {
            var res = getClass().getResource("/mediathek/res/css/javafx-dark-mode/style.css");
            assert res != null;
            scene.getStylesheets().add(res.toExternalForm());
        }
        else
            scene.getStylesheets().clear();
    }

    @Handler
    private void handleDarkModeChange(DarkModeChangeEvent e) {
        Platform.runLater(this::setupJavaFxDarkMode);
    }

    @Handler
    private void handleTableModelChangeEvent(TableModelChangeEvent e) {
        SwingUtilities.invokeLater(() -> setEnabled(!e.active));
    }

    private void restoreDialogVisibility() {
        var config = ApplicationConfiguration.getConfiguration();
        final boolean visible = config.getBoolean(ApplicationConfiguration.FilterDialog.VISIBLE, false);
        setVisible(visible);
    }
}
