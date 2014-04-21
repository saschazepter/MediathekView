/*    
 *    MediathekView
 *    Copyright (C) 2012   W. Xaver
 *    W.Xaver[at]googlemail.com
 *    http://zdfmediathk.sourceforge.net/
 *    
 *    This program is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    any later version.
 *
 *    This program is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.gui.dialogEinstellungen;

import com.jidesoft.utils.SystemInfo;
import java.awt.Cursor;
import java.awt.FileDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.nio.file.Path;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mediathek.controller.IoXmlLesen;
import mediathek.controller.Log;
import mediathek.daten.Daten;
import mediathek.daten.ListePsetVorlagen;
import mediathek.gui.PanelVorlage;
import mediathek.res.GetIcon;
import mediathek.tool.MVMessageDialog;

public class PanelImport extends PanelVorlage {

    ListePsetVorlagen listeVorlagen = new ListePsetVorlagen();

    public PanelImport(Daten d, JFrame parentComponent) {
        super(d, parentComponent);
        initComponents();
        init();
    }

    private void init() {
        jButtonPfad.setIcon(GetIcon.getIcon("fileopen_16.png"));
        jButtonImportDatei.setEnabled(false);
        jButtonPfad.addActionListener(new BeobPfad());
        jTextFieldDatei.getDocument().addDocumentListener(new BeobPfadDoc());
        jButtonImportDatei.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                importDatei(jTextFieldDatei.getText());
            }
        });
        jCheckBoxAbo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setButtonImport();
            }
        });
        jCheckBoxBlack.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                setButtonImport();
            }
        });
        final Path xmlFilePath = Daten.getMediathekXmlFilePath();
        jTextFieldPfadKonfig.setText(xmlFilePath.toAbsolutePath().toString());
    }

    private void importDatei(String datei) {
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        int[] found = IoXmlLesen.importAboBlacklist(parentComponent, datei, jCheckBoxAbo.isSelected(), jCheckBoxBlack.isSelected());
        String text = "Es wurden\n"
                + found[0] + " Abos und\n"
                + found[1] + " Blacklisteinträge\n"
                + "hinzugefügt";
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        MVMessageDialog.showMessageDialog(parentComponent, text, "Import", JOptionPane.INFORMATION_MESSAGE);
    }

    private void setButtonImport() {
        jButtonImportDatei.setEnabled(!jTextFieldDatei.getText().isEmpty() && (jCheckBoxAbo.isSelected() || jCheckBoxBlack.isSelected()));
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        javax.swing.JPanel jPanel6 = new javax.swing.JPanel();
        jTextFieldDatei = new javax.swing.JTextField();
        jButtonPfad = new javax.swing.JButton();
        jButtonImportDatei = new javax.swing.JButton();
        javax.swing.JLabel jLabel7 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        jTextArea1 = new javax.swing.JTextArea();
        jCheckBoxAbo = new javax.swing.JCheckBox();
        jCheckBoxBlack = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        jTextFieldPfadKonfig = new javax.swing.JTextField();

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Abos und Blacklist aus Datei importieren"));

        jButtonPfad.setIcon(new javax.swing.ImageIcon(getClass().getResource("/mediathek/res/fileopen_16.png"))); // NOI18N

        jButtonImportDatei.setText("Import");

        jLabel7.setText("Datei:");

        jTextArea1.setEditable(false);
        jTextArea1.setColumns(20);
        jTextArea1.setLineWrap(true);
        jTextArea1.setRows(5);
        jTextArea1.setText("Damit können Abos/Blacklist aus einer alten gesicherten Konfigurationsdatei\nimportiert werden.\n\nSollen die aktuellen Abos/Blacklist durch die importierten ersetzt werden,\nsollten die aktuellen zuerst gelöscht werden.\n\nDie importierten Abos/Blacklist werden an die vorhandenen angehängt.\n");
        jScrollPane1.setViewportView(jTextArea1);

        jCheckBoxAbo.setText("Abos importieren");

        jCheckBoxBlack.setText("Blacklist importieren");

        jLabel1.setText("aktuelle Konfigurationsdatei:");

        jTextFieldPfadKonfig.setEditable(false);
        jTextFieldPfadKonfig.setText("jTextField1");

        javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel6Layout.createSequentialGroup()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jTextFieldPfadKonfig))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane1))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                        .addContainerGap(12, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jTextFieldDatei)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonPfad))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jCheckBoxBlack)
                                .addGap(0, 0, Short.MAX_VALUE))
                            .addGroup(jPanel6Layout.createSequentialGroup()
                                .addComponent(jCheckBoxAbo)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 379, Short.MAX_VALUE)
                                .addComponent(jButtonImportDatei))))
                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, jPanel6Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jLabel1)
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonPfad)
                    .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jTextFieldDatei, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel7)))
                .addGap(18, 18, 18)
                .addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jButtonImportDatei)
                    .addComponent(jCheckBoxAbo))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jCheckBoxBlack)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 128, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 173, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jTextFieldPfadKonfig, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jPanel6Layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {jButtonPfad, jTextFieldDatei, jTextFieldPfadKonfig});

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel6, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonImportDatei;
    private javax.swing.JButton jButtonPfad;
    private javax.swing.JCheckBox jCheckBoxAbo;
    private javax.swing.JCheckBox jCheckBoxBlack;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextArea jTextArea1;
    private javax.swing.JTextField jTextFieldDatei;
    private javax.swing.JTextField jTextFieldPfadKonfig;
    // End of variables declaration//GEN-END:variables

    private class BeobPfadDoc implements DocumentListener {

        @Override
        public void insertUpdate(DocumentEvent arg0) {
            eingabe();
        }

        @Override
        public void removeUpdate(DocumentEvent arg0) {
            eingabe();
        }

        @Override
        public void changedUpdate(DocumentEvent arg0) {
            eingabe();
        }

        private void eingabe() {
            setButtonImport();
        }
    }

    private class BeobPfad implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            //we can use native chooser on Mac...
            if (SystemInfo.isMacOSX()) {
                FileDialog chooser = new FileDialog(daten.mediathekGui, "Konfigdatei auswählen");
                chooser.setMode(FileDialog.LOAD);
                chooser.setVisible(true);
                if (chooser.getFile() != null) {
                    try {
                        jTextFieldDatei.setText(new File(chooser.getDirectory() + chooser.getFile()).getAbsolutePath());
                    } catch (Exception ex) {
                        Log.fehlerMeldung(304656587, Log.FEHLER_ART_PROG, "PanelImport.BeobPfad", ex);
                    }
                }
            } else {
                int returnVal;
                JFileChooser chooser = new JFileChooser();
                chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                chooser.setFileHidingEnabled(false);
                if (jTextFieldDatei.getText().equals("")) {
                    chooser.setCurrentDirectory(Daten.getMediathekXmlFilePath().toFile());
                } else {
                    chooser.setCurrentDirectory(new File(jTextFieldDatei.getText()));
                }
                returnVal = chooser.showOpenDialog(null);
                if (returnVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        jTextFieldDatei.setText(chooser.getSelectedFile().getAbsolutePath());
                    } catch (Exception ex) {
                        Log.fehlerMeldung(802039730, Log.FEHLER_ART_PROG, "PanelImport.BeobPfad", ex);
                    }
                }
            }
        }
    }
}
