package mediathek.gui.statistics;

import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

public class FilmStatisticsDialog extends FilmStatisticsCoroutineDialog {
    public FilmStatisticsDialog(@NotNull Window owner, @NotNull AbstractAction action) {
        super(owner, action);
    }
}
