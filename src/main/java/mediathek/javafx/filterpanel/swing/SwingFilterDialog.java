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

/*
 * Created by JFormDesigner on Sun Apr 06 12:46:21 CEST 2025
 */

package mediathek.javafx.filterpanel.swing;

import com.jidesoft.swing.CheckBoxList;
import com.jidesoft.swing.ComboBoxSearchable;
import mediathek.config.Daten;
import mediathek.filmeSuchen.ListenerFilmeLaden;
import mediathek.filmeSuchen.ListenerFilmeLadenEvent;
import mediathek.gui.messages.ReloadTableDataEvent;
import mediathek.gui.messages.TableModelChangeEvent;
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBox;
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel;
import mediathek.javafx.filterpanel.swing.zeitraum.SwingZeitraumSpinner;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.*;
import net.engio.mbassy.listener.Handler;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.sync.LockMode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.util.NoSuchElementException;
import java.util.UUID;

/**
 * @author christianfranzke
 */
public class SwingFilterDialog extends JDialog {
    private static final Logger logger = LogManager.getLogger();
    private final FilterSelectionComboBoxModel filterSelectionComboBoxModel;
    private final ComboBoxSearchable searchable;
    private final Configuration config = ApplicationConfiguration.getConfiguration();
    private final JToggleButton filterToggleButton;
    private final FilterConfiguration filterConfig;

    public static class ToggleVisibilityKeyHandler {
        private static final String TOGGLE_FILTER_VISIBILITY = "toggle_dialog_visibility";
        private final JRootPane rootPane;
        public ToggleVisibilityKeyHandler(JDialog dlg) {
            this.rootPane = dlg.getRootPane();
        }

