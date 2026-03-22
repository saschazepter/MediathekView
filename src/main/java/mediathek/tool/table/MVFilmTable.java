package mediathek.tool.table;

import mediathek.config.MVColor;
import mediathek.config.MVConfig;
import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.DatenFilm;
import mediathek.gui.tabs.tab_film.GuiFilme;
import mediathek.tool.FilmSize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.*;
import java.util.List;

public class MVFilmTable extends MVTable {
    private static final Logger logger = LogManager.getLogger();
    private final List<Color> bgList = new ArrayList<>();
    private MyRowSorter<TableModel> sorter;

    public MVFilmTable() {
        super(DatenFilm.MAX_ELEM, GuiFilme.VISIBLE_COLUMNS,
                Optional.of(MVConfig.Configs.SYSTEM_TAB_FILME_ICON_ANZEIGEN),
                Optional.of(MVConfig.Configs.SYSTEM_TAB_FILME_ICON_KLEIN),
                Optional.of(MVConfig.Configs.SYSTEM_EIGENSCHAFTEN_TABELLE_FILME));

        setAutoCreateRowSorter(false);
        addPropertyChangeListener("model", evt -> {
            //we need to setup sorter later as the model is invalid at ctor point...
            var model = (TableModel) evt.getNewValue();
            if (sorter == null) {
                sorter = new MyRowSorter<>(model);
                sorter.setModel(model);
                setRowSorter(sorter);
            }
            else
                sorter.setModel(model);
        });
    }

    protected static Color blend(Collection<Color> colors) {
        if (colors == null || colors.isEmpty()) {
            return null;
        }

        int a = 0;
        int r = 0;
        int g = 0;
        int b = 0;

        for (Color color : colors) {
            a += color.getAlpha();
            r += color.getRed();
            g += color.getGreen();
            b += color.getBlue();
        }

        int size = colors.size();
        return new Color(r / size, g / size, b / size, a / size);
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        var selected = isRowSelected(row);

        var component = super.prepareRenderer(renderer, row, column);
        if (!selected) {
            component.setBackground(defaultRowBackground(row));
            var film = (DatenFilm) getModel().getValueAt(convertRowIndexToModel(row), DatenFilm.FILM_REF);
            applyNewColorSetting(component, film);
            applyColorSettings(component, film);
        }

        return component;
    }

    private void applyNewColorSetting(Component c, @NotNull DatenFilm datenFilm) {
        if (datenFilm.isNew()) {
            c.setForeground(MVColor.getNewColor());
        }
    }

    private void applyColorSettings(Component c, @NotNull DatenFilm datenFilm) {
        bgList.clear();
        bgList.add(c.getBackground());

        if (SeenHistoryController.hasBeenSeenFromSharedCache(datenFilm)) {
            bgList.add(MVColor.FILM_HISTORY.color);
        }
        if (datenFilm.isBookmarked()) {
            bgList.add(MVColor.FILM_BOOKMARKED.color);
        }
        if (datenFilm.isDuplicate()) {
            bgList.add(MVColor.FILM_DUPLICATE.color);
        }

        if (bgList.size() >= 2)
            c.setBackground(blend(bgList));
        else
            c.setBackground(bgList.getFirst());
    }

    @Override
    public String getToolTipText(MouseEvent e) {
        var p = e.getPoint(); // MouseEvent
        final int viewColumn = columnAtPoint(p);
        final int modelColumnIndex = convertColumnIndexToModel(viewColumn);

        //only show title as tooltip for TITEL column...
        if (modelColumnIndex != DatenFilm.FILM_TITEL)
            return super.getToolTipText(e);

        String toolTipText = null;
        final int viewRow = rowAtPoint(p);
        var comp = prepareRenderer(getCellRenderer(viewRow, viewColumn), viewRow, viewColumn);
        var bounds = getCellRect(viewRow, viewColumn, false);


        try {
            //comment row, exclude heading
            if (comp.getPreferredSize().width > bounds.width) {
                final int modelRowIndex = convertRowIndexToModel(viewRow);
                final DatenFilm datenFilm = (DatenFilm) getModel().getValueAt(modelRowIndex, DatenFilm.FILM_REF);

                toolTipText = datenFilm.getTitle();
            }
        }
        catch (RuntimeException ignored) {
            //catch null pointer exception if mouse is over an empty line
        }

        return toolTipText;
    }

