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
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.datum.DateUtil;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.jdesktop.swingx.VerticalLayout;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

public class BookmarkDialog extends JDialog {
    private DefaultEventSelectionModel<BookmarkData> selectionModel;
    private final JButton updateTableButton = new JButton("Set Note");
    private final JButton removeTableButton = new JButton("Remove Note");
    private final JButton deleteEntryButton = new JButton("Delete Entry");

    public BookmarkDialog(Frame owner) {
        super(owner);
        setTitle("Merkliste verwalten");
        setModal(false);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(400, 200);

        JTable table = new JTable();
        JScrollPane scrollPane = new JScrollPane(table);
        getContentPane().add(scrollPane, BorderLayout.CENTER);

        updateTableButton.addActionListener(_ -> {
            if (!selectionModel.isSelectionEmpty()) {
                var selectedPeople = selectionModel.getSelected();
                for (var bookmark : selectedPeople) {
                    bookmark.setNote("Hello World22");
                }
                Daten.getInstance().getListeBookmarkList().saveToFile();
            }
        });
        removeTableButton.addActionListener(_ -> {
            if (!selectionModel.isSelectionEmpty()) {
                var selectedPeople = selectionModel.getSelected();
                for (var bookmark : selectedPeople) {
                    bookmark.setNote(null);
                }
                Daten.getInstance().getListeBookmarkList().saveToFile();
            }
        });
        deleteEntryButton.addActionListener(_ -> {
            if (!selectionModel.isSelectionEmpty()) {
                var bookmarkList = Daten.getInstance().getListeBookmarkList();
                //we need to make a copy otherwise the selection list will get modified during deletion
                //and will fail to delete all bookmarks
                ArrayList<BookmarkData> list = new ArrayList<>(selectionModel.getSelected());
                System.out.println("SIZE: " + list.size());
                for (var bookmark : list) {
                    System.out.println("source bookmark: " + bookmark.getFilmHashCode());
                    bookmarkList.removeBookmark(bookmark);
                }
                bookmarkList.saveToFile();
            }
            SwingUtilities.invokeLater(() -> MediathekGui.ui().tabFilme.repaint());

        });

        JPanel btnPanel = new JPanel(new VerticalLayout());
        btnPanel.add(updateTableButton);
        btnPanel.add(removeTableButton);
        btnPanel.add(deleteEntryButton);
        getContentPane().add(btnPanel, BorderLayout.SOUTH);

        ObservableElementList.Connector<BookmarkData> personConnector = GlazedLists.beanConnector(BookmarkData.class);
        var observedBookmarks =
                new ObservableElementList<>(Daten.getInstance().getListeBookmarkList().getEventList(), personConnector);

        var tableFormat = GlazedLists.tableFormat(new String[]{"sender", "thema", "title", "dauer", "sendedatum", "AvailableUntil", "url", "note", "filmHashCode", "BookmarkAdded"},
                new String[]{"Sender", "Thema", "Titel", "Dauer", "Sendedatum", "Verfügbar bis", "URL", "Notiz", "Hash Code", "hinzugefügt am"});
        var model = new DefaultEventTableModel<>(observedBookmarks, tableFormat);
        selectionModel = new DefaultEventSelectionModel<>(observedBookmarks);
        /*selectionModel.addListSelectionListener(l -> {
            if (!l.getValueIsAdjusting()) {
                var selectedBookmarks = selectionModel.getSelected();
                if (selectedBookmarks.size() > 1) {
                    System.out.println("TOO MANY SELECTIONS");
                }
                else if (selectedBookmarks.size() == 1) {
                    System.out.println("Showing info");
                    System.out.println(selectedBookmarks.getFirst().getFilmHashCode());
                }
            }
        });*/

        table.setModel(model);
        table.setSelectionModel(selectionModel);
        /*
         */
        var columnModel = table.getColumnModel();
        // dauer column
        columnModel.getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                long length = (int) value;
                var duration = TimeUnit.MILLISECONDS.convert(length, TimeUnit.SECONDS);
                var durationStr = DurationFormatUtils.formatDuration(duration, "HH:mm:ss", true);
                setText(durationStr);
                setHorizontalAlignment(JLabel.CENTER);
                return this;
            }
        });
        //hinzugefügt am Column
        columnModel.getColumn(9).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                var date = (LocalDate) value;
                setText(date.format(DateUtil.FORMATTER));
                return this;
            }
        });
        /*table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {

            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                var elem = ((DefaultEventTableModel<BookmarkData>)table.getModel()).getElementAt(row);
                setText(value == null ? "null" : elem.getDatenFilm().getTitle());
                return this;
            }
        });*/
    }
}
