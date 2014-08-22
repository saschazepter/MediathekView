/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.tool;

import com.jidesoft.utils.SystemInfo;
import java.io.File;
import java.security.CodeSource;
import java.util.ResourceBundle;
import mediathek.Main;
import mediathek.controller.Log;

public class Funktionen {
    public enum OperatingSystemType {
        UNKNOWN(""), WIN32("Windows"), WIN64("Windows"), LINUX("Linux"), MAC("Mac");
        private final String name;

        OperatingSystemType(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     * Detect and return the currently used operating system.
     *
     * @return The enum for supported Operating Systems.
     */
    public static OperatingSystemType getOs() {
        OperatingSystemType os = OperatingSystemType.UNKNOWN;

        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            if (System.getenv("ProgramFiles") != null) {
                // win 32Bit
                os = OperatingSystemType.WIN32;
            } else if (System.getenv("ProgramFiles(x86)") != null) {
                // win 64Bit
                os = OperatingSystemType.WIN64;
            }
        } else if (SystemInfo.isLinux()) {
            os = OperatingSystemType.LINUX;
        } else if (SystemInfo.isMacOSX()) {
            os = OperatingSystemType.MAC;
        }

        return os;
    }

    public static String getOsString() {
        return getOs().toString();
    }

    /**
     * Retrieve the path to the program jar file.
     * @return The program jar file path with a separator added.
     */
    public static String getPathJar() {
        String pFilePath = "pFile";
        File propFile = new File(pFilePath);
        if (!propFile.exists()) {
            try {
                CodeSource cS = Main.class.getProtectionDomain().getCodeSource();
                File jarFile = new File(cS.getLocation().toURI().getPath());
                String jarDir = jarFile.getParentFile().getPath();
                propFile = new File(jarDir + File.separator + pFilePath);
            } catch (Exception ignored) {
            }
        }
        String s = propFile.getAbsolutePath().replace(pFilePath, "");
        if (!s.endsWith(File.separator)) {
            s = s + File.separator;
        }
        return s;
    }

    public static String pathProgramIcons() {
        return getPathJar() + Konstanten.VERZEICHNIS_PROGRAMM_ICONS;
    }

    public static String pathSenderIcons() {
        return getPathJar() + Konstanten.VERZEICHNIS_SENDER_ICONS;
    }

    public static String getProgVersionString() {
        return Konstanten.PROGRAMMNAME + " " + Konstanten.VERSION + "  [Buildnummer: " + getBuildNr() + "]";
    }

    public static String[] getJavaVersion() {
        String[] ret = new String[4];

        ret[0] = "Vendor: " + System.getProperty("java.vendor");
        ret[1] = "VMname: " + System.getProperty("java.vm.name");
        ret[2] = "Version: " + System.getProperty("java.version");
        ret[3] = "Runtimeversion: " + System.getProperty("java.runtime.version");
        return ret;
    }

    public static String getCompileDate() {
        final ResourceBundle rb;
        String propToken = "DATE";
        String msg = "";
        try {
            ResourceBundle.clearCache();
            rb = ResourceBundle.getBundle("version");
            msg = rb.getString(propToken);
        } catch (Exception e) {
            Log.fehlerMeldung(807293847,  Funktionen.class.getName(), e);
        }
        return msg;
    }

    public static String getBuildNr() {
        final ResourceBundle rb;
        String propToken = "BUILD";
        String msg = "";
        try {
            ResourceBundle.clearCache();
            rb = ResourceBundle.getBundle("version");
            msg = rb.getString(propToken);
        } catch (Exception e) {
            Log.fehlerMeldung(134679898, Funktionen.class.getName(), e);
        }
        return msg;
    }

//    public enum ReleaseType { DEBUG, RELEASE}
//    public static ReleaseType getReleaseType()
//    {
//        ReleaseType releaseType;
//        try {
//            ResourceBundle.clearCache();
//            ResourceBundle rb = ResourceBundle.getBundle("version");
//            String msg = rb.getString("TYPE");
//            switch(msg) {
//                case "DEBUG":
//                    releaseType = ReleaseType.DEBUG;
//                    break;
//                default:
//                    releaseType = ReleaseType.RELEASE;
//                    break;
//            }
//        } catch (Exception ex) {
//            //in case of an exception always pretend we are in Release mode...
//            ex.printStackTrace();
//            releaseType = ReleaseType.DEBUG;////////////////
//        }
//
//        return releaseType;
//    }
}
