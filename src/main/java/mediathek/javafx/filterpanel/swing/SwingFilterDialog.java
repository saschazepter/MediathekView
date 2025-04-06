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

import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBox;
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel;
import mediathek.javafx.filterpanel.swing.zeitraum.SwingZeitraumSpinner;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.awt.*;

/**
 * @author christianfranzke
 */
public class SwingFilterDialog extends JDialog {
    private final FilterSelectionComboBoxModel filterSelectionComboBoxModel;

    public SwingFilterDialog(Window owner, @NotNull FilterSelectionComboBoxModel model) {
        super(owner);
        this.filterSelectionComboBoxModel = model;

        initComponents();
        comboBox1.setMaximumSize(new Dimension(500, 100));
    }

    private void createUIComponents() {
        // TODO: add custom component creation code here
        comboBox1 = new FilterSelectionComboBox(filterSelectionComboBoxModel);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        createUIComponents();

        panel1 = new JPanel();
        button1 = new JButton();
        button2 = new JButton();
        button3 = new JButton();
        separator1 = new JSeparator();
        button4 = new JButton();
        separator2 = new JSeparator();
        checkBox2 = new JCheckBox();
        checkBox1 = new JCheckBox();
        checkBox3 = new JCheckBox();
        checkBox4 = new JCheckBox();
        checkBox5 = new JCheckBox();
        separator3 = new JSeparator();
        checkBox6 = new JCheckBox();
        checkBox7 = new JCheckBox();
        checkBox8 = new JCheckBox();
        checkBox9 = new JCheckBox();
        checkBox10 = new JCheckBox();
        checkBox11 = new JCheckBox();
        separator4 = new JSeparator();
        label1 = new JLabel();
        spinner1 = new SwingZeitraumSpinner();
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
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap()
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
                .gap("0") //NON-NLS
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
            panel1.add(comboBox1, new CC().cell(0, 0));

            //---- button1 ----
            button1.setText("1"); //NON-NLS
            button1.setToolTipText("Filter umbenennen"); //NON-NLS
            panel1.add(button1, new CC().cell(1, 0).alignX("center").growX(0)); //NON-NLS

            //---- button2 ----
            button2.setText("2"); //NON-NLS
            button2.setToolTipText("Neuen Filter anlegen"); //NON-NLS
            panel1.add(button2, new CC().cell(2, 0).alignX("center").growX(0)); //NON-NLS

            //---- button3 ----
            button3.setText("3"); //NON-NLS
            button3.setToolTipText("Aktuellen Filter l\u00f6schen"); //NON-NLS
            panel1.add(button3, new CC().cell(3, 0).alignX("center").growX(0)); //NON-NLS

            //---- separator1 ----
            separator1.setOrientation(SwingConstants.VERTICAL);
            panel1.add(separator1, new CC().cell(4, 0));

            //---- button4 ----
            button4.setText("4"); //NON-NLS
            button4.setToolTipText("Aktuellen Filter zur\u00fccksetzen"); //NON-NLS
            panel1.add(button4, new CC().cell(5, 0).alignX("center").growX(0)); //NON-NLS
        }
        contentPane.add(panel1, new CC().cell(0, 0, 3, 1).growX());
        contentPane.add(separator2, new CC().cell(0, 1, 3, 1));

        //---- checkBox2 ----
        checkBox2.setText("Nur neue Filme anzeigen"); //NON-NLS
        contentPane.add(checkBox2, new CC().cell(0, 2, 3, 1));

        //---- checkBox1 ----
        checkBox1.setText("Nur gemerkte Filme anzeigen"); //NON-NLS
        contentPane.add(checkBox1, new CC().cell(0, 3, 3, 1));

        //---- checkBox3 ----
        checkBox3.setText("Nur High Quality(HQ) Filme anzeigen"); //NON-NLS
        contentPane.add(checkBox3, new CC().cell(0, 4, 3, 1));

        //---- checkBox4 ----
        checkBox4.setText("Nur Filme mit Untertitel anzeigen"); //NON-NLS
        contentPane.add(checkBox4, new CC().cell(0, 5, 3, 1));

        //---- checkBox5 ----
        checkBox5.setText("Nur Livestreams anzeigen"); //NON-NLS
        contentPane.add(checkBox5, new CC().cell(0, 6, 3, 1));
        contentPane.add(separator3, new CC().cell(0, 7, 3, 1));

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
        contentPane.add(separator4, new CC().cell(0, 14, 3, 1));

        //---- label1 ----
        label1.setText("Zeitraum:"); //NON-NLS
        contentPane.add(label1, new CC().cell(0, 15));
        contentPane.add(spinner1, new CC().cell(1, 15));

        //---- label2 ----
        label2.setText("Tage"); //NON-NLS
        contentPane.add(label2, new CC().cell(2, 15));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JPanel panel1;
    private FilterSelectionComboBox comboBox1;
    private JButton button1;
    private JButton button2;
    private JButton button3;
    private JSeparator separator1;
    private JButton button4;
    private JSeparator separator2;
    private JCheckBox checkBox2;
    private JCheckBox checkBox1;
    private JCheckBox checkBox3;
    private JCheckBox checkBox4;
    private JCheckBox checkBox5;
    private JSeparator separator3;
    private JCheckBox checkBox6;
    private JCheckBox checkBox7;
    private JCheckBox checkBox8;
    private JCheckBox checkBox9;
    private JCheckBox checkBox10;
    private JCheckBox checkBox11;
    private JSeparator separator4;
    private JLabel label1;
    private SwingZeitraumSpinner spinner1;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
