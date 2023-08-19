/*
 * Created by JFormDesigner on Sat Aug 19 13:40:47 CEST 2023
 */

package mediathek.gui.tabs.cellrenderer;

import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;

/**
 * @author christianfranzke
 */
public class TitleCell extends JPanel {
    public TitleCell() {
        initComponents();

        title.setText("Hello Title");
        title.setBackground(Color.blue);
        icons.setBackground(Color.red);
        icons.setText("");
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        // Generated using JFormDesigner non-commercial license
        title = new JLabel();
        icons = new JLabel();

        //======== this ========
        setLayout(new MigLayout(
            new LC().fillX().insets("0").hideMode(3), //NON-NLS
            // columns
            new AC()
                .grow().fill().gap()
                .align("right"), //NON-NLS
            // rows
            new AC()
                .grow().align("center"))); //NON-NLS

        //---- title ----
        title.setText("text"); //NON-NLS
        add(title, new CC().cell(0, 0).growY());

        //---- icons ----
        icons.setText("blablabla"); //NON-NLS
        add(icons, new CC().cell(1, 0).alignX("trailing").grow(0, 100)); //NON-NLS
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    // Generated using JFormDesigner non-commercial license
    JLabel title;
    JLabel icons;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
