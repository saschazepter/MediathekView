package mediathek.gui.dialogEinstellungen;

import mediathek.config.Daten;
import mediathek.daten.ListePset;
import mediathek.gui.dialogEinstellungen.pset.PanelPsetKurz;
import mediathek.gui.dialogEinstellungen.pset.PanelPsetLang;
import mediathek.tool.EscapeKeyHandler;

import javax.swing.*;

public class DialogImportPset extends JDialog {
    public boolean ok = false;
    private final ListePset liste;
    private final Daten ddaten;
    private final JFrame parentComponent;

    public DialogImportPset(JFrame parent, boolean modal, Daten dd, ListePset lliste) {
        super(parent, modal);
        parentComponent = parent;
        initComponents();
        ddaten = dd;
        this.setTitle("Programmset");
        liste = lliste;
        jScrollPane1.setViewportView(new PanelPsetKurz(ddaten, parentComponent, liste));
        jButtonOk.addActionListener(e -> disposeWithCode(true));
        jButtonAbbrechen.addActionListener(e -> disposeWithCode(false));

        EscapeKeyHandler.installHandler(this, () -> disposeWithCode(false));

        jCheckBoxAlleEinstellungen.addActionListener(e -> {
            if (jCheckBoxAlleEinstellungen.isSelected()) {
                jScrollPane1.setViewportView(new PanelPsetLang(ddaten, parentComponent, liste));
            } else {
                jScrollPane1.setViewportView(new PanelPsetKurz(ddaten, parentComponent, liste));
            }
        });
    }

    private void disposeWithCode(boolean ok) {
        this.ok = ok;
        dispose();
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        jCheckBoxAlleEinstellungen = new javax.swing.JCheckBox();
        jButtonOk = new javax.swing.JButton();
        jButtonAbbrechen = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

        jCheckBoxAlleEinstellungen.setText("alle Einstellungen anzeigen");

        jButtonOk.setText("Ok");

        jButtonAbbrechen.setText("Abbrechen");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jCheckBoxAlleEinstellungen)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 328, Short.MAX_VALUE)
                        .addComponent(jButtonOk, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButtonAbbrechen)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jButtonAbbrechen, jButtonOk});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 516, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jCheckBoxAlleEinstellungen)
                    .addComponent(jButtonAbbrechen)
                    .addComponent(jButtonOk))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton jButtonAbbrechen;
    private javax.swing.JButton jButtonOk;
    private javax.swing.JCheckBox jCheckBoxAlleEinstellungen;
    private javax.swing.JScrollPane jScrollPane1;
    // End of variables declaration//GEN-END:variables
}
