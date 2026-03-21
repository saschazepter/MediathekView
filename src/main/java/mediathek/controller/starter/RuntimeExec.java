/*
 * Copyright (c) 2008-2026 derreisende77.
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

package mediathek.controller.starter;

import mediathek.config.Config;
import mediathek.tool.MVFilmSize;
import mediathek.tool.ProcessCommandUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.OptionalDouble;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Responsible for the interaction with ffmpeg/avconv.
 */
public class RuntimeExec {
    public static final String TRENNER_PROG_ARRAY = "<>";
    private static final Pattern PATTERN_FFMPEG = Pattern.compile("(?<= {2}Duration: )[^,]*"); // Duration: 00:00:30.28, start: 0.000000, bitrate: N/A
    private static final Pattern PATTERN_TIME = Pattern.compile("(?<=time=)[^ ]*"); // frame=  147 fps= 17 q=-1.0 size=    1588kB time=00:00:05.84 bitrate=2226.0kbits/s
    private static final Pattern PATTERN_SIZE = Pattern.compile("(?<=size=)\\s*\\d+(?:\\.\\d+)?\\s*[KMG]?i?B", Pattern.CASE_INSENSITIVE);
    private static final Logger logger = LogManager.getLogger();
    private final String strProgCall;
    private final ProgressTracker progressTracker = new ProgressTracker();
    private Start start;
    private MVFilmSize mVFilmSize;
    private String[] arrProgCallArray;
    private String strProgCallArray = "";

    public RuntimeExec(MVFilmSize mVFilmSize, Start start,
                       String strProgCall, String strProgCallArray) {
        this.mVFilmSize = mVFilmSize;
        this.start = start;
        this.strProgCall = strProgCall;
        this.arrProgCallArray = strProgCallArray.split(TRENNER_PROG_ARRAY);
        this.strProgCallArray = strProgCallArray;
        if (arrProgCallArray.length <= 1) {
            arrProgCallArray = null;
        }
    }

    public RuntimeExec(String p) {
        strProgCall = p;
    }

    static OptionalDouble parseDurationSeconds(String input) {
        Matcher matcher = PATTERN_FFMPEG.matcher(input);
        if (!matcher.find()) {
            return OptionalDouble.empty();
        }

        return parseTimeSeconds(matcher.group().trim());
    }

    static OptionalDouble parseTimeSeconds(String timeToken) {
        if (timeToken == null || timeToken.isBlank()) {
            return OptionalDouble.empty();
        }

        try {
            if (timeToken.contains(":")) {
                String[] hms = timeToken.trim().split(":");
                if (hms.length != 3) {
                    return OptionalDouble.empty();
                }
                double seconds = Integer.parseInt(hms[0]) * 3600
                        + Integer.parseInt(hms[1]) * 60
                        + Double.parseDouble(hms[2]);
                return OptionalDouble.of(seconds);
            }

            return OptionalDouble.of(Double.parseDouble(timeToken.trim()));
        }
        catch (NumberFormatException ignored) {
            return OptionalDouble.empty();
        }
    }

    static OptionalDouble parseProgressTimeSeconds(String input) {
        Matcher matcher = PATTERN_TIME.matcher(input);
        if (!matcher.find()) {
            return OptionalDouble.empty();
        }

        return parseTimeSeconds(matcher.group().trim());
    }

    static OptionalLong parseSizeBytes(String input) {
        Matcher matcher = PATTERN_SIZE.matcher(input);
        if (!matcher.find()) {
            return OptionalLong.empty();
        }

        String sizeToken = matcher.group().trim();
        int unitStart = 0;
        while (unitStart < sizeToken.length()) {
            char c = sizeToken.charAt(unitStart);
            if (!(Character.isDigit(c) || c == '.')) {
                break;
            }
            unitStart++;
        }

        if (unitStart == 0 || unitStart >= sizeToken.length()) {
            return OptionalLong.empty();
        }

        try {
            double value = Double.parseDouble(sizeToken.substring(0, unitStart));
            String unit = sizeToken.substring(unitStart).trim().toUpperCase(Locale.ROOT);
            long multiplier = switch (unit) {
                case "B" -> 1L;
                case "KB" -> 1_000L;
                case "KIB" -> 1_024L;
                case "MB" -> 1_000_000L;
                case "MIB" -> 1_048_576L;
                case "GB" -> 1_000_000_000L;
                case "GIB" -> 1_073_741_824L;
                default -> -1L;
            };
            if (multiplier < 0) {
                return OptionalLong.empty();
            }

            return OptionalLong.of(Math.round(value * multiplier));
        }
        catch (NumberFormatException ignored) {
            return OptionalLong.empty();
        }
    }