        public void installHandler(Action action) {
            final var inputMap = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
            inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), TOGGLE_FILTER_VISIBILITY);
            rootPane.getActionMap().put(TOGGLE_FILTER_VISIBILITY, action);
        }
    }

    public SwingFilterDialog(Window owner, @NotNull FilterSelectionComboBoxModel model,
                             @NotNull JToggleButton filterToggleButton,
                             @NotNull FilterConfiguration filterConfig) {
        super(owner);
        this.filterSelectionComboBoxModel = model;
        this.filterToggleButton = filterToggleButton;
        this.filterConfig = filterConfig;

        initComponents();

        setupRenameFilterButton();
        setupDeleteCurrentFilterButton();
        setupResetCurrentFilterButton();
        setupAddNewFilterButton();

        cbShowNewOnly.addActionListener(l -> {
            filterConfig.setShowNewOnly(cbShowNewOnly.isSelected());
            MessageBus.getMessageBus().publish(new ReloadTableDataEvent());
        });
        cbShowBookMarkedOnly.addActionListener(l -> {
            filterConfig.setShowBookMarkedOnly(cbShowBookMarkedOnly.isSelected());
            MessageBus.getMessageBus().publish(new ReloadTableDataEvent());
        });
        cbShowOnlyHq.addActionListener(l -> {
            filterConfig.setShowHighQualityOnly(cbShowOnlyHq.isSelected());
            MessageBus.getMessageBus().publish(new ReloadTableDataEvent());
        });
        cbShowSubtitlesOnly.addActionListener(l -> {
            filterConfig.setShowSubtitlesOnly(cbShowSubtitlesOnly.isSelected());
            MessageBus.getMessageBus().publish(new ReloadTableDataEvent());
        });
        cbShowOnlyLivestreams.addActionListener(l -> {
           filterConfig.setShowLivestreamsOnly(cbShowOnlyLivestreams.isSelected());
           MessageBus.getMessageBus().publish(new ReloadTableDataEvent());
        });


        setupZeitraumSpinner();

        searchable = new ComboBoxSearchable(jcbThema);

        restoreConfigSettings();
        filterSelectionComboBoxModel.addListDataListener(new ListDataListener() {

            @Override
            public void intervalAdded(ListDataEvent e) {
                restoreConfigSettings();
            }

            @Override
            public void intervalRemoved(ListDataEvent e) {
                restoreConfigSettings();
            }

            @Override
            public void contentsChanged(ListDataEvent e) {
                restoreConfigSettings();
            }
        });

        cboxFilterSelection.setMaximumSize(new Dimension(500, 100));

        ToggleVisibilityKeyHandler handler = new ToggleVisibilityKeyHandler(this);
        handler.installHandler(filterToggleButton.getAction());

        restoreWindowSizeFromConfig();
        restoreDialogVisibility();
        addComponentListener(new FilterDialogComponentListener());

        var size = getSize();
        setMinimumSize(size);

        MessageBus.getMessageBus().subscribe(this);

        Daten.getInstance().getFilmeLaden().addAdListener(new ListenerFilmeLaden() {
            @Override
            public void start(ListenerFilmeLadenEvent event) {
                //FIXME here we must manually enable/disable our controls
                setEnabled(false);
            }

            @Override
            public void fertig(ListenerFilmeLadenEvent event) {
                setEnabled(true);
            }
        });
    }

    private void setupZeitraumSpinner() {
        spZeitraum.restoreFilterConfig(filterConfig);
        spZeitraum.installFilterConfigurationChangeListener(filterConfig);
    }

    private void checkDeleteCurrentFilterButtonState() {
        if (filterConfig.getAvailableFilterCount() <= 1) {
            btnDeleteCurrentFilter.setEnabled(false);
        }
    }

    private void restoreConfigSettings() {
        cbShowNewOnly.setSelected(filterConfig.isShowNewOnly());
        cbShowBookMarkedOnly.setSelected(filterConfig.isShowBookMarkedOnly());
        cbShowOnlyHq.setSelected(filterConfig.isShowHighQualityOnly());
        cbShowSubtitlesOnly.setSelected(filterConfig.isShowSubtitlesOnly());
        cbShowOnlyLivestreams.setSelected(filterConfig.isShowLivestreamsOnly());
        /*
        showUnseenOnly.set(filterConfig.isShowUnseenOnly());
        dontShowAbos.set(filterConfig.isDontShowAbos());
        dontShowTrailers.set(filterConfig.isDontShowTrailers());
        dontShowSignLanguage.set(filterConfig.isDontShowSignLanguage());
        dontShowAudioVersions.set(filterConfig.isDontShowAudioVersions());
        dontShowDuplicates.set(filterConfig.isDontShowDuplicates());
        viewSettingsPane.themaComboBox.setValue(filterConfig.getThema());

        restoreFilmLengthSlider();

        restoreSenderList();*/
        //TODO restore Zeitraum?
    }

    private void setupResetCurrentFilterButton() {
        btnResetCurrentFilter.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/recycle.svg"));
        btnResetCurrentFilter.addActionListener(e -> {
            //TODO clear sender check list?
            filterConfig.clearCurrentFilter();
            restoreConfigSettings();
        });
    }

    private void setupAddNewFilterButton() {
        btnAddNewFilter.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/plus.svg"));
        btnAddNewFilter.addActionListener(e -> {
            FilterDTO newFilter = new FilterDTO(UUID.randomUUID(), String.format("Filter %d", filterConfig.getAvailableFilters().size() + 1));
            filterConfig.addNewFilter(newFilter);
            checkDeleteCurrentFilterButtonState();
            filterSelectionComboBoxModel.setSelectedItem(newFilter);
        });
    }

    private void setupDeleteCurrentFilterButton() {
        checkDeleteCurrentFilterButtonState();

        btnDeleteCurrentFilter.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/trash-can.svg"));
        btnDeleteCurrentFilter.addActionListener(e -> {
            FilterDTO filterToDelete = filterConfig.getCurrentFilter();
            filterConfig.deleteFilter(filterToDelete);

            checkDeleteCurrentFilterButtonState();
        });
    }

    private void setupRenameFilterButton() {
        //FIXME check if filter already exists -> do nothing then!!
        btnRenameFilter.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/pen-to-square.svg"));
        btnRenameFilter.addActionListener(l -> {
            final var fltName = filterConfig.getCurrentFilter().name();
            String thema;
            String s = (String) JOptionPane.showInputDialog(MediathekGui.ui(), "Neuer Name des Filters:", "Filter umbenennen", JOptionPane.PLAIN_MESSAGE, null, null, fltName);
            if (s != null) {
                if (!s.isEmpty()) {
                    final var fName = s.trim();
                    if (!fName.equals(fltName)) {
                        Configuration config = ApplicationConfiguration.getConfiguration();
                        config.lock(LockMode.WRITE);
                        thema = filterConfig.getThema();
                        filterConfig.setThema("");
                        filterConfig.renameCurrentFilter(fName);
                        filterConfig.setThema(thema);
                        config.unlock(LockMode.WRITE);
                        logger.trace("Renamed filter \"{}\" to \"{}\"", fltName, fName);
                    } else
                        logger.warn("New and old filter name are identical...doing nothing");
                } else
                    logger.warn("Rename filter text was empty...doing nothing");
            } else
                logger.trace("User cancelled rename");
        });
    }

    @Handler
    private void handleTableModelChangeEvent(TableModelChangeEvent e) {
        SwingUtilities.invokeLater(() -> {
            var enable = !e.active;
            setEnabled(enable);
            //FIXME disable all items in dialog
            btnRenameFilter.setEnabled(enable);

            //This looks strange but works...check later
            if (e.active) {
                btnDeleteCurrentFilter.setEnabled(false);
            } else {
                btnDeleteCurrentFilter.setEnabled(filterConfig.getAvailableFilterCount() > 1);
            }

            cbShowNewOnly.setEnabled(enable);
            cbShowBookMarkedOnly.setEnabled(enable);
            cbShowOnlyHq.setEnabled(enable);
            cbShowSubtitlesOnly.setEnabled(enable);
            cbShowOnlyLivestreams.setEnabled(enable);

            cboxFilterSelection.setEnabled(enable);
            spZeitraum.setEnabled(enable);
            label1.setEnabled(enable);
            label2.setEnabled(enable);
        });
    }

    private void restoreDialogVisibility() {
        final boolean visible = config.getBoolean(ApplicationConfiguration.FilterDialog.VISIBLE, false);
        setVisible(visible);
    }

    private void storeDialogVisibility() {
        var config = ApplicationConfiguration.getConfiguration();
        config.setProperty(ApplicationConfiguration.FilterDialog.VISIBLE, isVisible());
    }

    private void restoreWindowSizeFromConfig() {
        try {
            config.lock(LockMode.READ);
            final int width = config.getInt(ApplicationConfiguration.FilterDialog.WIDTH);
            final int height = config.getInt(ApplicationConfiguration.FilterDialog.HEIGHT);
            final int x = config.getInt(ApplicationConfiguration.FilterDialog.X);
            final int y = config.getInt(ApplicationConfiguration.FilterDialog.Y);

            setBounds(x, y, width, height);
        } catch (NoSuchElementException ignored) {
            //do not restore anything
        } finally {
            config.unlock(LockMode.READ);
        }

    }

    public class FilterDialogComponentListener extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            storeWindowPosition(e);
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            storeWindowPosition(e);
        }

        @Override
        public void componentShown(ComponentEvent e) {
            storeDialogVisibility();
            filterToggleButton.setSelected(true);
        }

        @Override
        public void componentHidden(ComponentEvent e) {
            storeWindowPosition(e);
            storeDialogVisibility();

            filterToggleButton.setSelected(false);
        }

        private void storeWindowPosition(ComponentEvent e) {
            var component = e.getComponent();

            var dims = component.getSize();
            var loc = component.getLocation();
            try {
                config.lock(LockMode.WRITE);
                config.setProperty(ApplicationConfiguration.FilterDialog.WIDTH, dims.width);
                config.setProperty(ApplicationConfiguration.FilterDialog.HEIGHT, dims.height);
                config.setProperty(ApplicationConfiguration.FilterDialog.X, loc.x);
                config.setProperty(ApplicationConfiguration.FilterDialog.Y, loc.y);
            } finally {
                config.unlock(LockMode.WRITE);
            }
        }
    }

    private void createUIComponents() {
        // TODO: add custom component creation code here
        cboxFilterSelection = new FilterSelectionComboBox(filterSelectionComboBoxModel);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        createUIComponents();

        panel1 = new JPanel();
        btnRenameFilter = new JButton();
        btnAddNewFilter = new JButton();
        btnDeleteCurrentFilter = new JButton();
        separator1 = new JSeparator();
        btnResetCurrentFilter = new JButton();
        separator2 = new JSeparator();
        cbShowNewOnly = new JCheckBox();
        cbShowBookMarkedOnly = new JCheckBox();
        cbShowOnlyHq = new JCheckBox();
        cbShowSubtitlesOnly = new JCheckBox();
        cbShowOnlyLivestreams = new JCheckBox();
        separator3 = new JSeparator();
        checkBox6 = new JCheckBox();
        checkBox7 = new JCheckBox();
        checkBox8 = new JCheckBox();
        checkBox9 = new JCheckBox();
        checkBox10 = new JCheckBox();
        checkBox11 = new JCheckBox();
        separator4 = new JSeparator();
        label3 = new JLabel();
        scrollPane1 = new JScrollPane();
        list1 = new CheckBoxList();
        separator5 = new JSeparator();
        label4 = new JLabel();
        jcbThema = new JComboBox<>();
        separator6 = new JSeparator();
        panel2 = new JPanel();
        label5 = new JLabel();
        label6 = new JLabel();
        hSpacer1 = new JPanel(null);
        label7 = new JLabel();
        label8 = new JLabel();
        slider1 = new JSlider();
        separator7 = new JSeparator();
        label1 = new JLabel();
        spZeitraum = new SwingZeitraumSpinner();
        label2 = new JLabel();

        //======== this ========
        setType(Window.Type.UTILITY);
        setTitle("Swing Filter"); //NON-NLS
        var contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            new LC().fillX().insets("5").hideMode(3), //NON-NLS
            // columns
            new AC()
                .align("left").gap() //NON-NLS
                .grow().fill().gap()
                .fill(),
            // rows
            new AC()
                .gap()
                .shrink(0).align("top").gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .shrink(0).gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .shrink(0).gap()
                .gap()
                .grow().gap()
                .shrink(0).gap()
                .gap()
                .shrink(0).gap()
                .gap()
                .shrink(0).gap()
                ));

        //======== panel1 ========
        {
            panel1.setLayout(new MigLayout(
                new LC().fillX().insets("0").hideMode(3), //NON-NLS
                // columns
                new AC()
                    .grow().fill().gap()
                    .fill().gap()
                    .fill().gap()
                    .align("left").gap() //NON-NLS
                    .fill().gap()
                    .fill(),
                // rows
                new AC()
                    .grow().fill()));
            panel1.add(cboxFilterSelection, new CC().cell(0, 0));

            //---- btnRenameFilter ----
            btnRenameFilter.setToolTipText("Filter umbenennen"); //NON-NLS
            panel1.add(btnRenameFilter, new CC().cell(1, 0).alignX("center").growX(0)); //NON-NLS

            //---- btnAddNewFilter ----
            btnAddNewFilter.setToolTipText("Neuen Filter anlegen"); //NON-NLS
            panel1.add(btnAddNewFilter, new CC().cell(2, 0).alignX("center").growX(0)); //NON-NLS

            //---- btnDeleteCurrentFilter ----
            btnDeleteCurrentFilter.setToolTipText("Aktuellen Filter l\u00f6schen"); //NON-NLS
            panel1.add(btnDeleteCurrentFilter, new CC().cell(3, 0).alignX("center").growX(0)); //NON-NLS

            //---- separator1 ----
            separator1.setOrientation(SwingConstants.VERTICAL);
            panel1.add(separator1, new CC().cell(4, 0));

            //---- btnResetCurrentFilter ----
            btnResetCurrentFilter.setToolTipText("Aktuellen Filter zur\u00fccksetzen"); //NON-NLS
            panel1.add(btnResetCurrentFilter, new CC().cell(5, 0).alignX("center").growX(0)); //NON-NLS
        }
        contentPane.add(panel1, new CC().cell(0, 0, 3, 1).growX());
        contentPane.add(separator2, new CC().cell(0, 1, 3, 1).growX());

        //---- cbShowNewOnly ----
        cbShowNewOnly.setText("Nur neue Filme anzeigen"); //NON-NLS
        contentPane.add(cbShowNewOnly, new CC().cell(0, 2, 3, 1));

        //---- cbShowBookMarkedOnly ----
        cbShowBookMarkedOnly.setText("Nur gemerkte Filme anzeigen"); //NON-NLS
        contentPane.add(cbShowBookMarkedOnly, new CC().cell(0, 3, 3, 1));

        //---- cbShowOnlyHq ----
        cbShowOnlyHq.setText("Nur High Quality(HQ) Filme anzeigen"); //NON-NLS
        contentPane.add(cbShowOnlyHq, new CC().cell(0, 4, 3, 1));

        //---- cbShowSubtitlesOnly ----
        cbShowSubtitlesOnly.setText("Nur Filme mit Untertitel anzeigen"); //NON-NLS
        contentPane.add(cbShowSubtitlesOnly, new CC().cell(0, 5, 3, 1));

        //---- cbShowOnlyLivestreams ----
        cbShowOnlyLivestreams.setText("Nur Livestreams anzeigen"); //NON-NLS
        contentPane.add(cbShowOnlyLivestreams, new CC().cell(0, 6, 3, 1));
        contentPane.add(separator3, new CC().cell(0, 7, 3, 1).growX());

        //---- checkBox6 ----
        checkBox6.setText("Gesehene Filme nicht anzeigen"); //NON-NLS
        contentPane.add(checkBox6, new CC().cell(0, 8, 3, 1));

        //---- checkBox7 ----
        checkBox7.setText("Abos nicht anzeigen"); //NON-NLS
        contentPane.add(checkBox7, new CC().cell(0, 9, 3, 1));

        //---- checkBox8 ----
        checkBox8.setText("Geb\u00e4rdensprache nicht anzeigen"); //NON-NLS
        contentPane.add(checkBox8, new CC().cell(0, 10, 3, 1));

        //---- checkBox9 ----
        checkBox9.setText("Trailer/Teaser/Vorschau nicht anzeigen"); //NON-NLS
        contentPane.add(checkBox9, new CC().cell(0, 11, 3, 1));

        //---- checkBox10 ----
        checkBox10.setText("H\u00f6rfassungen ausblenden"); //NON-NLS
        contentPane.add(checkBox10, new CC().cell(0, 12, 3, 1));

        //---- checkBox11 ----
        checkBox11.setText("Duplikate nicht anzeigen"); //NON-NLS
        contentPane.add(checkBox11, new CC().cell(0, 13, 3, 1));
        contentPane.add(separator4, new CC().cell(0, 14, 3, 1).growX());

        //---- label3 ----
        label3.setText("Sender:"); //NON-NLS
        contentPane.add(label3, new CC().cell(0, 15, 3, 1));

        //======== scrollPane1 ========
        {

            //---- list1 ----
            list1.setModel(new AbstractListModel<String>() {
                String[] values = {
                    "3Sat", //NON-NLS
                    "ARD", //NON-NLS
                    "ARD-alpha", //NON-NLS
                    "ARTE.DE", //NON-NLS
                    "ARTE.EN", //NON-NLS
                    "ARTE.ES", //NON-NLS
                    "ARTE.FR", //NON-NLS
                    "ARTE.IT", //NON-NLS
                    "ARTE.PL", //NON-NLS
                    "BR", //NON-NLS
                    "DW", //NON-NLS
                    "Funk.net", //NON-NLS
                    "HR", //NON-NLS
                    "KiKA", //NON-NLS
                    "MDR", //NON-NLS
                    "NDR", //NON-NLS
                    "ONE", //NON-NLS
                    "ORF", //NON-NLS
                    "PHOENIX", //NON-NLS
                    "Radio Bremen TV", //NON-NLS
                    "RBB", //NON-NLS
                    "SR", //NON-NLS
                    "SRF", //NON-NLS
                    "SRF.Podcast", //NON-NLS
                    "SWR", //NON-NLS
                    "tagesschau24", //NON-NLS
                    "WDR", //NON-NLS
                    "ZDF", //NON-NLS
                    "ZDF-tivi" //NON-NLS
                };
                @Override
                public int getSize() { return values.length; }
                @Override
                public String getElementAt(int i) { return values[i]; }
            });
            scrollPane1.setViewportView(list1);
        }
        contentPane.add(scrollPane1, new CC().cell(0, 16, 3, 1).grow().minHeight("50")); //NON-NLS
        contentPane.add(separator5, new CC().cell(0, 17, 3, 1).growX());

        //---- label4 ----
        label4.setText("Thema:"); //NON-NLS
        contentPane.add(label4, new CC().cell(0, 18));

        //---- jcbThema ----
        jcbThema.setModel(new DefaultComboBoxModel<>(new String[] {
            "Mein Titel", //NON-NLS
            "Mein Test", //NON-NLS
            "Sendung mit der Maus" //NON-NLS
        }));
        contentPane.add(jcbThema, new CC().cell(1, 18, 2, 1).growX());
        contentPane.add(separator6, new CC().cell(0, 19, 3, 1).growX());

        //======== panel2 ========
        {
            panel2.setLayout(new MigLayout(
                new LC().fill().insets("0").hideMode(3), //NON-NLS
                // columns
                new AC()
                    .fill().gap("0") //NON-NLS
                    .fill().gap()
                    .grow().fill().gap()
                    .fill().gap("0") //NON-NLS
                    .fill(),
                // rows
                new AC()
                    .gap("0") //NON-NLS
                    ));

            //---- label5 ----
            label5.setText("Mindestl\u00e4nge:"); //NON-NLS
            panel2.add(label5, new CC().cell(0, 0));

            //---- label6 ----
            label6.setText("0"); //NON-NLS
            panel2.add(label6, new CC().cell(1, 0));
            panel2.add(hSpacer1, new CC().cell(2, 0).growX());

            //---- label7 ----
            label7.setText("Maximall\u00e4nge:"); //NON-NLS
            panel2.add(label7, new CC().cell(3, 0));

            //---- label8 ----
            label8.setText("100"); //NON-NLS
            panel2.add(label8, new CC().cell(4, 0));
            panel2.add(slider1, new CC().cell(0, 1, 5, 1).growX());
        }
        contentPane.add(panel2, new CC().cell(0, 20, 3, 1).growX());
        contentPane.add(separator7, new CC().cell(0, 21, 3, 1).growX());

        //---- label1 ----
        label1.setText("Zeitraum:"); //NON-NLS
        contentPane.add(label1, new CC().cell(0, 22));
        contentPane.add(spZeitraum, new CC().cell(1, 22));

        //---- label2 ----
        label2.setText("Tage"); //NON-NLS
        contentPane.add(label2, new CC().cell(2, 22));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JPanel panel1;
    private FilterSelectionComboBox cboxFilterSelection;
    private JButton btnRenameFilter;
    private JButton btnAddNewFilter;
    private JButton btnDeleteCurrentFilter;
    private JSeparator separator1;
    private JButton btnResetCurrentFilter;
    private JSeparator separator2;
    private JCheckBox cbShowNewOnly;
    private JCheckBox cbShowBookMarkedOnly;
    private JCheckBox cbShowOnlyHq;
    private JCheckBox cbShowSubtitlesOnly;
    private JCheckBox cbShowOnlyLivestreams;
    private JSeparator separator3;
    private JCheckBox checkBox6;
    private JCheckBox checkBox7;
    private JCheckBox checkBox8;
    private JCheckBox checkBox9;
    private JCheckBox checkBox10;
    private JCheckBox checkBox11;
    private JSeparator separator4;
    private JLabel label3;
    private JScrollPane scrollPane1;
    private CheckBoxList list1;
    private JSeparator separator5;
    private JLabel label4;
    private JComboBox<String> jcbThema;
    private JSeparator separator6;
    private JPanel panel2;
    private JLabel label5;
    private JLabel label6;
    private JPanel hSpacer1;
    private JLabel label7;
    private JLabel label8;
    private JSlider slider1;
    private JSeparator separator7;
    private JLabel label1;
    public SwingZeitraumSpinner spZeitraum;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
