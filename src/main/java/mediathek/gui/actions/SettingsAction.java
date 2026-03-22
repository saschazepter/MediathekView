package mediathek.gui.actions;

import mediathek.mainwindow.MediathekGui;
import mediathek.swing.IconUtils;
import org.apache.commons.lang3.SystemUtils;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class SettingsAction extends AbstractAction {
    private final MediathekGui mediathekGui;

    public SettingsAction(MediathekGui mediathekGui) {
        this.mediathekGui = mediathekGui;

        putValue(Action.NAME, "Einstellungen...");
        if (!SystemUtils.IS_OS_MAC_OSX)
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        putValue(Action.SMALL_ICON, IconUtils.windowBarSpecificToolbarIcon(FontAwesomeSolid.COGS));
        putValue(Action.SHORT_DESCRIPTION, "Einstellungen öffnen");
    }
    @Override
    public void actionPerformed(ActionEvent e) {
        var dialog = mediathekGui.getSettingsDialog();
        dialog.setVisible(true);
        if (!SystemUtils.IS_OS_LINUX)
            dialog.toFront();
    }
}
