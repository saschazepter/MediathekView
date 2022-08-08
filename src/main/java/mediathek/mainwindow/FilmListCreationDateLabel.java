package mediathek.mainwindow;

import mediathek.config.Daten;
import mediathek.daten.FilmListMetaData;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class FilmListCreationDateLabel extends JLabel implements PropertyChangeListener {
    public FilmListCreationDateLabel() {
        var blacklist = Daten.getInstance().getListeFilmeNachBlackList();
        setText(blacklist.getMetaData());
        //works only on blacklist!
        blacklist.addMetaDataChangeListener(this);
    }

    private void setText(@NotNull FilmListMetaData metaData) {
        var text = String.format("Filmliste erstellt: %s Uhr", metaData.getGenerationDateTimeAsString());
        SwingUtilities.invokeLater(() -> setText(text));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        var metaData = (FilmListMetaData) evt.getNewValue();
        setText(metaData);
    }
}
