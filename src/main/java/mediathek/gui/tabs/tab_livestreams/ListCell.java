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
 * Created by JFormDesigner on Tue Jun 24 13:55:46 CEST 2025
 */

package mediathek.gui.tabs.tab_livestreams;

import mediathek.gui.tabs.SenderIconLabel;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * @author christianfranzke
 */
public class ListCell extends JPanel {
    public ListCell() {
        initComponents();

        var size = new Dimension(215,116);
        setPreferredSize(size);
        setMinimumSize(size);
    }

    public void setSubtitle(String subtitle) {
        if (subtitle.isEmpty()) {
            lblSubtitle.setVisible(false);
        }
        else {
            lblSubtitle.setText(subtitle);
            lblSubtitle.setVisible(true);
        }
    }

    public void setSubtitleForegroundColor(Color color) {
        lblSubtitle.setForeground(color);
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        lblSender = new SenderIconLabel();
        var panel1 = new JPanel();
        lblTitle = new JLabel();
        lblSubtitle = new JLabel();
        lblZeitraum = new JLabel();
        progressBar = new JProgressBar();

        //======== this ========
        setOpaque(false);
        setMinimumSize(new Dimension(215, 116));
        setLayout(new MigLayout(
            new LC().insets("0").hideMode(3),
            // columns
            new AC()
                .fill().gap()
                .grow().fill(),
            // rows
            new AC()
                ));

        //---- lblSender ----
        lblSender.setToolTipText("SerIconLabl");
        add(lblSender, new CC().cell(0, 0));

        //======== panel1 ========
        {
            panel1.setOpaque(false);
            panel1.setLayout(new MigLayout(
                new LC().hideMode(3),
                // columns
                new AC()
                    .grow().fill(),
                // rows
                new AC()
                    .gap()
                    .gap()
                    .gap()
                    ));

            //---- lblTitle ----
            lblTitle.setText("Titel");
            lblTitle.setFont(lblTitle.getFont().deriveFont(lblTitle.getFont().getStyle() | Font.BOLD));
            panel1.add(lblTitle, new CC().cell(0, 0));

            //---- lblSubtitle ----
            lblSubtitle.setText("Subtitel");
            panel1.add(lblSubtitle, new CC().cell(0, 1));

            //---- lblZeitraum ----
            lblZeitraum.setText("Zeitraum");
            panel1.add(lblZeitraum, new CC().cell(0, 2));
            panel1.add(progressBar, new CC().cell(0, 3).growX());
        }
        add(panel1, new CC().cell(1, 0));
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    public SenderIconLabel lblSender;
    public JLabel lblTitle;
    private JLabel lblSubtitle;
    public JLabel lblZeitraum;
    public JProgressBar progressBar;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
