/*    
 *    MediathekView
 *    Copyright (C) 2008   W. Xaver
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
package mediathek.gui.dialog;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import mediathek.controller.io.starter.Start;
import mediathek.controller.io.starter.StarterClass;
import mediathek.daten.DDaten;
import mediathek.daten.DatenDownload;
import mediathek.tool.EscBeenden;

public class DialogEditDownload extends javax.swing.JDialog {

    private DatenDownload download;
    private JTextField[] textfeldListe;
    private JLabel[] labelListe;
    private JCheckBox jCheckBox = new JCheckBox(DatenDownload.DOWNLOAD_PROGRAMM_RESTART);
    private DDaten ddaten;
    private Start start = null;
    public boolean ok = false;

    public DialogEditDownload(java.awt.Frame parent, boolean modal, DDaten dd, DatenDownload ddownload) {
        super(parent, modal);
        initComponents();
        download = ddownload;
        ddaten = dd;
        String url = download.arr[DatenDownload.DOWNLOAD_URL_NR];
        start = ddaten.starterClass.getStart(url);
        jButtonBeenden.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                check();
                beenden();
            }
        });
        jButtonAbbrechen.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                beenden();
            }
        });
        getRootPane().setDefaultButton(jButtonBeenden);
        new EscBeenden(this) {
            @Override
            public void beenden_() {
                beenden();
            }
        };
        setExtra();
    }

    private void setExtra() {
        textfeldListe = new JTextField[DatenDownload.DOWNLOAD_MAX_ELEM];
        labelListe = new JLabel[DatenDownload.DOWNLOAD_MAX_ELEM];
        GridBagLayout gridbag = new GridBagLayout();
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(5, 10, 10, 5);
        jPanelExtra.setLayout(gridbag);
        int zeile = 0;
        for (int i = 0; i < DatenDownload.DOWNLOAD_MAX_ELEM; ++i) {
            JLabel label = new JLabel("  " + DatenDownload.DOWNLOAD_COLUMN_NAMES[i] + ": ");
            labelListe[i] = label;
            JTextField textfeld = new JTextField();
            textfeld.setEditable(false);
            textfeld.setText(download.arr[i]);
            textfeldListe[i] = textfeld;
            addExtraFeld(i, gridbag, c);
            ++zeile;
            c.gridy = zeile;
        }
    }

    private void addExtraFeld(int i, GridBagLayout gridbag, GridBagConstraints c) {
        //Label
        c.gridx = 0;
        c.weightx = 0;
        if (i == DatenDownload.DOWNLOAD_ZURUECKGESTELLT_NR || i == DatenDownload.DOWNLOAD_URL_AUTH_NR || i == DatenDownload.DOWNLOAD_URL_RTMP_NR) {
            // ist eigentlich Unsinn, es anzuzeigen
            return;
        }
        if (i == DatenDownload.DOWNLOAD_PROGRAMM_RESTART_NR) {
            labelListe[i].setForeground(Color.BLUE);
            jCheckBox.setSelected(download.isRestart());
            jCheckBox.addActionListener(new BeobCheckbox());
            gridbag.setConstraints(labelListe[i], c);
            jPanelExtra.add(labelListe[i]);
            c.gridx = 1;
            c.weightx = 10;
            gridbag.setConstraints(jCheckBox, c);
            jPanelExtra.add(jCheckBox);
        } else {
            if (i == DatenDownload.DOWNLOAD_PROGRAMM_AUFRUF_NR) {
                labelListe[i].setForeground(Color.BLUE);
                if (download.getArt() != Start.ART_DOWNLOAD) {
                    textfeldListe[i].setEditable(true);
                    textfeldListe[i].getDocument().addDocumentListener(new BeobachterDocumentTextfeld(i));
                }
            } else if (i == DatenDownload.DOWNLOAD_PROGRESS_NR) {
                textfeldListe[i].setText(StarterClass.getTextProgress(start));
            } else if (i == DatenDownload.DOWNLOAD_RESTZEIT_NR) {
                textfeldListe[i].setText(download.getTextRestzeit(ddaten, start));
            } else if (i == DatenDownload.DOWNLOAD_ART_NR) {
                switch (download.getArt()) {
                    case Start.ART_DOWNLOAD:
                        textfeldListe[i].setText(Start.ART_DOWNLOAD_TXT);
                        break;
                    case Start.ART_PROGRAMM:
                        textfeldListe[i].setText(Start.ART_PROGRAMM_TXT);
                        break;
                }
            } else if (i == DatenDownload.DOWNLOAD_QUELLE_NR) {
                switch (download.getQuelle()) {
                    case Start.QUELLE_ALLE:
                        textfeldListe[i].setText(Start.QUELLE_ALLE_TXT);
                        break;
                    case Start.QUELLE_ABO:
                        textfeldListe[i].setText(Start.QUELLE_ABO_TXT);
                        break;
                    case Start.QUELLE_BUTTON:
                        textfeldListe[i].setText(Start.QUELLE_BUTTON_TXT);
                        break;
                    case Start.QUELLE_DOWNLOAD:
                        textfeldListe[i].setText(Start.QUELLE_DOWNLOAD_TXT);
                        break;
                }
            }
            gridbag.setConstraints(labelListe[i], c);
            jPanelExtra.add(labelListe[i]);
            //Textfeld
            c.gridx = 1;
            c.weightx = 10;
            gridbag.setConstraints(textfeldListe[i], c);
            jPanelExtra.add(textfeldListe[i]);
        }
    }

    private void check() {
        ok = true;
    }

    private void beenden() {
        this.dispose();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jPanelExtra = new javax.swing.JPanel();
        jButtonBeenden = new javax.swing.JButton();
        jButtonAbbrechen = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(500, 0));

        javax.swing.GroupLayout jPanelExtraLayout = new javax.swing.GroupLayout(jPanelExtra);
        jPanelExtra.setLayout(jPanelExtraLayout);
        jPanelExtraLayout.setHorizontalGroup(
            jPanelExtraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 648, Short.MAX_VALUE)
        );
        jPanelExtraLayout.setVerticalGroup(
            jPanelExtraLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 392, Short.MAX_VALUE)
        );

        jScrollPane1.setViewportView(jPanelExtra);

        jButtonBeenden.setText("Ok");

        jButtonAbbrechen.setText("Abbrechen");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(jButtonBeenden, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAbbrechen)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonAbbrechen, jButtonBeenden});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButtonAbbrechen)
                    .addComponent(jButtonBeenden))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAbbrechen;
    private javax.swing.JButton jButtonBeenden;
    private javax.swing.JPanel jPanelExtra;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables

    private class BeobachterDocumentTextfeld implements DocumentListener {

        int nr;

        public BeobachterDocumentTextfeld(int n) {
            nr = n;
        }

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
            download.arr[nr] = textfeldListe[nr].getText().trim();
        }
    }

    private class BeobCheckbox implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            download.arr[DatenDownload.DOWNLOAD_PROGRAMM_RESTART_NR] = Boolean.toString(jCheckBox.isSelected());
        }
    }
}
