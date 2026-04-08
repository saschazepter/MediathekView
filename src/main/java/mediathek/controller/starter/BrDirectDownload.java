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

package mediathek.controller.starter;

import mediathek.config.Daten;
import mediathek.config.Konstanten;
import mediathek.controller.ByteRateLimiter;
import mediathek.controller.MVBandwidthCountingInputStream;
import mediathek.controller.ThrottlingInputStream;
import mediathek.controller.history.SeenHistoryController;
import mediathek.daten.DatenDownload;
import mediathek.gui.dialog.DialogContinueDownload;
import mediathek.gui.dialog.MeldungDownloadfehler;
import mediathek.gui.messages.*;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.*;
import mediathek.tool.http.MVHttpClient;
import mediathek.tool.subtitles.MVSubtitle;
import net.engio.mbassy.bus.MBassador;
import net.engio.mbassy.listener.Handler;
import okhttp3.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class BrDirectDownload extends Thread {

    private static final int HTTP_RANGE_NOT_SATISFIABLE = 416;
    private static final int HTTP_PARTIAL_CONTENT = 206;
    private static final long RETRY_DELAY_MILLIS = 1_000L;
    private static final int BR_MAX_CONCURRENT_REQUESTS = 1;
    private static final int BR_MAX_CONCURRENT_REQUESTS_PER_HOST = 1;
    private static final int BR_MAX_CHUNK_RETRIES = 25;
    private static final long BR_DOWNLOAD_CHUNK_SIZE = 16L * 1024L * 1024L;
    private static final int DOWNLOAD_BUFFER_SIZE = 256 * 1024;
    private static final Logger logger = LogManager.getLogger(BrDirectDownload.class);
    private static final Dispatcher brHttp11Dispatcher = createBrHttp11Dispatcher();
    private final DatenDownload datenDownload;
    private final Start start;
    private final ByteRateLimiter rateLimiter;
    private final MBassador<BaseEvent> messageBus;
    private final OkHttpClient http11Client;
    private HttpDownloadState state = HttpDownloadState.DOWNLOAD;
    private long alreadyDownloaded;
    private File file;
    private boolean retAbbrechen;
    private boolean dialogAbbrechenIsVis;
    private CompletableFuture<Void> infoFuture;
    private CompletableFuture<Void> subtitleFuture;

    public BrDirectDownload(DatenDownload d) {
        super();

        http11Client = MVHttpClient.getInstance().getHttpClient().newBuilder()
                .protocols(List.of(Protocol.HTTP_1_1))
                .dispatcher(brHttp11Dispatcher)
                .build();
        rateLimiter = new ByteRateLimiter(getDownloadLimit());
        messageBus = MessageBus.getMessageBus();
        messageBus.subscribe(this);

        datenDownload = d;
        start = datenDownload.start;
        setName("BR DIRECT DL THREAD_" + d.arr[DatenDownload.DOWNLOAD_TITEL]);

        start.status = Start.STATUS_RUN;
        StarterClass.notifyStartEvent(datenDownload);
    }

    private static Dispatcher createBrHttp11Dispatcher() {
        Dispatcher dispatcher = new Dispatcher();
        dispatcher.setMaxRequests(BR_MAX_CONCURRENT_REQUESTS);
        dispatcher.setMaxRequestsPerHost(BR_MAX_CONCURRENT_REQUESTS_PER_HOST);
        return dispatcher;
    }

    @Handler
    private void handleRateLimitChanged(@NotNull DownloadRateLimitChangedEvent evt) {
        long limit = calcLimit(evt.newLimit, evt.active);
        logger.info("thread changing download speed limit to {} KB", limit);
        rateLimiter.setRate(limit);
    }

    private long calcLimit(long limit, boolean active) {
        if (limit <= 0 || !active) {
            return Long.MAX_VALUE;
        }
        return limit * FileUtils.ONE_KB;
    }

    private long calculateDownloadLimit(long limit) {
        var active = ApplicationConfiguration.getConfiguration().getBoolean(ApplicationConfiguration.DownloadRateLimiter.ACTIVE, false);
        return calcLimit(limit, active);
    }

    private long getDownloadLimit() {
        final long downloadLimit = ApplicationConfiguration.getConfiguration().getLong(ApplicationConfiguration.DownloadRateLimiter.LIMIT, 0);
        return calculateDownloadLimit(downloadLimit);
    }

    private long getContentLength(@NotNull HttpUrl url) throws IOException {
        final Request request = new Request.Builder().url(url).head()
                .header("User-Agent", getUserAgent())
                .header("Connection", "close")
                .build();

        try (Response response = http11Client.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                return -1;
            }

            long contentSize = FileSize.getContentLength(response);
            if (contentSize < 300_000) {
                return -1;
            }
            return contentSize;
        }
    }

    private String getUserAgent() {
        return ApplicationConfiguration.getConfiguration().getString(ApplicationConfiguration.APPLICATION_USER_AGENT);
    }

    private void startInfoFileDownload() {
        final boolean downloadInfoFile = Boolean.parseBoolean(datenDownload.arr[DatenDownload.DOWNLOAD_INFODATEI]);
        if (downloadInfoFile) {
            infoFuture = CompletableFuture.runAsync(() -> {
                try {
                    MVInfoFile infoFile = new MVInfoFile();
                    infoFile.writeInfoFile(datenDownload);
                }
                catch (IOException ex) {
                    logger.error("Failed to write info file", ex);
                }
            });
        }
    }

    private void downloadSubtitleFile() {
        if (Boolean.parseBoolean(datenDownload.arr[DatenDownload.DOWNLOAD_SUBTITLE])) {
            subtitleFuture = CompletableFuture.runAsync(() -> {
                MVSubtitle subtitleFile = new MVSubtitle();
                subtitleFile.writeSubtitle(datenDownload);
            });
        }
    }

    private void prepareDownloadContent() {
        startInfoFileDownload();
        downloadSubtitleFile();
        datenDownload.interruptRestart();
        datenDownload.mVFilmSize.setAktSize(alreadyDownloaded);
    }

    private void transferContent(InputStream inputStream) throws IOException {
        OutputStream fileSink;
        if (alreadyDownloaded != 0) {
            fileSink = Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.APPEND);
        } else {
            fileSink = Files.newOutputStream(file.toPath(), StandardOpenOption.CREATE, StandardOpenOption.WRITE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        try (fileSink;
             var bufferedSink = new BufferedOutputStream(fileSink, DOWNLOAD_BUFFER_SIZE);
             var tis = new ThrottlingInputStream(inputStream, rateLimiter);
             var mvis = new MVBandwidthCountingInputStream(tis)) {
            start.mVBandwidthCountingInputStream = mvis;
            final byte[] buffer = new byte[DOWNLOAD_BUFFER_SIZE];
            long p, pp = 0, startProzent = -1;
            int len;
            long aktBandwidth;
            long aktSize = 0;
            boolean melden = false;

            while (!start.stoppen) {
                len = start.mVBandwidthCountingInputStream.read(buffer);
                if (len == -1) {
                    break;
                }
                alreadyDownloaded += len;
                bufferedSink.write(buffer, 0, len);
                datenDownload.mVFilmSize.addAktSize(len);

                if (aktSize != datenDownload.mVFilmSize.getAktSize()) {
                    aktSize = datenDownload.mVFilmSize.getAktSize();
                    melden = true;
                }
                if (datenDownload.mVFilmSize.getSize() > 0) {
                    p = (aktSize * (long) 1000) / datenDownload.mVFilmSize.getSize();
                    if (startProzent == -1) {
                        startProzent = p;
                    }
                    if (p == 0) {
                        p = Start.PROGRESS_GESTARTET;
                    }
                    else if (p >= 1000) {
                        p = 999;
                    }
                    start.percent = (int) p;
                    if (p != pp) {
                        pp = p;
                        if (p > 2 && p > startProzent) {
                            final var diffZeit = Duration.between(start.startTime, LocalDateTime.now()).toSeconds();
                            final long restProzent = 1000L - p;
                            start.restSekunden = diffZeit * restProzent / (p - startProzent);
                        }
                        melden = true;
                    }
                }
                aktBandwidth = start.mVBandwidthCountingInputStream.getBandwidth();
                if (aktBandwidth != start.bandbreite) {
                    start.bandbreite = aktBandwidth;
                    melden = true;
                }
                if (melden) {
                    DownloadProgressEventPublisher.publishThrottled();
                    melden = false;
                }
            }
            bufferedSink.flush();
        }

        start.bandbreite = start.mVBandwidthCountingInputStream.getSumBandwidth();
    }

    private void finishSuccessfulDownload() {
        if (!start.stoppen) {
            if (datenDownload.quelle == DatenDownload.QUELLE_BUTTON) {
                start.status = Start.STATUS_FERTIG;
            }
            else if (StarterClass.pruefen(Daten.getInstance(), datenDownload, start)) {
                start.status = Start.STATUS_FERTIG;
            }
            else {
                start.status = Start.STATUS_ERR;
            }
        }
    }

    private void printHttpErrorMessage(Response response) {
        final String responseCode = "Responsecode: " + response.code() + '\n' + response.message();
        logger.error("HTTP-Fehler: {} {}", response.code(), response.message());

        if (!(start.countRestarted < Konstanten.MAX_DOWNLOAD_RESTARTS)) {
            SwingUtilities.invokeLater(() -> new MeldungDownloadfehler(MediathekGui.ui(), "URL des Films:\n"
                    + datenDownload.arr[DatenDownload.DOWNLOAD_URL] + "\n\n"
                    + responseCode + '\n', datenDownload).setVisible(true));
        }

        state = HttpDownloadState.ERROR;
        start.status = Start.STATUS_ERR;
    }

    private Request buildChunkRequest(@NotNull HttpUrl url, long rangeStart, long rangeEnd) {
        return new Request.Builder().url(url).get()
                .header("User-Agent", getUserAgent())
                .header("Connection", "close")
                .header("Range", "bytes=" + rangeStart + '-' + rangeEnd)
                .build();
    }

    private void executeChunkedDownloadRequest(@NotNull HttpUrl url, long totalSize) throws IOException {
        int retryCount = 0;
        while (!start.stoppen && alreadyDownloaded < totalSize) {
            final long chunkStart = alreadyDownloaded;
            final long chunkEnd = Math.min(chunkStart + BR_DOWNLOAD_CHUNK_SIZE - 1, totalSize - 1);
            final Request request = buildChunkRequest(url, chunkStart, chunkEnd);

            try (Response response = http11Client.newCall(request).execute()) {
                final ResponseBody body = response.body();
                if (response.code() == HTTP_PARTIAL_CONTENT) {
                    transferContent(body.byteStream());
                    retryCount = 0;
                    continue;
                }

                if (response.code() == HTTP_RANGE_NOT_SATISFIABLE && alreadyDownloaded >= totalSize) {
                    break;
                }

                if (response.code() == HttpURLConnection.HTTP_NOT_FOUND) {
                    logger.error("HTTP error 404 received for URL: {}", request.url());
                    state = HttpDownloadState.ERROR;
                    start.status = Start.STATUS_ERR;
                }
                else {
                    printHttpErrorMessage(response);
                }
                return;
            }
            catch (IOException ex) {
                if (isRetryableStreamException(ex) && alreadyDownloaded > chunkStart) {
                    logger.warn("Transient BR chunk error after partial progress, resuming at byte {}",
                            alreadyDownloaded, ex);
                    retryCount = 0;
                    waitForRetry(0, ex);
                    continue;
                }

                if (isRetryableStreamException(ex) && retryCount < BR_MAX_CHUNK_RETRIES) {
                    retryCount++;
                    waitForRetry(retryCount, ex);
                    continue;
                }
                throw ex;
            }
        }

        if (alreadyDownloaded >= totalSize) {
            finishSuccessfulDownload();
        }
    }

    private boolean isHttp2InternalStreamReset(@NotNull IOException ex) {
        Throwable current = ex;
        while (current != null) {
            if ("okhttp3.internal.http2.StreamResetException".equals(current.getClass().getName())) {
                final String msg = String.valueOf(current.getMessage());
                if (msg.contains("INTERNAL_ERROR")) {
                    return true;
                }
            }

            final String msg = current.getMessage();
            if (msg != null) {
                final String lower = msg.toLowerCase(Locale.ROOT);
                if (lower.contains("stream was reset") && lower.contains("internal_error")) {
                    return true;
                }
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isRetryableStreamException(@NotNull IOException ex) {
        if (isHttp2InternalStreamReset(ex)) {
            return true;
        }

        final String msg = ex.getMessage();
        if (msg == null) {
            return false;
        }

        final String lower = msg.toLowerCase(Locale.ROOT);
        return lower.contains("stream was reset")
                || lower.contains("unexpected end of stream")
                || lower.contains("connection reset")
                || lower.contains("broken pipe")
                || lower.contains("remote host terminated handshake");
    }

    private void waitForRetry(int retryCount, @NotNull IOException ex) {
        logger.warn("Transient BR chunk error (retry {}/{}), resuming at byte {}",
                retryCount, BR_MAX_CHUNK_RETRIES, alreadyDownloaded, ex);
        try {
            Thread.sleep(RETRY_DELAY_MILLIS);
        }
        catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
        }
    }

    @Override
    public synchronized void run() {
        StarterClass.startmeldung(datenDownload, start);

        messageBus.publishAsync(new DownloadStartEvent());

        try {
            createDirectory();
            file = new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME]);

            if (!cancelDownload()) {
                HttpUrl url = HttpUrl.parse(datenDownload.arr[DatenDownload.DOWNLOAD_URL]);
                assert url != null;
                logger.info("Using dedicated BR HTTP/1.1 chunked downloader for sender {}", datenDownload.arr[DatenDownload.DOWNLOAD_SENDER]);
                final long contentLength = getContentLength(url);
                datenDownload.mVFilmSize.setSize(contentLength);
                prepareDownloadContent();

                if (contentLength > 0) {
                    executeChunkedDownloadRequest(url, contentLength);
                }
                else {
                    throw new IOException("Could not determine content length for BR download");
                }
            }
        }
        catch (IOException ex) {
            logger.error("run()", ex);
            start.status = Start.STATUS_ERR;
            state = HttpDownloadState.ERROR;

            removeSeenHistoryEntry();

            SwingUtilities.invokeLater(() -> new MeldungDownloadfehler(MediathekGui.ui(), ex.getLocalizedMessage(), datenDownload).setVisible(true));
        }

        waitForPendingDownloads();

        StarterClass.finalizeDownload(datenDownload, start, state);

        messageBus.publishAsync(new DownloadFinishedEvent());
        messageBus.unsubscribe(this);
    }

    private void removeSeenHistoryEntry() {
        if (datenDownload.film != null) {
            logger.trace("Removing failed download entry from history");
            try (var historyController = new SeenHistoryController()) {
                historyController.markUnseen(datenDownload.film);
            }
        }
    }

    private void waitForPendingDownloads() {
        try {
            if (infoFuture != null) {
                infoFuture.get();
            }
            if (subtitleFuture != null) {
                subtitleFuture.get();
            }
        }
        catch (InterruptedException | ExecutionException e) {
            logger.error("waitForPendingDownloads().", e);
        }
    }

    private boolean cancelDownload() {
        if (!file.exists()) {
            return false;
        }

        dialogAbbrechenIsVis = true;
        retAbbrechen = true;
        if (SwingUtilities.isEventDispatchThread()) {
            retAbbrechen = abbrechen_();
        }
        else {
            SwingUtilities.invokeLater(() -> {
                retAbbrechen = abbrechen_();
                dialogAbbrechenIsVis = false;
            });
        }
        while (dialogAbbrechenIsVis) {
            try {
                wait(100);
            }
            catch (Exception ignored) {
            }
        }
        return retAbbrechen;
    }

    private void createDirectory() {
        try {
            Files.createDirectories(Paths.get(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD]));
        }
        catch (IOException ignored) {
        }
    }

    private boolean abbrechen_() {
        boolean result = false;
        if (file.exists()) {
            DialogContinueDownload dialogContinueDownload = new DialogContinueDownload(MediathekGui.ui(), datenDownload, true);
            dialogContinueDownload.setVisible(true);

            switch (dialogContinueDownload.getResult()) {
                case CANCELLED:
                    state = HttpDownloadState.CANCEL;
                    result = true;
                    break;

                case CONTINUE:
                    alreadyDownloaded = file.length();
                    break;

                case RESTART_WITH_NEW_NAME:
                    if (dialogContinueDownload.isNewName()) {
                        MessageBus.getMessageBus().publishAsync(new DownloadListChangedEvent());
                        createDirectory();
                        file = new File(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME]);
                    }
                    break;
            }
        }
        return result;
    }
}