    private void resetFilmeTab(int i) {
        //logger.debug("resetFilmeTab()");

        reihe[i] = i;
        breite[i] = 200;
        switch (i) {
            case DatenFilm.FILM_NR -> breite[i] = 75;
            case DatenFilm.FILM_TITEL -> breite[i] = 300;
            case DatenFilm.FILM_DATUM, DatenFilm.FILM_ZEIT, DatenFilm.FILM_SENDER, DatenFilm.FILM_GROESSE,
                 DatenFilm.FILM_DAUER, DatenFilm.FILM_GEO -> breite[i] = 100;
            case DatenFilm.FILM_URL -> breite[i] = 500;
            case DatenFilm.FILM_ABSPIELEN, DatenFilm.FILM_AUFZEICHNEN, DatenFilm.FILM_MERKEN -> breite[i] = 20;
            case DatenFilm.FILM_HD, DatenFilm.FILM_UT -> breite[i] = 50;
        }
    }

    @Override
    public void resetTabelle() {
        //logger.debug("resetTabelle()");

        for (int i = 0; i < maxSpalten; ++i) {
            resetFilmeTab(i);
        }

        getRowSorter().setSortKeys(null); // empty sort keys
        spaltenAusschalten();
        setSpaltenEinAus(breite);
        setSpalten();
        calculateRowHeight();
    }

    @Override
    protected void spaltenAusschalten() {
        // do nothing here
    }

    @Override
    public void getSpalten() {
        //logger.debug("getSpalten()");

        // Einstellungen der Tabelle merken
        saveSelectedTableRows();

        for (int i = 0; i < reihe.length && i < getModel().getColumnCount(); ++i) {
            reihe[i] = convertColumnIndexToModel(i);
        }

        for (int i = 0; i < breite.length && i < getModel().getColumnCount(); ++i) {
            breite[i] = getColumnModel().getColumn(convertColumnIndexToView(i)).getWidth();
        }

        // save sortKeys
        var rowSorter = getRowSorter();
        if (rowSorter != null) {
            listeSortKeys = rowSorter.getSortKeys();
        }
        else {
            listeSortKeys = null;
        }
    }

    private void reorderColumns() {
        final TableColumnModel model = getColumnModel();
        var numCols = getColumnCount();
        for (int i = 0; i < reihe.length && i < numCols; ++i) {
            //move only when there are changes...
            if (reihe[i] != i)
                model.moveColumn(convertColumnIndexToView(reihe[i]), i);
        }
    }

    private void restoreSortKeys() {
        if (listeSortKeys != null) {
            var rowSorter = getRowSorter();
            var tblSortKeys = rowSorter.getSortKeys();
            if (!(listeSortKeys == tblSortKeys)) {
                if (!listeSortKeys.isEmpty()) {
                    rowSorter.setSortKeys(listeSortKeys);
                }
            }
        }
    }

    /**
     * Setzt die gemerkte Position der Spalten in der Tabelle wieder.
     * Ziemlich ineffizient!
     */
    @Override
    public void setSpalten() {
        //logger.debug("setSpalten()");
        try {
            changeInternalColumnWidths();

            changeTableModelColumnWidths();

            reorderColumns();

            restoreSortKeys();

            restoreSelectedTableRows();

            validate();
        }
        catch (Exception ex) {
            logger.error("setSpalten", ex);
        }
    }

    static class MyRowSorter<M extends TableModel> extends TableRowSorter<M> {
        public MyRowSorter(M model) {
            super(model);
        }

        @Override
        public void setModel(M model) {
            super.setModel(model);

            //must be set after each model change
            // do not sort buttons
            setSortable(DatenFilm.FILM_ABSPIELEN, false);
            setSortable(DatenFilm.FILM_AUFZEICHNEN, false);
            setSortable(DatenFilm.FILM_GEO, false);
            setSortable(DatenFilm.FILM_MERKEN, false);

            //compare to FilmSize->int instead of String
            setComparator(DatenFilm.FILM_GROESSE, (Comparator<FilmSize>) FilmSize::compareTo);
            // deactivate german collator used in DatenFilm as it slows down sorting as hell...
            setComparator(DatenFilm.FILM_SENDER, (Comparator<String>) String::compareTo);
            setComparator(DatenFilm.FILM_ZEIT, (Comparator<String>) String::compareTo);
            setComparator(DatenFilm.FILM_URL, (Comparator<String>) String::compareTo);
            setComparator(DatenFilm.FILM_DAUER, Comparator.naturalOrder());
        }

        @Override
        public void setSortKeys(List<? extends SortKey> sortKeys) {
            // MV config stores only ONE sort key
            // here we make sure that only one will be set on the table...
            if (sortKeys != null) {
                while (sortKeys.size() > 1)
                    sortKeys.remove(1);
            }
            super.setSortKeys(sortKeys);
        }
    }
}
