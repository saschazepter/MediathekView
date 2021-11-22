package mediathek.tool.table;

import mediathek.daten.DatenProg;
import mediathek.tool.models.TModel;

public class MVProgTable extends MVTable {

    @Override
    protected void setupTableType() {
        maxSpalten = DatenProg.MAX_ELEM;
        spaltenAnzeigen = getSpaltenEinAus(DatenProg.spaltenAnzeigen, DatenProg.MAX_ELEM);

        setModel(new TModel(new Object[][]{}, DatenProg.COLUMN_NAMES));
    }

    @Override
    protected void spaltenAusschalten() {
        //do nothing
    }
}
