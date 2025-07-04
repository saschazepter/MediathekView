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

package mediathek.mac;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

/**
 * Prevents system sleep on macOS
 */
public class OsxPowerManager {
    private Process caffeinateProcess = null;
    private static final Logger logger = LogManager.getLogger(OsxPowerManager.class);

    public void disablePowerManagement() {
        //we already have pm disabled..
        if (caffeinateProcess != null)
            return;

        try {
            ProcessBuilder pb = new ProcessBuilder("/usr/bin/caffeinate");
            caffeinateProcess = pb.start();
            logger.trace("power management disabled");
        }
        catch (IOException e) {
            caffeinateProcess = null;
            logger.error("disabling power management failed", e);
        }
    }

    public void enablePowerManagement() {
            if (caffeinateProcess != null) {
                caffeinateProcess.destroy();
            }

            caffeinateProcess = null;
            logger.trace("power management enabled");
    }
}
