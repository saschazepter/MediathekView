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
 * Created by JFormDesigner on Sat Apr 05 16:55:37 CEST 2025
 */

package mediathek.javafx.filterpanel;

import javafx.embed.swing.JFXPanel;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * @author christianfranzke
 */
public class SwingFilterContentPane extends JPanel {
    public SwingFilterContentPane() {
        initComponents();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        fxPanel = new JFXPanel();
        label3 = new JLabel();
        label2 = new JLabel();

        //======== this ========
        setLayout(new MigLayout(
            new LC().insets("5").hideMode(3), //NON-NLS
            // columns
            new AC()
                .grow().align("left"), //NON-NLS
            // rows
            new AC()
                .grow().gap()
                .gap()
                .gap()
                .shrink(0)));

        //---- fxPanel ----
        fxPanel.setPreferredSize(new Dimension(500, 500));
        add(fxPanel, new CC().cell(0, 0).grow());

        //---- label3 ----
        label3.setText("row2"); //NON-NLS
        add(label3, new CC().cell(0, 2));

        //---- label2 ----
        label2.setText("row3"); //NON-NLS
        add(label2, new CC().cell(0, 3));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    public JFXPanel fxPanel;
    private JLabel label3;
    private JLabel label2;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
