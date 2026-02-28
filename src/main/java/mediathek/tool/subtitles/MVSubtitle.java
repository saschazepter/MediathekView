/*
 * Copyright (c) 2026 derreisende77.
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

package mediathek.tool.subtitles;

import mediathek.daten.DatenDownload;
import mediathek.tool.FileUtils;
import mediathek.tool.PathExtensions;
import mediathek.tool.subtitles.detector.TimedTextFormatDetector;
import mediathek.tool.subtitles.ttml2.AssExporter;
import mediathek.tool.subtitles.ttml2.SubRipHtmlExporter;
import mediathek.tool.subtitles.ttml2.Ttml2Parser;
import mediathek.tool.subtitles.vtt.WebVttToTtml2Converter;
import org.apache.logging.log4j.LogManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

public class MVSubtitle {
    public static Path addFileExtension(@NotNull Path selectedFilePath, @NotNull TimedTextFormatDetector.Format format) throws IOException {
        Path path;
        switch (format) {
            case WEBVTT -> {
                path = PathExtensions.withExtension(selectedFilePath, ".vtt");
                Files.move(selectedFilePath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            case TTML1, TTML2 -> {
                path = PathExtensions.withExtension(selectedFilePath, ".ttml");
                Files.move(selectedFilePath, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            }
            default -> throw new IOException("Unknown subtitle format: " + format);
        }
        return path;
    }

    private void downloadAndConvertSubtitleFile(@NotNull String subtitleUrl, @NotNull Path selectedFilePath) throws Exception {
        Path tempSubtitleFile = null;
        try {
            tempSubtitleFile = FileUtils.downloadToTempFile(subtitleUrl);
            Files.move(tempSubtitleFile, selectedFilePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            var res = TimedTextFormatDetector.detect(selectedFilePath, true);
            if (!res.valid()) {
                throw new IOException("Invalid subtitle format: " + res.format());
            }
            else {
                //valid result
                if (res.format() == TimedTextFormatDetector.Format.UNKNOWN) {
                    throw new IOException("Unknown subtitle format: " + res.format());
                }

                selectedFilePath = addFileExtension(selectedFilePath, res.format());

                if (res.format() == TimedTextFormatDetector.Format.WEBVTT) {
                    //convert WebVTT to TTML2
                    WebVttToTtml2Converter converter = new WebVttToTtml2Converter();
                    var newPath = PathExtensions.withExtension(selectedFilePath, ".ttml");
                    converter.convert(selectedFilePath, newPath);
                    selectedFilePath = newPath;
                }

                Ttml2Parser parser = new Ttml2Parser();
                var ttmlDoc = parser.parse(selectedFilePath);

                //SRT conversion
                var srtPath = PathExtensions.withExtension(selectedFilePath, ".srt");
                var srtStr = new SubRipHtmlExporter().export(ttmlDoc);
                Files.writeString(srtPath, srtStr);

                //ASS conversion
                var assPath = PathExtensions.withExtension(selectedFilePath, ".ass");
                var assOptions = new AssExporter.Options(384, 288, false);
                var assStr = new AssExporter(assOptions).export(ttmlDoc);
                Files.writeString(assPath, assStr);
            }
        }
        finally {
            try {
                if (tempSubtitleFile != null)
                    Files.deleteIfExists(tempSubtitleFile);
            }
            catch (IOException ignored) {
            }
        }
    }

    public void writeSubtitle(@NotNull DatenDownload datenDownload) {
        final String urlSubtitle = datenDownload.arr[DatenDownload.DOWNLOAD_URL_SUBTITLE];
        if (urlSubtitle.isEmpty())
            return;

        Path destinationPath = Paths.get(datenDownload.getFileNameWithoutSuffix());
        try {
            downloadAndConvertSubtitleFile(urlSubtitle, destinationPath);
        }
        catch (Exception e) {
            LogManager.getLogger().error("Error writing subtitle: {}", e.getMessage());
        }
    }
}
