package mediathek.gui.dialogEinstellungen;

import mediathek.config.application.ApplicationConfiguration;
import mediathek.tool.*;
import mediathek.tool.models.NonEditableTableModel;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.EtchedBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.Optional;

public class PanelDateinamen extends JPanel {
    @FunctionalInterface
    interface AddReplacementRuleDialog {
        Optional<ReplaceEntry> show(Component parent);
    }

    private final ReplacementRules replacementRules;
    private final AddReplacementRuleDialog addReplacementRuleDialog;
    private boolean stopBeob;

    public PanelDateinamen(ReplacementRules replacementRules) {
        this(replacementRules, PanelDateinamen::showAddReplacementRuleDialog);
    }

    PanelDateinamen(ReplacementRules replacementRules, AddReplacementRuleDialog addReplacementRuleDialog) {
        this.replacementRules = replacementRules;
        this.addReplacementRuleDialog = addReplacementRuleDialog;
        initComponents();

        jLabelAlert.setVisible(false);
        jLabelAlert.setText("");
        jLabelAlert.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/triangle-exclamation.svg", 32f));
        jButtonPlus.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/plus.svg"));
        jButtonMinus.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/minus.svg"));
        jButtonUp.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/arrow-up.svg"));
        jButtonDown.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/arrow-down.svg"));
        jButtonReset.addActionListener(_ -> {
            replacementRules.initDefaults();
            reloadTable();
            updateTextFields();
        });
        jButtonPlus.addActionListener(_ -> addReplacementRuleDialog.show(this).ifPresent(entry -> {
            replacementRules.add(entry.getFrom(), entry.getTo());
            reloadTable();
            tabelle.setRowSelectionInterval(tabelle.getRowCount() - 1, tabelle.getRowCount() - 1);
            updateTextFields();
        }));
        jButtonMinus.addActionListener(_ -> {
            final int selectedTableRow = tabelle.getSelectedRow();
            if (selectedTableRow != -1) {
                replacementRules.removeAt(tabelle.convertRowIndexToModel(selectedTableRow));
                reloadTable();
                updateTextFields();
            }
        });
        jButtonUp.addActionListener(_ -> moveSelectedRule(true));
        jButtonDown.addActionListener(_ -> moveSelectedRule(false));
        reloadTable();
        updateTextFields();
        tabelle.getSelectionModel().addListSelectionListener(new BeobachterTableSelect());
        jTextFieldVon.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateFromText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateFromText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateFromText();
            }
        });
        jTextFieldNach.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                updateToText();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateToText();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateToText();
            }
        });

        var handler = new TextCopyPasteHandler<>(jTextFieldNach);
        jTextFieldNach.setComponentPopupMenu(handler.getPopupMenu());

        handler = new TextCopyPasteHandler<>(jTextFieldVon);
        jTextFieldVon.setComponentPopupMenu(handler.getPopupMenu());

        var applicationConfiguration = ApplicationConfiguration.getInstance();
        jCheckBoxTable.addActionListener(_ -> applicationConfiguration.setUseFilenameReplaceTable(jCheckBoxTable.isSelected()));
        jCheckBoxTable.setSelected(applicationConfiguration.getUseFilenameReplaceTable());

        jCheckBoxAscii.addActionListener(_ -> applicationConfiguration.setOnlyAsciiFilenames(jCheckBoxAscii.isSelected()));
        jCheckBoxAscii.setSelected(applicationConfiguration.getOnlyAsciiFilenames());
    }

    private void updateFromText() {
        if (!stopBeob) {
            final int selectedTableRow = tabelle.getSelectedRow();
            if (selectedTableRow != -1) {
                replacementRules.setFrom(tabelle.convertRowIndexToModel(selectedTableRow), jTextFieldVon.getText());
                reloadTable();
            }
        }
    }

    private void updateToText() {
        if (!stopBeob) {
            final int selectedTableRow = tabelle.getSelectedRow();
            if (selectedTableRow != -1) {
                replacementRules.setTo(tabelle.convertRowIndexToModel(selectedTableRow), jTextFieldNach.getText());
                reloadTable();
            }
        }
    }

    private void moveSelectedRule(boolean up) {
        final int rows = tabelle.getSelectedRow();
        if (rows != -1) {
            final int row = tabelle.convertRowIndexToModel(rows);
            final int newIndex = replacementRules.up(row, up);
            reloadTable();
            tabelle.setRowSelectionInterval(newIndex, newIndex);
            tabelle.scrollRectToVisible(tabelle.getCellRect(newIndex, 0, true));
        } else {
            NoSelectionErrorDialog.show(this);
        }

    }

    private static Optional<ReplaceEntry> showAddReplacementRuleDialog(Component parent) {
        var dialog = new JDialog(SwingUtilities.getWindowAncestor(parent), "Neue Ersetzungsregel", Dialog.ModalityType.APPLICATION_MODAL);
        var fromField = new JTextField(24);
        var toField = new JTextField(24);
        var okButton = new JButton("OK");
        var cancelButton = new JButton("Abbrechen");
        var result = new ReplaceEntry[1];

        okButton.setEnabled(false);
        fromField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateOkButton();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateOkButton();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateOkButton();
            }

            private void updateOkButton() {
                okButton.setEnabled(!fromField.getText().isEmpty());
            }
        });

        okButton.addActionListener(_ -> {
            result[0] = new ReplaceEntry(fromField.getText(), toField.getText());
            dialog.dispose();
        });
        cancelButton.addActionListener(_ -> dialog.dispose());

        var inputPanel = new JPanel(new GridBagLayout());
        var constraints = new GridBagConstraints();
        constraints.insets = new Insets(4, 4, 4, 4);
        constraints.anchor = GridBagConstraints.WEST;
        inputPanel.add(new JLabel("Von:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        inputPanel.add(fromField, constraints);
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.fill = GridBagConstraints.NONE;
        constraints.weightx = 0.0;
        inputPanel.add(new JLabel("Nach:"), constraints);
        constraints.gridx = 1;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        inputPanel.add(toField, constraints);

        var buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(okButton);
        buttonPanel.add(cancelButton);

        dialog.getContentPane().setLayout(new BorderLayout(8, 8));
        dialog.getContentPane().add(inputPanel, BorderLayout.CENTER);
        dialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
        dialog.getRootPane().setDefaultButton(okButton);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        fromField.requestFocusInWindow();
        dialog.setVisible(true);

        return Optional.ofNullable(result[0]);
    }

    private void reloadTable() {
        stopBeob = true;
        int selectedTableRow = tabelle.getSelectedRow();
        if (selectedTableRow != -1)
            selectedTableRow = tabelle.convertRowIndexToModel(selectedTableRow);

        var model = new NonEditableTableModel(new Object[][]{}, replacementRules.columnNames());
        model.setRowCount(0);
        for (ReplaceEntry entry : replacementRules.entries()) {
            model.addRow(entry.toArray());
        }

        tabelle.setModel(model);
        if (selectedTableRow != -1) {
            if (tabelle.getRowCount() > 0 && selectedTableRow < tabelle.getRowCount()) {
                tabelle.setRowSelectionInterval(selectedTableRow, selectedTableRow);
            } else if (tabelle.getRowCount() > 0 && selectedTableRow > 0) {
                tabelle.setRowSelectionInterval(tabelle.getRowCount() - 1, tabelle.getRowCount() - 1);
            } else if (tabelle.getRowCount() > 0) {
                tabelle.setRowSelectionInterval(0, 0);
            }
        } else if (tabelle.getRowCount() > 0) {
            tabelle.setRowSelectionInterval(0, 0);
        }
        jLabelAlert.setVisible(replacementRules.check());
        stopBeob = false;
    }

    private void updateTextFields() {
        stopBeob = true;
        final int selectedTableRow = tabelle.getSelectedRow();
        try {
            if (selectedTableRow != -1) {
                var model = tabelle.getModel();
                var modelRow = tabelle.convertRowIndexToModel(selectedTableRow);
                jTextFieldVon.setText(model.getValueAt(modelRow, ReplacementRules.VON_NR).toString());
                jTextFieldNach.setText(model.getValueAt(modelRow, ReplacementRules.NACH_NR).toString());
            } else {
                jTextFieldVon.setText("");
                jTextFieldNach.setText("");
            }
        } finally {
            stopBeob = false;
        }

        jTextFieldNach.setEnabled(selectedTableRow >= 0);
        jTextFieldVon.setEnabled(selectedTableRow >= 0);
        jButtonUp.setEnabled(selectedTableRow >= 0);
        jButtonDown.setEnabled(selectedTableRow >= 0);
        jLabelNach.setEnabled(selectedTableRow >= 0);
        jLabelVon.setEnabled(selectedTableRow >= 0);
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    private void initComponents() {
        var jTabbedPane1 = new JTabbedPane();
        var jPanel1 = new JPanel();
        var jScrollPane5 = new JScrollPane();
        var jTextArea3 = new JTextArea();
        var jPanel2 = new JPanel();
        jCheckBoxTable = new JCheckBox();
        var jPanel3 = new JPanel();
        jButtonReset = new JButton();
        var jScrollPane3 = new JScrollPane();
        var jTextArea2 = new JTextArea();
        var jScrollPane4 = new JScrollPane();
        tabelle = new JTable();
        jLabelAlert = new JLabel();
        jLabelVon = new JLabel();
        jTextFieldVon = new JTextField();
        jLabelNach = new JLabel();
        jTextFieldNach = new JTextField();
        jButtonMinus = new JButton();
        jButtonPlus = new JButton();
        jButtonDown = new JButton();
        jButtonUp = new JButton();
        jCheckBoxAscii = new JCheckBox();

        //======== this ========

        //======== jTabbedPane1 ========
        {

            //======== jPanel1 ========
            {

                //======== jScrollPane5 ========
                {

                    //---- jTextArea3 ----
                    jTextArea3.setEditable(false);
                    jTextArea3.setColumns(20);
                    jTextArea3.setRows(5);
                    jTextArea3.setText("\nDie Dateinamen werden f\u00fcr jedes Betriebssystem passend aufbereitet.\n\nWer will, kann dar\u00fcber hinaus weitere Einstellungen mit einer Ersetzungstabelle\nvornehmen: z.B. \"\u00df\" durch \"ss\" ersetzen.\n"); //NON-NLS
                    jTextArea3.setMargin(new Insets(3, 3, 3, 3));
                    jScrollPane5.setViewportView(jTextArea3);
                }

                GroupLayout jPanel1Layout = new GroupLayout(jPanel1);
                jPanel1.setLayout(jPanel1Layout);
                jPanel1Layout.setHorizontalGroup(
                    jPanel1Layout.createParallelGroup()
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jScrollPane5, GroupLayout.DEFAULT_SIZE, 773, Short.MAX_VALUE)
                            .addContainerGap())
                );
                jPanel1Layout.setVerticalGroup(
                    jPanel1Layout.createParallelGroup()
                        .addGroup(jPanel1Layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jScrollPane5, GroupLayout.PREFERRED_SIZE, 119, GroupLayout.PREFERRED_SIZE)
                            .addContainerGap(459, Short.MAX_VALUE))
                );
            }
            jTabbedPane1.addTab("Dateinamen", jPanel1); //NON-NLS

            //======== jPanel2 ========
            {

                //---- jCheckBoxTable ----
                jCheckBoxTable.setText("Ersetzungstabelle anwenden"); //NON-NLS

                //======== jPanel3 ========
                {
                    jPanel3.setBorder(new EtchedBorder());

                    //---- jButtonReset ----
                    jButtonReset.setText("Tabelle zur\u00fccksetzen"); //NON-NLS

                    //======== jScrollPane3 ========
                    {
                        jScrollPane3.setBorder(new EmptyBorder(1, 1, 1, 1));

                        //---- jTextArea2 ----
                        jTextArea2.setEditable(false);
                        jTextArea2.setBackground(UIManager.getColor("Label.background")); //NON-NLS
                        jTextArea2.setColumns(20);
                        jTextArea2.setRows(4);
                        jTextArea2.setText("Die Tabelle wird von oben nach unten abgearbeitet.\nEs ist also m\u00f6glich, dass eine Ersetzung durch eine weitere\nwieder ersetzt wird!"); //NON-NLS
                        jTextArea2.setBorder(new EmptyBorder(1, 1, 1, 1));
                        jScrollPane3.setViewportView(jTextArea2);
                    }

                    //======== jScrollPane4 ========
                    {

                        //---- tabelle ----
                        tabelle.setModel(new DefaultTableModel(
                            new Object[][] {
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null},
                                {null, null, null, null},
                            },
                            new String[] {
                                "Title 1", "Title 2", "Title 3", "Title 4" //NON-NLS
                            }
                        ));
                        jScrollPane4.setViewportView(tabelle);
                    }

                    //---- jLabelAlert ----
                    jLabelAlert.setText("Achtung"); //NON-NLS

                    //---- jLabelVon ----
                    jLabelVon.setText("von:"); //NON-NLS

                    //---- jLabelNach ----
                    jLabelNach.setText("nach:"); //NON-NLS

                    //---- jButtonMinus ----
                    jButtonMinus.setIcon(new ImageIcon(getClass().getResource("/mediathek/res/muster/button-remove.png"))); //NON-NLS

                    //---- jButtonPlus ----
                    jButtonPlus.setIcon(new ImageIcon(getClass().getResource("/mediathek/res/muster/button-add.png"))); //NON-NLS

                    //---- jButtonDown ----
                    jButtonDown.setIcon(new ImageIcon(getClass().getResource("/mediathek/res/muster/button-move-down.png"))); //NON-NLS

                    //---- jButtonUp ----
                    jButtonUp.setIcon(new ImageIcon(getClass().getResource("/mediathek/res/muster/button-move-up.png"))); //NON-NLS

                    GroupLayout jPanel3Layout = new GroupLayout(jPanel3);
                    jPanel3.setLayout(jPanel3Layout);
                    jPanel3Layout.setHorizontalGroup(
                        jPanel3Layout.createParallelGroup()
                            .addGroup(jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(jPanel3Layout.createParallelGroup()
                                    .addComponent(jScrollPane4)
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(jScrollPane3)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabelAlert))
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(jButtonReset)
                                        .addGap(0, 0, Short.MAX_VALUE))
                                    .addGroup(jPanel3Layout.createSequentialGroup()
                                        .addComponent(jLabelVon)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldVon, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                        .addComponent(jLabelNach)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jTextFieldNach, GroupLayout.PREFERRED_SIZE, 100, GroupLayout.PREFERRED_SIZE)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED, 111, Short.MAX_VALUE)
                                        .addComponent(jButtonUp)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonDown)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonPlus)
                                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(jButtonMinus)))
                                .addGap(15, 15, 15))
                    );
                    jPanel3Layout.setVerticalGroup(
                        jPanel3Layout.createParallelGroup()
                            .addGroup(GroupLayout.Alignment.TRAILING, jPanel3Layout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jScrollPane4, GroupLayout.DEFAULT_SIZE, 328, Short.MAX_VALUE)
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                    .addComponent(jLabelVon)
                                    .addComponent(jTextFieldVon, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabelNach)
                                    .addComponent(jTextFieldNach, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jButtonUp)
                                    .addComponent(jButtonDown)
                                    .addComponent(jButtonPlus)
                                    .addComponent(jButtonMinus))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                                .addGroup(jPanel3Layout.createParallelGroup(GroupLayout.Alignment.CENTER)
                                    .addComponent(jScrollPane3, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jLabelAlert))
                                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jButtonReset)
                                .addContainerGap())
                    );
                    jPanel3Layout.linkSize(SwingConstants.VERTICAL, new Component[] {jButtonDown, jButtonMinus, jButtonPlus, jButtonUp, jLabelNach, jLabelVon, jTextFieldNach, jTextFieldVon});
                }

                //---- jCheckBoxAscii ----
                jCheckBoxAscii.setText("Nur ASCII-Zeichen erlauben"); //NON-NLS
                jCheckBoxAscii.setToolTipText("<html>Es werden alle Zeichen \"\u00fcber 127\" ersetzt.  Auch Umlaute wie \"\u00f6 -> oe\" werden ersetzt.<br>Wenn die Ersetzungstabelle aktiv ist, wird sie vorher abgearbeitet.</html>"); //NON-NLS

                GroupLayout jPanel2Layout = new GroupLayout(jPanel2);
                jPanel2.setLayout(jPanel2Layout);
                jPanel2Layout.setHorizontalGroup(
                    jPanel2Layout.createParallelGroup()
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(jPanel2Layout.createParallelGroup()
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGap(21, 21, 21)
                                    .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                .addGroup(jPanel2Layout.createSequentialGroup()
                                    .addGroup(jPanel2Layout.createParallelGroup()
                                        .addComponent(jCheckBoxAscii)
                                        .addComponent(jCheckBoxTable))
                                    .addGap(0, 0, Short.MAX_VALUE)))
                            .addContainerGap())
                );
                jPanel2Layout.setVerticalGroup(
                    jPanel2Layout.createParallelGroup()
                        .addGroup(jPanel2Layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jCheckBoxTable)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel3, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGap(18, 18, 18)
                            .addComponent(jCheckBoxAscii)
                            .addContainerGap())
                );
            }
            jTabbedPane1.addTab("Eigene Einstellungen", jPanel2); //NON-NLS
        }

        GroupLayout layout = new GroupLayout(this);
        setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup()
                .addGroup(GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jTabbedPane1)
                    .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup()
                .addGroup(layout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(jTabbedPane1)
                    .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JCheckBox jCheckBoxTable;
    private JButton jButtonReset;
    private JTable tabelle;
    private JLabel jLabelAlert;
    private JLabel jLabelVon;
    private JTextField jTextFieldVon;
    private JLabel jLabelNach;
    private JTextField jTextFieldNach;
    private JButton jButtonMinus;
    private JButton jButtonPlus;
    private JButton jButtonDown;
    private JButton jButtonUp;
    private JCheckBox jCheckBoxAscii;
    // End of variables declaration//GEN-END:variables

    private class BeobachterTableSelect implements ListSelectionListener {

        @Override
        public void valueChanged(ListSelectionEvent event) {
            if (!stopBeob) {
                if (!event.getValueIsAdjusting()) {
                    stopBeob = true;
                    updateTextFields();
                    stopBeob = false;
                }
            }
        }
    }

}
