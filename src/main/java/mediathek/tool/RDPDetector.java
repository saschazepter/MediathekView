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

import com.sun.jna.Native;
import com.sun.jna.Pointer;
import com.sun.jna.platform.win32.User32;
import com.sun.jna.platform.win32.WinDef;
import com.sun.jna.platform.win32.WinNT;
import com.sun.jna.platform.win32.Wtsapi32;
import com.sun.jna.ptr.IntByReference;
import com.sun.jna.ptr.PointerByReference;
import com.sun.jna.win32.StdCallLibrary;

public class RDPDetector {
    public static final int WTS_CURRENT_SERVER_HANDLE = 0;
    public static final int WTSIsRemoteSession = 29;

    public static boolean isRemoteSessionAlt() {
        var result = User32.INSTANCE.GetSystemMetrics(User32.SM_REMOTESESSION);
        return result != 0;
    }

    public static boolean isRemoteSession() {
        var serverHandle = new WinNT.HANDLE(Pointer.createConstant(WTS_CURRENT_SERVER_HANDLE));
        final var sessionId = Kernel32.INSTANCE.WTSGetActiveConsoleSessionId();

        var buffer = new PointerByReference();
        var bytesReturned = new IntByReference();

        try {
            var success = Wtsapi32.INSTANCE.WTSQuerySessionInformation(serverHandle, sessionId,
            WTSIsRemoteSession, buffer, bytesReturned);
            if (success) {
                final int isRemote = buffer.getPointer().getInt(0);
                return isRemote != 0;
            }
            return false;
        } finally {
            if (buffer != null)
                Wtsapi32.INSTANCE.WTSFreeMemory(buffer.getValue());
        }
    }

    public interface Kernel32 extends StdCallLibrary {
        Kernel32 INSTANCE = Native.load("kernel32", Kernel32.class);

        int WTSGetActiveConsoleSessionId();
    }
}
