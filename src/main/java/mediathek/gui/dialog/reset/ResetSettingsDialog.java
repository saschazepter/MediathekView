package mediathek.gui.dialog.reset;

import mediathek.gui.dialog.StandardCloseDialog;

import javax.swing.*;
import java.awt.*;

public class ResetSettingsDialog extends StandardCloseDialog {
    public ResetSettingsDialog(Frame owner) {
        super(owner, "Programm zurücksetzen", true);
        setResizable(false);
    }

    @Override
    public JComponent createContentPanel() {
        return new ResetSettingsPanel(null);
    }
}
