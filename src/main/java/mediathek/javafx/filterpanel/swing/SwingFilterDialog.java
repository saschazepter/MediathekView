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

import mediathek.javafx.filterpanel.swing.zeitraum.SwingZeitraumSpinner;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * @author christianfranzke
 */
public class SwingFilterDialog extends JDialog {
    public SwingFilterDialog(Window owner) {
        super(owner);
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        checkBox2 = new JCheckBox();
        checkBox1 = new JCheckBox();
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
                .gap()
                ));

        //---- checkBox2 ----
        checkBox2.setText("text"); //NON-NLS
        contentPane.add(checkBox2, new CC().cell(0, 0, 3, 1));

        //---- checkBox1 ----
        checkBox1.setText("text"); //NON-NLS
        contentPane.add(checkBox1, new CC().cell(0, 1, 3, 1));

        //---- label1 ----
        label1.setText("Zeitraum:"); //NON-NLS
        contentPane.add(label1, new CC().cell(0, 2));
        contentPane.add(spinner1, new CC().cell(1, 2));

        //---- label2 ----
        label2.setText("Tage"); //NON-NLS
        contentPane.add(label2, new CC().cell(2, 2));
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    private JCheckBox checkBox2;
    private JCheckBox checkBox1;
    private JLabel label1;
    private SwingZeitraumSpinner spinner1;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
