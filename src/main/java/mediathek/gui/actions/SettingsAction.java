package mediathek.gui.actions;

import mediathek.gui.dialogEinstellungen.DialogEinstellungen;
import mediathek.mainwindow.MediathekGui;
import mediathek.swing.IconUtils;
import org.apache.commons.lang3.SystemUtils;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class SettingsAction extends AbstractAction {
    private DialogEinstellungen dlg;

    public SettingsAction() {
        putValue(Action.NAME, "Einstellungen...");
        if (!SystemUtils.IS_OS_MAC_OSX)
            putValue(Action.ACCELERATOR_KEY, KeyStroke.getKeyStroke(KeyEvent.VK_F4, 0));
        putValue(Action.SMALL_ICON, IconUtils.windowBarSpecificToolbarIcon(FontAwesomeSolid.COGS));
        putValue(Action.SHORT_DESCRIPTION, "Einstellungen öffnen");
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        if (dlg == null) {
            dlg = new DialogEinstellungen(MediathekGui.ui());
        }
        dlg.setVisible(true);
        if (!SystemUtils.IS_OS_LINUX)
            dlg.toFront();
    }
}
