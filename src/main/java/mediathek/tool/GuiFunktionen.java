/*
 * Copyright (c) 2026 derreisende77.
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

package mediathek.tool;

import mediathek.config.MVConfig;
import mediathek.config.MVConfig.Configs;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.intellij.lang.annotations.MagicConstant;
import org.jspecify.annotations.NonNull;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.InputEvent;
import java.io.File;

public class GuiFunktionen {

    private static final Logger logger = LogManager.getLogger();
    /**
     * Property string to indicate usage of install4j's external updater.
     */
    private static final String EXTERNAL_UPDATE_PROPERTY = "externalUpdateCheck";

    /**
     * Check whether or not we are using Install4j's external update mechanism.
     *
     * @return true if it is NOT used, true otherwise.
     */
    public static boolean isNotUsingExternalUpdater() {
        var externalUpdateCheck = System.getProperty(EXTERNAL_UPDATE_PROPERTY);
        boolean ret = false;
        if (externalUpdateCheck != null) {
            if (externalUpdateCheck.equalsIgnoreCase("true") || externalUpdateCheck.isEmpty())
                ret = true;
        }

        return !ret;
    }

    /**
     * Show a "red box" around a component to indicate error condition
     * @param component the target
     * @param hasError if true, set error box around component, otherwise remove it.
     */
    public static void showErrorIndication(@NonNull JComponent component, boolean hasError) {
        if (hasError)
            component.putClientProperty("JComponent.outline", "error");
        else
            component.putClientProperty("JComponent.outline", "");
    }

    public static boolean isUsingExternalUpdater() {
        return !isNotUsingExternalUpdater();
    }
    /**
     * Copy string to system clipboard.
     * @param s the data for the clipboard.
     */
    public static void copyToClipboard(@NonNull String s) {
        Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(s), null);
    }

    public static String addsPfad(String pfad1, String pfad2) {
        String ret = concatPaths(pfad1, pfad2);
        if (ret.isEmpty()) {
            logger.error("addsPfad({},{}):", pfad1, pfad2);
        }
        return ret;
    }

    public static String concatPaths(String pfad1, String pfad2) {
        if (pfad1 == null || pfad2 == null) {
            return "";
        }
        if (pfad1.isEmpty() || pfad2.isEmpty()) {
            return pfad1 + pfad2;
        }

        while (pfad1.endsWith(File.separator)) {
            pfad1 = pfad1.substring(0, pfad1.length() - 1);
        }
        if (pfad2.startsWith(File.separator)) {
            return pfad1 + pfad2;
        } else {
            return pfad1 + File.separator + pfad2;
        }
    }

    public static String cutName(String name, int length) {
        if (name.length() > length) {
            name = name.substring(0, length - 4) + name.substring(name.length() - 4);
        }
        return name;
    }

    public static String getDateiName(String pfad) {
        //Dateinamen einer URL extrahieren
        String ret = "";
        if (pfad != null) {
            if (!pfad.isEmpty()) {
                ret = pfad.substring(pfad.lastIndexOf('/') + 1);
            }
        }
        if (ret.contains("?")) {
            ret = ret.substring(0, ret.indexOf('?'));
        }
        if (ret.contains("&")) {
            ret = ret.substring(0, ret.indexOf('&'));
        }
        if (ret.isEmpty()) {
            logger.error("getDateiName({})", pfad);
        }
        return ret;
    }

    /**
     * Maps the "command" key to the correspondig icon based on operating system.
     *
     * @return an InputEvent modifier based on operating system.
     */
    @MagicConstant(flagsFromClass = java.awt.event.InputEvent.class)
    public static int getPlatformControlKey() {
        int result;

        if (SystemUtils.IS_OS_MAC_OSX) {
            result = InputEvent.META_DOWN_MASK;
        } else {
            result = InputEvent.CTRL_DOWN_MASK;
        }

        return result;
    }

}
