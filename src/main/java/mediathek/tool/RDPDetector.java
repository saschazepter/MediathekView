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

package mediathek.tool;

import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.win32.StdCallLibrary;

public class RDPDetector {
    public static final int WTS_CURRENT_SERVER_HANDLE = 0;
    public static final int WTSIsRemoteSession = 29;

    public static boolean isRemoteSession() {
        WinNT.HANDLE serverHandle = new WinNT.HANDLE(Pointer.createConstant(WTS_CURRENT_SERVER_HANDLE));
        final var sessionId = Kernel32.INSTANCE.WTSGetActiveConsoleSessionId();

        Pointer ppBuffer = new Memory(Native.POINTER_SIZE);
        IntByReference bytesReturned = new IntByReference();

        try {
            boolean success = Wtsapi32.INSTANCE.WTSQuerySessionInformation(
                    serverHandle,
                    sessionId,
                    WTSIsRemoteSession,
                    ppBuffer,
                    bytesReturned
            );

            if (success) {
                int isRemote = ppBuffer.getInt(0);
                return isRemote != 0;
            }
            return false;
        } finally {
            if (ppBuffer != null) {
                Wtsapi32.INSTANCE.WTSFreeMemory(ppBuffer);
            }
        }
    }
    public interface Wtsapi32 extends StdCallLibrary {
        Wtsapi32 INSTANCE = Native.load("Wtsapi32", Wtsapi32.class);

        boolean WTSQuerySessionInformation(
                WinNT.HANDLE hServer,
                int sessionId,
                int infoClass,
                Pointer ppBuffer,
                IntByReference pBytesReturned
        );

        void WTSFreeMemory(Pointer pointer);
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        int WTSGetActiveConsoleSessionId();
    }
}
