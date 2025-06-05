package mediathek.windows;

import java.lang.foreign.*;

public class WindowsVersionHelper {

    // Constants for VerSetConditionMask dwTypeBitMask parameter
    private static final int VER_MINORVERSION = 0x0000001;
    private static final int VER_MAJORVERSION = 0x0000002;
    private static final int VER_SERVICEPACKMAJOR = 0x0000020;

    // Constants for VerSetConditionMask dwCondition parameter
    private static final byte VER_GREATER_EQUAL = 3;

    private static final GroupLayout OSVERSIONINFOEXW_LAYOUT = MemoryLayout.structLayout(
            ValueLayout.JAVA_INT.withName("dwOSVersionInfoSize"),
            ValueLayout.JAVA_INT.withName("dwMajorVersion"),
            ValueLayout.JAVA_INT.withName("dwMinorVersion"),
            ValueLayout.JAVA_INT.withName("dwBuildNumber"),
            ValueLayout.JAVA_INT.withName("dwPlatformId"),
            MemoryLayout.sequenceLayout(128, ValueLayout.JAVA_CHAR).withName("szCSDVersion"),
            ValueLayout.JAVA_SHORT.withName("wServicePackMajor"),
            ValueLayout.JAVA_SHORT.withName("wServicePackMinor"),
            ValueLayout.JAVA_SHORT.withName("wSuiteMask"),
            ValueLayout.JAVA_BYTE.withName("wProductType"),
            ValueLayout.JAVA_BYTE.withName("wReserved")
    ).withName("OSVERSIONINFOEXW");

    private static final long OSVERSIONINFOEXW_STRUCT_SIZE = OSVERSIONINFOEXW_LAYOUT.byteSize();


    public static boolean IsWindows10OrGreater() throws Throwable {
        return isWindowsVersionOrGreater(10,0,0);
    }

    public static boolean isWindowsVersionOrGreater(int major, int minor, int servicePackMajor) throws Throwable {
        try (Arena arena = Arena.ofConfined()) {
            SymbolLookup kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global());

            var NATIVE_LINKER = Linker.nativeLinker();
            var MH_VerSetConditionMask = NATIVE_LINKER.downcallHandle(
                    kernel32.find("VerSetConditionMask").orElseThrow(() -> new UnsatisfiedLinkError("VerSetConditionMask not found")),
                    FunctionDescriptor.of(ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_BYTE)
            );

            var MH_VerifyVersionInfoW = NATIVE_LINKER.downcallHandle(
                    kernel32.find("VerifyVersionInfoW").orElseThrow(() -> new UnsatisfiedLinkError("VerifyVersionInfoW not found")),
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_LONG)
            );
            MemorySegment osvi = arena.allocate(OSVERSIONINFOEXW_LAYOUT);

            var VH_dwOSVersionInfoSize = OSVERSIONINFOEXW_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("dwOSVersionInfoSize"));
            var VH_dwMajorVersion = OSVERSIONINFOEXW_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("dwMajorVersion"));
            var VH_dwMinorVersion = OSVERSIONINFOEXW_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("dwMinorVersion"));
            var VH_wServicePackMajor = OSVERSIONINFOEXW_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("wServicePackMajor"));

            VH_dwOSVersionInfoSize.set(osvi, 0L, (int) OSVERSIONINFOEXW_STRUCT_SIZE);
            VH_dwMajorVersion.set(osvi, 0L, major);
            VH_dwMinorVersion.set(osvi, 0L, minor);
            VH_wServicePackMajor.set(osvi, 0L, (short) servicePackMajor);

            long conditionMask = 0L;
            conditionMask = (long) MH_VerSetConditionMask.invokeExact(conditionMask, VER_MAJORVERSION, VER_GREATER_EQUAL);
            conditionMask = (long) MH_VerSetConditionMask.invokeExact(conditionMask, VER_MINORVERSION, VER_GREATER_EQUAL);
            conditionMask = (long) MH_VerSetConditionMask.invokeExact(conditionMask, VER_SERVICEPACKMAJOR, VER_GREATER_EQUAL);

            int typeMask = VER_MAJORVERSION | VER_MINORVERSION | VER_SERVICEPACKMAJOR;

            int result = (int) MH_VerifyVersionInfoW.invokeExact(osvi, typeMask, conditionMask);

            return result != 0;
        }
    }

    public static void main(String[] args) {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            System.out.println("This version check is designed for Windows OS.");
            return;
        }

        try {
            var v = new WindowsVersionHelper();
            boolean isWin10OrGreater = v.isWindowsVersionOrGreater(10, 0, 0);
            System.out.println("Is current OS Windows 10.0 SP0 or greater? " + isWin10OrGreater);

            boolean isWin7OrGreater = v.isWindowsVersionOrGreater(6, 1, 0);
            System.out.println("Is current OS Windows 7 (6.1 SP0) or greater? " + isWin7OrGreater);

            boolean isFutureVersion = v.isWindowsVersionOrGreater(99, 0, 0);
            System.out.println("Is current OS Windows 99.0 SP0 or greater? " + isFutureVersion);

        } catch (Throwable t) {
            System.err.println("An error occurred during the Windows version check:");
            t.printStackTrace();
        }
    }
}