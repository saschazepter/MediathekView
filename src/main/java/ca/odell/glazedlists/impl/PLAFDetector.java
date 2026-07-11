/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl;

import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.plaf.metal.MetalTheme;

/**
 * A PLAFDetector provides a means to discover which versions of themes
 * are available on the host system.
 *
 * @author <a href="mailto:jesse@swank.ca">Jesse Wilson</a>
 */
public final class PLAFDetector {

    /**
     * Gets the current Metal theme because the look and feel name does not
     * distinguish between themes.
     */
    public static String getMetalTheme() {
        MetalTheme currentTheme = MetalLookAndFeel.getCurrentTheme();
        return "Metal/" + currentTheme.getName();
    }

}