    private void logOutput(String s, boolean isArray) {
        logger.info("=====================");
        if (isArray) {
            logger.info("Starte Array: ");
        }
        else {
            logger.info("Starte nicht als Array:");
        }
        logger.info(" -> {}", s);
        logger.info("=====================");
    }

    public Process exec(boolean log) {
        Process process = null;

        try {
            if (arrProgCallArray != null) {
                if (log) {
                    logOutput(strProgCallArray, true);
                }
                process = new ProcessBuilder(arrProgCallArray).start();
            }
            else {
                if (log) {
                    logOutput(strProgCall, false);
                }
                process = new ProcessBuilder(ProcessCommandUtils.tokenizeCommand(strProgCall)).start();
            }

            startStreamConsumer(process, IoType.INPUT);
            startStreamConsumer(process, IoType.ERROR);
        }
        catch (Exception ex) {
            logger.error("Fehler beim Starten", ex);
        }
        return process;
    }

    private void startStreamConsumer(Process process, IoType ioType) {
        Thread.ofVirtual()
                .name(String.format("RuntimeExec ffmpeg stream consumer type %s for pid %d", ioType, process.pid()))
                .start(() -> consumeStream(process, ioType));
    }

    private void consumeStream(Process process, IoType ioType) {
        var streamContext = createStreamContext(process, ioType);

        try (var reader = new BufferedReader(new InputStreamReader(streamContext.stream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (streamContext.parseProgress()) {
                    progressTracker.acceptErrorLine(line);
                }
                if (Config.isEnhancedLoggingEnabled()) {
                    logger.trace("  >> {}}: {}}", streamContext.title(), line);
                }
            }
        }
        catch (IOException ex) {
            logger.error("Error while consuming {} for pid {}", ioType, process.pid(), ex);
        }
    }

    private StreamContext createStreamContext(Process process, IoType ioType) {
        return switch (ioType) {
            case INPUT -> new StreamContext("INPUTSTREAM", process.getInputStream(), false);
            case ERROR ->
                    new StreamContext(String.format("ERRORSTREAM [%d]", process.pid()), process.getErrorStream(), true);
        };
    }

    private boolean canTrackProgress() {
        return start != null && start.startTime != null && mVFilmSize != null;
    }

    private long secondsSinceStart() {
        return Duration.between(start.startTime, LocalDateTime.now()).toSeconds();
    }

    private enum IoType {INPUT, ERROR}

    private record StreamContext(String title, InputStream stream, boolean parseProgress) {
    }

    private final class ProgressTracker {
        private double totalSecs;
        private long oldSizeBytes;
        private long oldBandwidthSampleSecs;
        private int percent = -1;
        private int percentStart = -1;

        void acceptErrorLine(String input) {
            if (!canTrackProgress()) {
                return;
            }

            // ffmpeg progress and diagnostics are emitted on stderr.
            try {
                parseDurationSeconds(input).ifPresent(duration -> totalSecs = duration);
                parseSizeBytes(input).ifPresent(this::updateTransferredBytes);
                parseProgressTimeSeconds(input).ifPresent(this::updateProgressSeconds);
            }
            catch (RuntimeException ex) {
                DownloadProgressEventPublisher.publishThrottled();
                logger.error("Failed to parse ffmpeg output line: {}", input, ex);
            }
        }

        private void updateTransferredBytes(long sizeBytes) {
            mVFilmSize.setAktSize(sizeBytes);

            var elapsedSecs = secondsSinceStart();
            if (oldBandwidthSampleSecs < elapsedSecs - 5) {
                start.bandbreite = (sizeBytes - oldSizeBytes) / (elapsedSecs - oldBandwidthSampleSecs);
                oldBandwidthSampleSecs = elapsedSecs;
                oldSizeBytes = sizeBytes;
            }
        }

        private void updateProgressSeconds(double progressSeconds) {
            if (totalSecs <= 0) {
                return;
            }

            updatePercent(progressSeconds / totalSecs * 100);
        }

        private void updatePercent(double percentValue) {
            // nur ganze Int speichern, und 1000 Schritte
            int newPercent = (int) (percentValue * 10);
            start.percent = newPercent;
            if (newPercent == percent) {
                return;
            }

            percent = newPercent;
            if (percentStart == -1) {
                // für wiedergestartete Downloads
                percentStart = percent;
            }
            if (percent > (percentStart + 5)) {
                // sonst macht es noch keinen Sinn
                long elapsedSecs = secondsSinceStart();
                int progressed = percent - percentStart;
                int remaining = 1000 - percent;
                start.restSekunden = elapsedSecs * remaining / progressed;
            }
            DownloadProgressEventPublisher.publishThrottled();
        }
    }
}
