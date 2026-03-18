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

package mediathek.gui.dialog.add_download;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.jdesktop.swingx.JXBusyLabel;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;

/**
 * Base class for UI Designer.
 * Subclass uses kotlin coroutines for concurrent work.
 */
public class DialogAddDownload extends JDialog {
    public DialogAddDownload(Frame parent) {
        super(parent, true);
        initComponents();
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    private void initComponents() {
        var panel2 = new JPanel();
        var panel1 = new JPanel();
        btnQueueDownload = new JButton();
        btnDownloadImmediately = new JButton();
        jButtonAbbrechen = new JButton();
        var jPanel1 = new JPanel();
        var jPanel2 = new JPanel();
        jCheckBoxInfodatei = new JCheckBox();
        jCheckBoxPfadSpeichern = new JCheckBox();
        jCheckBoxSubtitle = new DownloadSubtitleCheckBox();
        var jPanel7 = new JPanel();
        var jLabelSet = new JLabel();
        jComboBoxPset = new javax.swing.JComboBox<>();
        var jLabel1 = new JLabel();
        var jPanel4 = new JPanel();
        jComboBoxPfad = new javax.swing.JComboBox<>();
        jButtonZiel = new JButton();
        jButtonDelHistory = new JButton();
        var jLabel4 = new JLabel();
        jTextFieldName = new JTextField();
        jPanelSize = new JPanel();
        var jPanel6 = new JPanel();
        jRadioButtonAufloesungHd = new JRadioButton();
        jRadioButtonAufloesungHoch = new JRadioButton();
        jRadioButtonAufloesungKlein = new JRadioButton();
        var jPanel3 = new JPanel();
        btnRequestLiveInfo = new JButton();
        lblBusyIndicator = new JXBusyLabel();
        var jPanel5 = new JPanel();
        lblStatus = new JLabel();
        lblAudioInfo = new JLabel();
        jTextFieldSender = new JTextField();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Film speichern");
        setMinimumSize(new Dimension(660, 420));
        setPreferredSize(new Dimension(660, 420));
        var contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            new LC().fillX().insets("dialog").hideMode(3),
            // columns
            new AC()
                .grow().fill(),
            // rows
            new AC()
                .gap()
                .gap()
                .grow().fill().gap()
                ));

        //======== panel2 ========
        {
            panel2.setLayout(new MigLayout(
                new LC().fillX().insets("0").hideMode(3),
                // columns
                new AC()
                    .grow().fill(),
                // rows
                new AC()
                    ));

            //======== panel1 ========
            {
                panel1.setLayout(new MigLayout(
                    new LC().insets("0").hideMode(3).alignX("right").gridGap("5", "5"),
                    // columns
                    new AC()
                        .fill().gap()
                        .fill().gap()
                        .fill(),
                    // rows
                    new AC()
                        .fill()));

                //---- btnQueueDownload ----
                btnQueueDownload.setText("In die Warteschlange");
                panel1.add(btnQueueDownload, new CC().cell(0, 0));

                //---- btnDownloadImmediately ----
                btnDownloadImmediately.setText("Sofort laden");
                panel1.add(btnDownloadImmediately, new CC().cell(1, 0));

                //---- jButtonAbbrechen ----
                jButtonAbbrechen.setText("Abbrechen");
                panel1.add(jButtonAbbrechen, new CC().cell(2, 0));
            }
            panel2.add(panel1, new CC().cell(0, 0).alignX("right").growX());
        }
        contentPane.add(panel2, new CC().cell(0, 3).growX());

        //======== jPanel1 ========
        {
            jPanel1.setLayout(new MigLayout(
                new LC().fillX().insets("0").hideMode(3),
                // columns
                new AC()
                    .grow().fill(),
                // rows
                new AC()
                    .gap()
                    .size("12").gap()
                    ));

            //======== jPanel2 ========
            {
                jPanel2.setLayout(new GridLayout(2, 2));

                //---- jCheckBoxInfodatei ----
                jCheckBoxInfodatei.setText("Lege Infodatei an");
                jCheckBoxInfodatei.setToolTipText("Erzeugt eine Infodatei im Format \"Infodatei.txt\"");
                jPanel2.add(jCheckBoxInfodatei);

                //---- jCheckBoxPfadSpeichern ----
                jCheckBoxPfadSpeichern.setText("Zielpfad speichern");
                jPanel2.add(jCheckBoxPfadSpeichern);
                jPanel2.add(jCheckBoxSubtitle);
            }
            jPanel1.add(jPanel2, new CC().cell(0, 2).growX());

            //======== jPanel7 ========
            {
                jPanel7.setLayout(new MigLayout(
                    new LC().fillX().insets("0").hideMode(3).gridGap("5", "8"),
                    // columns
                    new AC()
                        .align("right").gap()
                        .grow().fill(),
                    // rows
                    new AC()
                        .gap()
                        .gap()
                        ));

                //---- jLabelSet ----
                jLabelSet.setText("Set:");
                jPanel7.add(jLabelSet, new CC().cell(0, 0));
                jPanel7.add(jComboBoxPset, new CC().cell(1, 0).growX());

                //---- jLabel1 ----
                jLabel1.setText("Zielpfad:");
                jPanel7.add(jLabel1, new CC().cell(0, 1));

                //======== jPanel4 ========
                {
                    jPanel4.setLayout(new MigLayout(
                        new LC().fillX().insets("0").hideMode(3).gridGap("5", "0"),
                        // columns
                        new AC()
                            .grow().fill().gap()
                            .size("pref!").gap()
                            .size("pref!"),
                        // rows
                        new AC()
                            ));

                    //---- jComboBoxPfad ----
                    jComboBoxPfad.setEditable(true);
                    jPanel4.add(jComboBoxPfad, new CC().cell(0, 0).pushX().growX());

                    //---- jButtonZiel ----
                    jButtonZiel.setText("F");
                    jButtonZiel.setToolTipText("Zielpfad ausw\u00e4hlen");
                    jPanel4.add(jButtonZiel, new CC().cell(1, 0));

                    //---- jButtonDelHistory ----
                    jButtonDelHistory.setText("H");
                    jButtonDelHistory.setToolTipText("History l\u00f6schen");
                    jPanel4.add(jButtonDelHistory, new CC().cell(2, 0));
                }
                jPanel7.add(jPanel4, new CC().cell(1, 1).growX());

                //---- jLabel4 ----
                jLabel4.setText("Dateiname:");
                jPanel7.add(jLabel4, new CC().cell(0, 2));

                //---- jTextFieldName ----
                jTextFieldName.setColumns(30);
                jTextFieldName.setMinimumSize(new Dimension(0, 26));
                jPanel7.add(jTextFieldName, new CC().cell(1, 2).pushX().growX());
            }
            jPanel1.add(jPanel7, new CC().cell(0, 0).growX());
        }
        contentPane.add(jPanel1, new CC().cell(0, 1).growX());

