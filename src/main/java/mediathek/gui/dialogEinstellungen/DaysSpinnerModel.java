package mediathek.gui.dialogEinstellungen;

import javax.swing.*;
import java.util.Arrays;

public class DaysSpinnerModel extends SpinnerListModel {
    public DaysSpinnerModel() {
        super(Arrays.asList("Alle", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10",
                "12", "14", "16", "18", "20", "25", "30", "60", "90", "180", "365"));
    }
}
