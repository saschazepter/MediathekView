/*
 * Copyright (c) 2025 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.javafx.bookmark;

import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ca.odell.glazedlists.swing.DefaultEventTableModel;
import mediathek.config.Daten;
import mediathek.gui.tabs.tab_film.FilmDescriptionPanel;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.datum.DateUtil;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.kordamp.ikonli.fontawesome6.FontAwesomeRegular;
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

public class BookmarkDialog extends JDialog {
    private final FilmDescriptionPanel filmDescriptionPanel = new FilmDescriptionPanel();
    private final JTextArea noteArea = new JTextArea();
    private final JTable table = new JTable();
    private DefaultEventSelectionModel<BookmarkData> selectionModel;

    public BookmarkDialog(Frame owner) {
        super(owner);
        setTitle("Merkliste verwalten");
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(800, 400);

        setupToolBar();

        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        setupNoteArea();
        getContentPane().add(createTabbedPane(), BorderLayout.SOUTH);

        setupTable();
    }

    private void setupNoteArea() {
        noteArea.setLineWrap(true);
        noteArea.setWrapStyleWord(true);
        noteArea.setEditable(false);
    }

    private JTabbedPane createTabbedPane() {
        JTabbedPane tabbedPane = new JTabbedPane(JTabbedPane.TOP);
        tabbedPane.addTab("Beschreibung", filmDescriptionPanel);
        JPanel notePanel = new JPanel(new BorderLayout());
        notePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        notePanel.add(noteArea, BorderLayout.CENTER);
        tabbedPane.addTab("Notizen", notePanel);
        return tabbedPane;
    }

    private void setupTable() {
        ObservableElementList.Connector<BookmarkData> personConnector = GlazedLists.beanConnector(BookmarkData.class);
        var observedBookmarks =
                new ObservableElementList<>(Daten.getInstance().getListeBookmarkList().getEventList(), personConnector);

        var tableFormat = GlazedLists.tableFormat(new String[]{"seen", "sender", "thema", "title", "dauer", "sendedatum", "AvailableUntil", "NormalQualityUrl", "note", "filmHashCode", "BookmarkAdded"},
                new String[]{"Gesehen", "Sender", "Thema", "Titel", "Dauer", "Sendedatum", "Verfügbar bis", "URL", "Notiz", "Hash Code", "hinzugefügt am"});
        var model = new DefaultEventTableModel<>(observedBookmarks, tableFormat);

        selectionModel = new DefaultEventSelectionModel<>(observedBookmarks);
        selectionModel.addListSelectionListener(l -> {
            if (!l.getValueIsAdjusting()) {
                updateInfoTabs();
            }
        });

        table.setModel(model);
        table.setSelectionModel(selectionModel);
        setupCellRenderers();

        //table.getColumnModel().removeColumn(table.getColumnModel().getColumn(8));
    }

    private void setupCellRenderers() {
        var columnModel = table.getColumnModel();
        //seen column
        columnModel.getColumn(0).setCellRenderer(new SeenCellRenderer());
        //sender column
        columnModel.getColumn(1).setCellRenderer(new CenteredCellRenderer());
        // dauer column
        columnModel.getColumn(4).setCellRenderer(new FilmLengthCellRenderer());
        //sendedatum column
        columnModel.getColumn(5).setCellRenderer(new CenteredCellRenderer());
        columnModel.getColumn(8).setCellRenderer(new NoteCellRenderer());
        //hinzugefügt am Column
        columnModel.getColumn(10).setCellRenderer(new AddedAtCellRenderer());
    }

    private void setupToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        JButton setNoteButton = new JButton();
        setNoteButton.setToolTipText("Notiz hinzufügen");
        setNoteButton.setIcon(IconUtils.toolbarIcon(FontAwesomeRegular.EDIT));
        setNoteButton.addActionListener(_ -> {
            if (!selectionModel.isSelectionEmpty()) {
                var selectedPeople = selectionModel.getSelected();
                for (var bookmark : selectedPeople) {
                    bookmark.setNote("Hello World22");
                }
                Daten.getInstance().getListeBookmarkList().saveToFile();
                updateInfoTabs();
            }
        });
        toolBar.add(setNoteButton);

        JButton removeNoteButton = new JButton();
        removeNoteButton.setToolTipText("Notiz entfernen");
        removeNoteButton.setIcon(IconUtils.toolbarIcon(FontAwesomeSolid.ERASER));
        removeNoteButton.addActionListener(_ -> {
            if (!selectionModel.isSelectionEmpty()) {
                var selectedPeople = selectionModel.getSelected();
                for (var bookmark : selectedPeople) {
                    bookmark.setNote(null);
                }
                Daten.getInstance().getListeBookmarkList().saveToFile();
                updateInfoTabs();
            }
        });
        toolBar.add(removeNoteButton);

        toolBar.addSeparator();
        JButton deleteEntryButton = new JButton();
        deleteEntryButton.setToolTipText("Eintrag löschen");
        deleteEntryButton.setIcon(IconUtils.toolbarIcon(FontAwesomeRegular.TRASH_ALT));
        deleteEntryButton.addActionListener(_ -> deleteBookmarkSelection());
        toolBar.add(deleteEntryButton);

        getContentPane().add(toolBar, BorderLayout.NORTH);
    }

    private void deleteBookmarkSelection() {
        if (!selectionModel.isSelectionEmpty()) {
            var bookmarkList = Daten.getInstance().getListeBookmarkList();
            //we need to make a copy otherwise the selection list will get modified during deletion
            //and will fail to delete all bookmarks
            var list = new ArrayList<>(selectionModel.getSelected());
            //System.out.println("SIZE: " + list.size());
            for (var bookmark : list) {
                //System.out.println("source bookmark: " + bookmark.getFilmHashCode());
                bookmarkList.removeBookmark(bookmark);
            }
            bookmarkList.saveToFile();
        }
        SwingUtilities.invokeLater(() -> MediathekGui.ui().tabFilme.repaint());
    }

    private void updateInfoTabs() {
        var selectedBookmarks = selectionModel.getSelected();
        if (selectedBookmarks.size() == 1) {
            var bookmark = selectedBookmarks.getFirst();
            bookmark.getDatenFilmOptional()
                    .ifPresentOrElse(film -> {
                                filmDescriptionPanel.setCurrentFilm(film);
                                noteArea.setText(Objects.requireNonNullElse(bookmark.getNote(), ""));
                            },
                            () -> {
                                filmDescriptionPanel.setCurrentFilm(null);
                                noteArea.setText("");
                            });
        }
        else {
            filmDescriptionPanel.setCurrentFilm(null);
            noteArea.setText("");
        }
    }

    static class NoteCellRenderer extends JPanel implements TableCellRenderer {
        protected final JCheckBox checkBox = new JCheckBox();

        public NoteCellRenderer() {
            setLayout(new BorderLayout());
            checkBox.setHorizontalAlignment(SwingConstants.CENTER);
            add(checkBox, BorderLayout.CENTER);
        }

        protected void performSelectionDrawing(JTable table, boolean isSelected, int row) {
            if (isSelected) {
                setForeground(table.getSelectionForeground());
                setBackground(table.getSelectionBackground());
            }
            else {
                Color background = table.getBackground();
                if (background == null || background instanceof UIResource) {
                    Color alternateColor = UIManager.getColor("Table.alternateRowColor");
                    if (alternateColor != null && row % 2 != 0) {
                        background = alternateColor;
                    }
                }
                setForeground(table.getForeground());
                setBackground(background);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table == null) {
                return this;
            }

            performSelectionDrawing(table, isSelected, row);

            checkBox.setSelected(value != null);
            return this;
        }
    }

    static class SeenCellRenderer extends NoteCellRenderer implements TableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (table == null) {
                return this;
            }

            performSelectionDrawing(table, isSelected, row);

            boolean seen = (boolean) value;
            checkBox.setSelected(seen);
            return this;
        }
    }

    static class FilmLengthCellRenderer extends CenteredCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            long length = (int) value;
            if (length >= 0) {
                var duration = TimeUnit.MILLISECONDS.convert(length, TimeUnit.SECONDS);
                var durationStr = DurationFormatUtils.formatDuration(duration, "HH:mm:ss", true);
                setText(durationStr);
            }
            else
                setText(null);
            return this;
        }
    }

    static class AddedAtCellRenderer extends CenteredCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            var date = (LocalDate) value;
            if (date != null) {
                setText(date.format(DateUtil.FORMATTER));
            }
            return this;
        }
    }

    static class CenteredCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            setHorizontalAlignment(JLabel.CENTER);
            return this;
        }
    }
}