        //======== jPanelSize ========
        {
            jPanelSize.setBorder(new TitledBorder("Download-Qualit\u00e4t"));
            jPanelSize.setLayout(new MigLayout(
                new LC().fillX().insets("0").hideMode(3),
                // columns
                new AC()
                    .grow().fill(),
                // rows
                new AC()
                    .gap()
                    .grow().fill()));

            //======== jPanel6 ========
            {
                jPanel6.setLayout(new FlowLayout());

                //---- jRadioButtonAufloesungHd ----
                jRadioButtonAufloesungHd.setText("H\u00f6chste/Hoch");
                jPanel6.add(jRadioButtonAufloesungHd);

                //---- jRadioButtonAufloesungHoch ----
                jRadioButtonAufloesungHoch.setText("Mittel");
                jPanel6.add(jRadioButtonAufloesungHoch);

                //---- jRadioButtonAufloesungKlein ----
                jRadioButtonAufloesungKlein.setText("Niedrig");
                jPanel6.add(jRadioButtonAufloesungKlein);
            }
            jPanelSize.add(jPanel6, new CC().cell(0, 0).alignX("left"));

            //======== jPanel3 ========
            {
                jPanel3.setLayout(new FlowLayout(FlowLayout.LEFT));

                //---- btnRequestLiveInfo ----
                btnRequestLiveInfo.setText("Codec-Details abrufen...");
                jPanel3.add(btnRequestLiveInfo);
                jPanel3.add(lblBusyIndicator);

                //======== jPanel5 ========
                {
                    jPanel5.setLayout(new GridLayout(2, 1));

                    //---- lblStatus ----
                    lblStatus.setText("status");
                    jPanel5.add(lblStatus);

                    //---- lblAudioInfo ----
                    lblAudioInfo.setText("audio");
                    jPanel5.add(lblAudioInfo);
                }
                jPanel3.add(jPanel5);
            }
            jPanelSize.add(jPanel3, new CC().cell(0, 1).pushY().growX());
        }
        contentPane.add(jPanelSize, new CC().cell(0, 2).push().grow());

        //---- jTextFieldSender ----
        jTextFieldSender.setEditable(false);
        jTextFieldSender.setColumns(30);
        jTextFieldSender.setMinimumSize(new Dimension(0, 26));
        jTextFieldSender.setFont(jTextFieldSender.getFont().deriveFont(jTextFieldSender.getFont().getStyle() | Font.BOLD));
        jTextFieldSender.setText(" ARD: Tatort, ...");
        jTextFieldSender.setBorder(new TitledBorder("Film"));
        contentPane.add(jTextFieldSender, new CC().cell(0, 0).growX());
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(jRadioButtonAufloesungHd);
        buttonGroup1.add(jRadioButtonAufloesungHoch);
        buttonGroup1.add(jRadioButtonAufloesungKlein);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    protected JButton btnQueueDownload;
    protected JButton btnDownloadImmediately;
    protected JButton jButtonAbbrechen;
    protected JCheckBox jCheckBoxInfodatei;
    protected JCheckBox jCheckBoxPfadSpeichern;
    protected DownloadSubtitleCheckBox jCheckBoxSubtitle;
    protected JComboBox<String> jComboBoxPset;
    protected JComboBox<String> jComboBoxPfad;
    protected JButton jButtonZiel;
    protected JButton jButtonDelHistory;
    protected JTextField jTextFieldName;
    protected JPanel jPanelSize;
    protected JRadioButton jRadioButtonAufloesungHd;
    protected JRadioButton jRadioButtonAufloesungHoch;
    protected JRadioButton jRadioButtonAufloesungKlein;
    protected JButton btnRequestLiveInfo;
    protected JXBusyLabel lblBusyIndicator;
    protected JLabel lblStatus;
    protected JLabel lblAudioInfo;
    protected JTextField jTextFieldSender;
    // End of variables declaration//GEN-END:variables

    protected void constrainPackedSizeToScreen() {
        var graphicsConfiguration = getGraphicsConfiguration();
        Rectangle usableBounds = graphicsConfiguration != null
                ? graphicsConfiguration.getBounds().intersection(GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds())
                : GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();

        int boundedWidth = (int) Math.min(getWidth(), usableBounds.getWidth());
        int boundedHeight = (int) Math.min(getHeight(), usableBounds.getHeight());
        if (boundedWidth != getWidth() || boundedHeight != getHeight()) {
            setSize(boundedWidth, boundedHeight);
        }
    }
}
