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
    private static final String LOG4J_SHUTDOWN_CALLBACK_REGISTRY = "mediathek.tool.Log4jShutdownCallbackRegistry";
    private static final String STARTUP_CLASS_NAME = "mediathek.MainStartup";
    public static Optional<SplashScreen> splashScreen = Optional.empty();

    static {
        System.setProperty("log4j.shutdownCallbackRegistry", LOG4J_SHUTDOWN_CALLBACK_REGISTRY);
    }

    private Main() {
    }

    public static void main(String... args) {
        try {
            startKotlinMain(args);
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

    /**
     * Keep a Java entrypoint type for mixed Java/Kotlin compilation while delegating the real startup flow to Kotlin.
     */
    private static void startKotlinMain(String... args) throws ReflectiveOperationException {
        Class<?> startupClass = Class.forName(STARTUP_CLASS_NAME);
        startupClass.getMethod("main", String[].class).invoke(null, (Object) args);
    }
}
