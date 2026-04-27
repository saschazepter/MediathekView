/*
 * Copyright (c) 2025-2026 derreisende77.
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

import java.lang.reflect.InvocationTargetException;
import java.util.Optional;

public final class Main {
    public static Optional<SplashScreen> splashScreen = Optional.empty();

    static {
        System.setProperty("log4j.shutdownCallbackRegistry", "mediathek.tool.Log4jShutdownCallbackRegistry");
    }

    private Main() {
    }

    public static void main(String... args) {
        try {
            Class<?> startupClass = Class.forName("mediathek.MainStartup");
            startupClass.getMethod("main", String[].class).invoke(null, (Object) args);
        }
        catch (InvocationTargetException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            if (cause instanceof Error error) {
                throw error;
            }
            throw new IllegalStateException("Failed to start MediathekView", cause);
        }
        catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to start MediathekView", ex);
        }
    }
}
