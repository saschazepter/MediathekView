package mediathek.gui.actions;

import mediathek.config.Daten;
import mediathek.config.Konstanten;
import mediathek.daten.DatenPset;
import mediathek.mainwindow.MediathekGui;
import mediathek.swing.IconUtils;
import mediathek.tool.GuiFunktionen;
import org.apache.commons.lang3.SystemUtils;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.function.Consumer;

public class PlayFilmAction extends AbstractAction {
    private final Consumer<DatenPset> startFilm;

    public PlayFilmAction(Consumer<DatenPset> startFilm) {
        this.startFilm = startFilm;
        putValue(Action.NAME, "Film abspielen");
        putValue(Action.SHORT_DESCRIPTION, "Film abspielen");
        putValue(Action.SMALL_ICON, IconUtils.toolbarIcon(FontAwesomeSolid.PLAY));
        KeyStroke keyStroke;
        if (SystemUtils.IS_OS_MAC_OSX)
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_F6, GuiFunktionen.getPlatformControlKey());
        else
            keyStroke = KeyStroke.getKeyStroke(KeyEvent.VK_P, GuiFunktionen.getPlatformControlKey());
        putValue(Action.ACCELERATOR_KEY, keyStroke);
    }

    @Override
    public synchronized void actionPerformed(ActionEvent e) {
        DatenPset pset = Daten.getInstance().getListePset().getPsetAbspielen();
        if (pset != null) {
            startFilm.accept(pset);
        } else {
            JOptionPane.showMessageDialog(MediathekGui.ui(),
                    "Es wurde kein Videoplayer eingerichtet.\n" +
                            "Bitte legen Sie diesen unter \"Einstellungen->Set bearbeiten\" fest.",
                    Konstanten.PROGRAMMNAME,
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
