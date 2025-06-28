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
package mediathek;

import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.jidesoft.popup.JidePopup;

import javax.swing.*;
import java.awt.*;

public class PopupDemo {

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FlatMacDarkLaf.setup();
            JFrame frame = new JFrame("Popup Demo");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLayout(new FlowLayout());

            JButton btnShowPopup = new JButton("S");

            btnShowPopup.addActionListener(e -> showPopup(btnShowPopup));

            frame.add(btnShowPopup);
            frame.setSize(400, 300);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }

    private static void showPopup(JButton anchor) {
        JPanel popupContent = new JPanel(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.gridx = 0;
        gbc.gridy = 0;

        popupContent.add(new JLabel("Name:"), gbc);

        gbc.gridx = 1;
        JTextField txtName = new JTextField(15);
        popupContent.add(txtName, gbc);

        gbc.gridx = 0;
        gbc.gridy++;
        gbc.gridwidth = 2;
        JButton btnOk = new JButton("Übernehmen");
        popupContent.add(btnOk, gbc);

        var popup = new JidePopup();
        popup.setMovable(false); // Bleibt an Ort und Stelle
        popup.setResizable(false);
        popup.setFocusable(true);
        popup.setTransient(true); // Schließt sich bei Klick außerhalb
        popup.setLayout(new BorderLayout());
        popup.add(popupContent, BorderLayout.CENTER);
        popup.setPreferredSize(new Dimension(250, 120));

        popup.showPopup(anchor);
    }
}
