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

package mediathek.windows;

import java.io.File;
import java.io.IOException;
import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class WindowsFileUtilsFfi {

    private static final MethodHandle SHFileOperationW;

    private static final int FO_DELETE = 3;
    private static final int FOF_ALLOWUNDO = 0x40;
    private static final int FOF_NOCONFIRMATION = 0x10;
    private static final int FOF_NO_UI = FOF_NOCONFIRMATION;

    private static final GroupLayout SHFILEOPSTRUCT_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("hwnd"),           // HWND
            ValueLayout.JAVA_INT.withName("wFunc"),          // UINT
            ValueLayout.ADDRESS.withName("pFrom"),           // PCZZWSTR
            ValueLayout.ADDRESS.withName("pTo"),             // PCZZWSTR
            ValueLayout.JAVA_SHORT.withName("fFlags"),       // FILEOP_FLAGS
            ValueLayout.JAVA_BOOLEAN.withName("fAnyOperationsAborted"), // BOOL
            ValueLayout.ADDRESS.withName("hNameMappings"),   // LPVOID
            ValueLayout.ADDRESS.withName("lpszProgressTitle") // LPCWSTR
    ).withName("SHFILEOPSTRUCT");

    static {
        var linker = Linker.nativeLinker();
        SymbolLookup shell32 = SymbolLookup.libraryLookup("shell32", Arena.global());
        SHFileOperationW = linker.downcallHandle(
                shell32.find("SHFileOperationW").orElseThrow(),
                FunctionDescriptor.of(ValueLayout.JAVA_INT, MemoryLayout.structLayout(SHFILEOPSTRUCT_LAYOUT))
        );
    }

    public static void moveToTrash(File... files) throws IOException {
        try (var arena = Arena.ofConfined()) {
            // Convert paths to double-null-terminated WCHAR strings
            var joinedPaths = Stream.of(files)
                    .map(File::getAbsolutePath)
                    .collect(Collectors.joining("\0")) + "\0\0";

            var pathSegment = arena.allocateFrom(joinedPaths);
            var shfileop = arena.allocate(SHFILEOPSTRUCT_LAYOUT);

            SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("wFunc")).set(shfileop, FO_DELETE);
            SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("pFrom")).set(shfileop, pathSegment);
            SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("pTo")).set(shfileop, MemorySegment.NULL);
            SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fFlags")).set(shfileop, (short) (FOF_ALLOWUNDO | FOF_NO_UI));
            SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fAnyOperationsAborted")).set(shfileop, false);
            SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("hNameMappings")).set(shfileop, MemorySegment.NULL);
            SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("lpszProgressTitle")).set(shfileop, MemorySegment.NULL);

            int result = (int) SHFileOperationW.invoke(shfileop);

            if (result != 0) {
                throw new IOException("Move to trash failed (code " + result + ")");
            }

            boolean aborted = (boolean) SHFILEOPSTRUCT_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("fAnyOperationsAborted")).get(shfileop);
            if (aborted) {
                throw new IOException("Move to trash aborted");
            }
        }
        catch (Throwable t) {
            throw new IOException("FFM moveToTrash failed", t);
        }
    }
}
