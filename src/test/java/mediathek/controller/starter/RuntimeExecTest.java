package mediathek.controller.starter;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RuntimeExecTest {

    @Test
    void parsesFfmpegDurationFromInputHeader() {
        var input = "  Duration: 02:46:33.40, start: 0.141000, bitrate: N/A";

        var duration = RuntimeExec.parseDurationSeconds(input);

        assertTrue(duration.isPresent());
        assertEquals(9993.40, duration.getAsDouble(), 0.0001);
    }

    @Test
    void parsesFfmpeg81ProgressTime() {
        var input = "frame=  138 fps=0.0 q=-1.0 size=    4608KiB time=00:00:05.39 bitrate=6994.0kbits/s speed=10.8x elapsed=0:00:00.50";

        var progress = RuntimeExec.parseProgressTimeSeconds(input);

        assertTrue(progress.isPresent());
        assertEquals(5.39, progress.getAsDouble(), 0.0001);
    }

    @Test
    void parsesBinarySizeUnitsFromFfmpeg81() {
        var input = "frame=  138 fps=0.0 q=-1.0 size=    4608KiB time=00:00:05.39 bitrate=6994.0kbits/s speed=10.8x elapsed=0:00:00.50";

        var size = RuntimeExec.parseSizeBytes(input);

        assertTrue(size.isPresent());
        assertEquals(4_718_592L, size.getAsLong());
    }

    @Test
    void parsesLegacyDecimalSizeUnits() {
        var input = "frame=  147 fps= 17 q=-1.0 size=    1588kB time=00:00:05.84 bitrate=2226.0kbits/s";

        var size = RuntimeExec.parseSizeBytes(input);

        assertTrue(size.isPresent());
        assertEquals(1_588_000L, size.getAsLong());
    }

    @Test
    void ignoresNonProgressLinesWithoutSize() {
        var input = "Press [q] to stop, [?] for help";

        assertFalse(RuntimeExec.parseSizeBytes(input).isPresent());
        assertFalse(RuntimeExec.parseProgressTimeSeconds(input).isPresent());
    }
}
