/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl;

// for inspecting themes
import javax.swing.*;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;
import java.lang.reflect.Method;

/**
 * A PLAFDetector provides a means to discover which versions of themes
 * are available on the host system.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public final class PLAFDetector {

    /**
     * Workaround method to get the metal theme, either "Steel" or "Ocean". This is because the
     * Metal look and feel's getName() property does not distinguish between versions correctly.
     * A bug has been submitted to the Sun Java bug database and will be reviewed.
     */
    public static String getMetalTheme() {
        try {
            MetalLookAndFeel metalLNF = (MetalLookAndFeel)UIManager.getLookAndFeel();
            Method getCurrentTheme = metalLNF.getClass().getMethod("getCurrentTheme", new Class[0]);
            MetalTheme currentTheme = (MetalTheme)getCurrentTheme.invoke(metalLNF, new Object[0]);
            return "Metal/" + currentTheme.getName();
        } catch(NoSuchMethodException e) {
            // must be Java 1.4 because getCurrentTheme() method does not exist
            // therefore the theme of interest is "Steel"
            return "Metal/Steel";
        } catch(Exception e) {
            e.printStackTrace();
            return "Metal/Steel";
        }
    }

}
