package mediathek.gui.dialog;

import com.github.kokorin.jaffree.StreamType;
import com.github.kokorin.jaffree.ffprobe.FFprobe;
import com.github.kokorin.jaffree.ffprobe.FFprobeResult;
import com.github.kokorin.jaffree.ffprobe.Stream;
import com.github.kokorin.jaffree.process.JaffreeAbnormalExitException;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.ListenableFuture;
import com.jidesoft.swing.MultilineLabel;
import mediathek.config.Daten;
import mediathek.config.Konstanten;
import mediathek.config.MVColor;
import mediathek.config.MVConfig;
import mediathek.daten.*;
import mediathek.gui.messages.DownloadListChangedEvent;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.*;
import net.miginfocom.layout.AC;
import net.miginfocom.layout.CC;
import net.miginfocom.layout.LC;
import net.miginfocom.swing.MigLayout;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.sync.LockMode;
import org.apache.commons.lang3.SystemUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdesktop.swingx.JXBusyLabel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class DialogAddDownload extends JDialog {
    private static final Logger logger = LogManager.getLogger();
    private static final String NO_DATA_AVAILABLE = "Keine Daten verfügbar.";
    private static final String KEY_LABEL_FOREGROUND = "Label.foreground";
    private static final String KEY_TEXTFIELD_BACKGROUND = "TextField.background";
    private static final String TITLED_BORDER_STRING = "Download-Qualität";
    private final DatenFilm film;
    private final Optional<FilmResolution.Enum> requestedResolution;
    private final ListePset listeSpeichern = Daten.listePset.getListeSpeichern();
    /**
     * The currently selected pSet or null when no selection.
     */
    private DatenPset active_pSet;
    private DatenDownload datenDownload;
    private String orgPfad = "";
    private String dateiGroesse_HQ = "";
    private String dateiGroesse_Hoch = "";
    private String dateiGroesse_Klein = "";
    private boolean nameGeaendert;
    private boolean stopBeob;
    private JTextComponent cbPathTextComponent;
    private Path ffprobePath;
    private ListenableFuture<FFprobeResult> resultListenableFuture;
    private ListenableFuture<String> hqFuture;
    private ListenableFuture<String> hochFuture;
    private ListenableFuture<String> kleinFuture;
    private boolean restoreFetchSize;
    private boolean highQualityMandated;

    public DialogAddDownload(@NotNull Frame parent, @NotNull DatenFilm film, @Nullable DatenPset pSet, @NotNull Optional<FilmResolution.Enum> requestedResolution) {
        super(parent, true);
        initComponents();

        getRootPane().setDefaultButton(jButtonOk);
        EscapeKeyHandler.installHandler(this, this::dispose);

        this.requestedResolution = requestedResolution;
        this.film = film;
        this.active_pSet = pSet;

        setupUI();

        restoreWindowSizeFromConfig();        //only install on windows and linux, macOS works...
        installMinResizePreventer();

        setLocationRelativeTo(parent);

        addComponentListener(new DialogPositionComponentListener());

        SwingUtilities.invokeLater(() -> jButtonOk.requestFocus());
    }

    /// Prevents that a dialog can be resized smaller than its minimum dimensions.
    /// Needed on Windows, but not macOS and Linux.
    private void installMinResizePreventer() {
        if (!SystemUtils.IS_OS_WINDOWS)
            return;

        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                Dimension min = getMinimumSize();
                Dimension size = getSize();
                int w = Math.max(size.width, min.width);
                int h = Math.max(size.height, min.height);
                if (w != size.width || h != size.height) {
                    setSize(w, h);
                }
            }
        });
    }

    public static void setModelPfad(String pfad, JComboBox<String> jcb) {
        ArrayList<String> pfade = new ArrayList<>();
        final boolean showLastUsedPath = ApplicationConfiguration.getConfiguration().getBoolean(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, true);

        // wenn gewünscht, den letzten verwendeten Pfad an den Anfang setzen
        if (!showLastUsedPath && !pfad.isEmpty()) {
            // aktueller Pfad an Platz 1
            pfade.add(pfad);

        }
        if (!MVConfig.get(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN).isEmpty()) {
            String[] p = MVConfig.get(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN).split("<>");
            for (String s : p) {
                if (!pfade.contains(s)) {
                    pfade.add(s);
                }
            }
        }
        if (showLastUsedPath && !pfad.isEmpty()) {
            // aktueller Pfad zum Schluss
            if (!pfade.contains(pfad)) {
                pfade.add(pfad);
            }
        }
        jcb.setModel(new DefaultComboBoxModel<>(pfade.toArray(new String[0])));
    }

    public static void saveComboPfad(JComboBox<String> jcb, String orgPath) {
        ArrayList<String> pfade = new ArrayList<>();
        String s = Objects.requireNonNull(jcb.getSelectedItem()).toString();

        if (!s.equals(orgPath) || ApplicationConfiguration.getConfiguration().getBoolean(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, true)) {
            pfade.add(s);
        }
        for (int i = 0; i < jcb.getItemCount(); ++i) {
            s = jcb.getItemAt(i);
            if (!s.equals(orgPath) && !pfade.contains(s)) {
                pfade.add(s);
            }
        }
        if (!pfade.isEmpty()) {
            s = pfade.stream()
                    .filter(pfad -> !pfad.isEmpty())
                    .limit(Konstanten.MAX_PFADE_DIALOG_DOWNLOAD)
                    .collect(Collectors.joining("<>"));
        }
        MVConfig.add(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN, s);
    }

    private void restoreWindowSizeFromConfig() {
        var config = ApplicationConfiguration.getConfiguration();
        try {
            config.lock(LockMode.READ);
            var dims = getMinimumSize();
            int MINIMUM_WIDTH = dims.width;
            int MINIMUM_HEIGHT = dims.height;
            int width = Math.max(config.getInt(ApplicationConfiguration.AddDownloadDialog.WIDTH), MINIMUM_WIDTH);
            int height = Math.max(config.getInt(ApplicationConfiguration.AddDownloadDialog.HEIGHT), MINIMUM_HEIGHT);
            int x = config.getInt(ApplicationConfiguration.AddDownloadDialog.X);
            int y = config.getInt(ApplicationConfiguration.AddDownloadDialog.Y);

            setBounds(x, y, width, height);
        }
        catch (NoSuchElementException ignored) {
            //do not restore anything
        }
        finally {
            config.unlock(LockMode.READ);
        }

    }

    private void setupFilmQualityRadioButtons() {
        var listener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                setNameFilm();
                lblStatus.setText("");
                lblAudioInfo.setText("");
                lblBusyIndicator.setBusy(false);
                lblBusyIndicator.setVisible(false);
                if (resultListenableFuture != null) {
                    resultListenableFuture.cancel(true);
                    resultListenableFuture = null;
                }
            }
        };
        jRadioButtonAufloesungHd.addActionListener(listener);
        jRadioButtonAufloesungHd.setEnabled(!film.getHighQualityUrl().isEmpty());

        jRadioButtonAufloesungKlein.addActionListener(listener);
        jRadioButtonAufloesungKlein.setEnabled(!film.getLowQualityUrl().isEmpty());

        jRadioButtonAufloesungHoch.addActionListener(listener);
        jRadioButtonAufloesungHoch.setSelected(true);

        btnRequestLiveInfo.addActionListener(_ -> handleRequestLiveFilmInfo());
    }

    /**
     * Return only the first part of the long codec name.
     *
     * @param stream The video stream from ffprobe.
     * @return First entry of long codec name.
     */
    private String getVideoCodecName(@NotNull Stream stream) {
        var name = stream.getCodecLongName();
        logger.trace("video codec long name: {}", name);
        try {
            var splitName = name.split("/");
            return splitName[0].trim();
        }
        catch (Exception e) {
            return name;
        }
    }

    private void handleRequestLiveFilmInfo() {
        var res = getFilmResolution();
        var url = film.getUrlFuerAufloesung(res);

        btnRequestLiveInfo.setEnabled(false);
        lblBusyIndicator.setVisible(true);
        lblBusyIndicator.setBusy(true);
        lblStatus.setText("");
        lblAudioInfo.setText("");

        Future<FFprobeResult> resultFuture = FFprobe.atPath(ffprobePath)
                .setShowStreams(true)
                .setInput(url)
                .executeAsync();
        resultListenableFuture = JdkFutureAdapters.listenInPoolThread(resultFuture);
        Futures.addCallback(resultListenableFuture, new FutureCallback<>() {
            private static final String ERR_MSG_PART = "Server returned ";
            private static final String MSG_UNKNOWN_ERROR = "Unbekannter Fehler aufgetreten.";

            @Override
            public void onSuccess(FFprobeResult result) {
                var audioStreamResult = result.getStreams().stream().filter(stream -> stream.getCodecType() == StreamType.AUDIO).findAny();
                audioStreamResult.ifPresentOrElse(astream -> {
                            var sample_rate = astream.getSampleRate();
                            final String audio_output = getAudioInfo(astream, sample_rate);
                            SwingUtilities.invokeLater(() -> {
                                lblAudioInfo.setForeground(UIManager.getColor(KEY_LABEL_FOREGROUND));
                                lblAudioInfo.setText(audio_output);
                            });
                        },
                        () -> SwingUtilities.invokeLater(() -> {
                            lblAudioInfo.setForeground(UIManager.getColor(KEY_LABEL_FOREGROUND));
                            lblAudioInfo.setText(NO_DATA_AVAILABLE);
                        }));

                var videoStreamResult = result.getStreams().stream().filter(stream -> stream.getCodecType() == StreamType.VIDEO).findAny();
                videoStreamResult.ifPresentOrElse(stream -> {
                    var frame_rate = stream.getAvgFrameRate().intValue();
                    var codecName = getVideoCodecName(stream);
                    final String video_output = getVideoInfoString(stream, frame_rate, codecName);
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setForeground(UIManager.getColor(KEY_LABEL_FOREGROUND));
                        lblStatus.setText(video_output);
                    });
                }, () -> SwingUtilities.invokeLater(() -> {
                    lblStatus.setForeground(UIManager.getColor(KEY_LABEL_FOREGROUND));
                    lblStatus.setText(NO_DATA_AVAILABLE);
                }));

                SwingUtilities.invokeLater(() -> resetBusyLabelAndButton());
            }

            private int safe_process_bit_rate(Integer in) {
                int bits;
                try {
                    bits = in / 1000;
                }
                catch (Exception e) {
                    bits = 0;
                }
                return bits;
            }

            private String getVideoInfoString(Stream stream, int frame_rate, String codecName) {
                int bit_rate = safe_process_bit_rate(stream.getBitRate());
                String video_output;
                if (bit_rate == 0) {
                    video_output = String.format("Video: %dx%d, %d fps (avg), %s", stream.getWidth(), stream.getHeight(), frame_rate, codecName);
                }
                else {
                    video_output = String.format("Video: %dx%d, %d kBit/s, %d fps (avg), %s", stream.getWidth(), stream.getHeight(), bit_rate, frame_rate, codecName);
                }
                return video_output;
            }

            private String getAudioInfo(Stream astream, Integer sample_rate) {
                int bits_per_sample = safe_process_bit_rate(astream.getBitRate());
                String audio_output;
                if (bits_per_sample == 0) {
                    audio_output = String.format("Audio: %d Hz, %s", sample_rate, astream.getCodecLongName());
                }
                else {
                    audio_output = String.format("Audio: %d Hz, %d kBit/s, %s", sample_rate, bits_per_sample, astream.getCodecLongName());
                }
                return audio_output;
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                // show nothing when task was cancelled...
                if (t instanceof CancellationException) {
                    SwingUtilities.invokeLater(() -> {
                        lblStatus.setText("");
                        lblAudioInfo.setText("");
                        resetBusyLabelAndButton();
                    });
                }
                else if (t instanceof JaffreeAbnormalExitException e) {
                    String final_str = getJaffreeErrorString(e);
                    setupLabels(final_str);
                }
                else {
                    setupLabels(MSG_UNKNOWN_ERROR);
                }
            }

            private void setupLabels(String text) {
                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText(text);
                    lblStatus.setForeground(Color.RED);
                    lblAudioInfo.setText("");
                    resetBusyLabelAndButton();
                });
            }

            private @NotNull String getJaffreeErrorString(JaffreeAbnormalExitException e) {
                String final_str;
                try {
                    var msg = e.getProcessErrorLogMessages().getFirst().message.split(":");
                    var err_msg = msg[msg.length - 1].trim();
                    if (err_msg.startsWith(ERR_MSG_PART)) {
                        final_str = err_msg.substring(ERR_MSG_PART.length());
                    }
                    else {
                        final_str = MSG_UNKNOWN_ERROR;
                    }
                }
                catch (Exception ignored) {
                    final_str = MSG_UNKNOWN_ERROR;
                }
                return final_str;
            }
        }, Daten.getInstance().getDecoratedPool());
    }

    private void resetBusyLabelAndButton() {
        lblBusyIndicator.setBusy(false);
        lblBusyIndicator.setVisible(false);
        btnRequestLiveInfo.setEnabled(true);
    }

    private void detectFfprobeExecutable() {
        try {
            ffprobePath = GuiFunktionenProgramme.findExecutableOnPath("ffprobe").getParent();
        }
        catch (Exception ex) {
            logger.error("ffprobe not found", ex);
            lblBusyIndicator.setText("Hilfsprogramm nicht gefunden!");
            lblBusyIndicator.setForeground(Color.RED);
            btnRequestLiveInfo.setEnabled(false);
        }
    }

    private void setupBusyIndicator() {
        lblBusyIndicator.setText("");
        lblBusyIndicator.setBusy(false);
        lblBusyIndicator.setVisible(false);
        lblStatus.setText("");
        lblAudioInfo.setText("");
    }

    private void setupUI() {
        setupBusyIndicator();
        detectFfprobeExecutable();

        // launch async tasks first
        launchResolutionFutures();

        jCheckBoxStarten.setSelected(Boolean.parseBoolean(MVConfig.get(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD_D_STARTEN)));
        jCheckBoxStarten.addActionListener(_ -> MVConfig.add(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD_D_STARTEN, String.valueOf(jCheckBoxStarten.isSelected())));

        setupZielButton();

        jButtonOk.addActionListener(_ -> {
            if (check()) {
                saveComboPfad(jComboBoxPfad, orgPfad);
                saveDownload();
            }
        });

        jButtonAbbrechen.addActionListener(_ -> dispose());

        setupPSetComboBox();
        setupSenderTextField();
        setupNameTextField();
        setupPathTextComponent();

        setupFilmQualityRadioButtons();

        setupDeleteHistoryButton();
        setupPfadSpeichernCheckBox();

        waitForFileSizeFutures();

        setupResolutionButtons();
        setupInfoFileCreationCheckBox();

        calculateAndCheckDiskSpace();
        nameGeaendert = false;
    }

    private void launchResolutionFutures() {
        // always fetch file size during dialog ops...
        restoreFetchSize = ApplicationConfiguration.getConfiguration().getBoolean(ApplicationConfiguration.DOWNLOAD_FETCH_FILE_SIZE, true);
        ApplicationConfiguration.getConfiguration().setProperty(ApplicationConfiguration.DOWNLOAD_FETCH_FILE_SIZE, true);

        var decoratedPool = Daten.getInstance().getDecoratedPool();
        hqFuture = decoratedPool.submit(() -> {
            var url = film.getUrlFuerAufloesung(FilmResolution.Enum.HIGH_QUALITY);
            return film.getFileSizeForUrl(url);
        });

        Futures.addCallback(hqFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(String result) {
                SwingUtilities.invokeLater(() -> {
                    if (jRadioButtonAufloesungHd.isEnabled()) {
                        dateiGroesse_HQ = result;
                        if (!dateiGroesse_HQ.isEmpty()) {
                            var text = jRadioButtonAufloesungHd.getText();
                            jRadioButtonAufloesungHd.setText(text + "   [ " + dateiGroesse_HQ + " MB ]");
                        }
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    dateiGroesse_HQ = "";
                    logger.error("Failed to retrieve HD resolution", t);
                });
            }
        }, decoratedPool);

        hochFuture = decoratedPool.submit(() -> {
            var url = film.getUrlNormalQuality();
            return film.getFileSizeForUrl(url);
        });
        Futures.addCallback(hochFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(String result) {
                SwingUtilities.invokeLater(() -> {
                    dateiGroesse_Hoch = result;
                    if (!dateiGroesse_Hoch.isEmpty()) {
                        var text = jRadioButtonAufloesungHoch.getText();
                        jRadioButtonAufloesungHoch.setText(text + "   [ " + dateiGroesse_Hoch + " MB ]");
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    dateiGroesse_Hoch = "";
                    logger.error("Failed to retrieve Hoch resolution", t);
                });
            }
        }, decoratedPool);

        kleinFuture = decoratedPool.submit(() -> {
            var url = film.getUrlFuerAufloesung(FilmResolution.Enum.LOW);
            return film.getFileSizeForUrl(url);
        });
        Futures.addCallback(kleinFuture, new FutureCallback<>() {
            @Override
            public void onSuccess(String result) {
                SwingUtilities.invokeLater(() -> {
                    if (jRadioButtonAufloesungKlein.isEnabled()) {
                        dateiGroesse_Klein = result;
                        if (!dateiGroesse_Klein.isEmpty()) {
                            var text = jRadioButtonAufloesungKlein.getText();
                            jRadioButtonAufloesungKlein.setText(text + "   [ " + dateiGroesse_Klein + " MB ]");
                        }
                    }
                });
            }

            @Override
            public void onFailure(@NotNull Throwable t) {
                SwingUtilities.invokeLater(() -> {
                    dateiGroesse_Klein = "";
                    logger.error("Failed to retrieve Klein resolution", t);
                });
            }
        }, decoratedPool);
    }

    private DefaultComboBoxModel<String> createPSetComboBoxModel() {
        return new DefaultComboBoxModel<>(listeSpeichern.getObjectDataCombo());
    }

    private void setupPSetComboBox() {
        // disable when only one entry...
        if (listeSpeichern.size() == 1) {
            jComboBoxPset.setEnabled(false);
        }

        var model = createPSetComboBoxModel();
        jComboBoxPset.setModel(model);

        if (active_pSet != null) {
            jComboBoxPset.setSelectedItem(active_pSet.getName());
        }
        else {
            active_pSet = listeSpeichern.get(jComboBoxPset.getSelectedIndex());
        }
        jComboBoxPset.addActionListener(_ -> setupResolutionButtons());
    }

    private void setupSenderTextField() {
        lblSender.setText(film.getSender());
        jTextFieldSender.setText(film.getTitle());
        jTextFieldSender.setBackground(UIManager.getColor("Label.background"));
    }

    private void setupNameTextField() {
        jTextFieldName.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                tus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                tus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                tus();
            }

            private void tus() {
                if (!stopBeob) {
                    nameGeaendert = true;
                    if (!jTextFieldName.getText().equals(FilenameUtils.checkDateiname(jTextFieldName.getText(), false /*pfad*/))) {
                        jTextFieldName.setBackground(MVColor.DOWNLOAD_FEHLER.color);
                    }
                    else {
                        jTextFieldName.setBackground(UIManager.getDefaults().getColor(KEY_TEXTFIELD_BACKGROUND));
                    }
                }

            }
        });
    }

    private void setupPathTextComponent() {
        cbPathTextComponent = ((JTextComponent) jComboBoxPfad.getEditor().getEditorComponent());
        cbPathTextComponent.setOpaque(true);
        cbPathTextComponent.getDocument().addDocumentListener(new DocumentListener() {

            @Override
            public void insertUpdate(DocumentEvent e) {
                tus();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                tus();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                tus();
            }

            private void tus() {
                if (!stopBeob) {
                    nameGeaendert = true;
                    //perform checks only when OS is not windows
                    if (!SystemUtils.IS_OS_WINDOWS) {
                        String s = cbPathTextComponent.getText();
                        final var editor = jComboBoxPfad.getEditor().getEditorComponent();
                        if (!s.equals(FilenameUtils.checkDateiname(s, true))) {
                            editor.setBackground(MVColor.DOWNLOAD_FEHLER.color);
                        }
                        else {
                            editor.setBackground(UIManager.getColor(KEY_TEXTFIELD_BACKGROUND));
                        }
                    }
                    calculateAndCheckDiskSpace();
                }

            }
        });
    }

    private void setupZielButton() {
        jButtonZiel.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/folder-open.svg"));
        jButtonZiel.setText("");
        jButtonZiel.addActionListener(_ -> {
            var initialDirectory = "";
            if (!Objects.requireNonNull(jComboBoxPfad.getSelectedItem()).toString().isEmpty()) {
                initialDirectory = jComboBoxPfad.getSelectedItem().toString();
            }
            var directory = FileDialogs.chooseDirectoryLocation(MediathekGui.ui(), "Film speichern", initialDirectory);
            if (directory != null) {
                var selectedDirectory = directory.getAbsolutePath();
                jComboBoxPfad.addItem(selectedDirectory);
                jComboBoxPfad.setSelectedItem(selectedDirectory);

            }
        });
    }

    private void setupDeleteHistoryButton() {
        jButtonDelHistory.setText("");
        jButtonDelHistory.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/trash-can.svg"));
        jButtonDelHistory.addActionListener(_ -> {
            MVConfig.add(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN, "");
            jComboBoxPfad.setModel(new DefaultComboBoxModel<>(new String[]{orgPfad}));
        });
    }

    private void waitForFileSizeFutures() {
        // for safety wait for all futures here...
        try {
            hqFuture.get();
            hochFuture.get();
            kleinFuture.get();
        }
        catch (InterruptedException | ExecutionException e) {
            logger.error("Error occured while waiting for file size futures", e);
        }
        finally {
            //reset fetch size state to previous value
            ApplicationConfiguration.getConfiguration().setProperty(ApplicationConfiguration.DOWNLOAD_FETCH_FILE_SIZE, restoreFetchSize);
        }
    }

    private void setupPfadSpeichernCheckBox() {
        final Configuration config = ApplicationConfiguration.getConfiguration();
        jCheckBoxPfadSpeichern.setSelected(config.getBoolean(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, true));
        jCheckBoxPfadSpeichern.addActionListener(_ ->
                config.setProperty(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, jCheckBoxPfadSpeichern.isSelected()));
    }

    private void setNameFilm() {
        // beim ersten mal werden die Standardpfade gesucht
        if (!nameGeaendert) {
            // nur wenn vom Benutzer noch nicht geändert!
            stopBeob = true;
            datenDownload = new DatenDownload(active_pSet, film, DatenDownload.QUELLE_DOWNLOAD, null, "", "", getFilmResolution().toString());
            if (datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_DATEINAME].isEmpty()) {
                // dann wird nicht gespeichert → eigentlich falsche Seteinstellungen?
                jTextFieldName.setEnabled(false);
                jComboBoxPfad.setEnabled(false);
                jButtonZiel.setEnabled(false);
                jTextFieldName.setText("");
                jComboBoxPfad.setModel(new DefaultComboBoxModel<>(new String[]{""}));
            }
            else {
                jTextFieldName.setEnabled(true);
                jComboBoxPfad.setEnabled(true);
                jButtonZiel.setEnabled(true);
                jTextFieldName.setText(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_DATEINAME]);
                setModelPfad(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD], jComboBoxPfad);
                orgPfad = datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD];
            }
            stopBeob = false;
        }
    }

    /**
     * Get the free disk space for a selected path.
     *
     * @return Free disk space in bytes.
     */
    private long getFreeDiskSpace(final String strPath) {
        long usableSpace = 0;
        if (!strPath.isEmpty()) {
            try {
                Path path = Paths.get(strPath);
                if (Files.notExists(path)) {
                    //getParent() may return null...therefore we need to bail out this loop at some point.
                    while (Files.notExists(path) && (path != null)) {
                        path = path.getParent();
                    }
                }

                if (path == null) {
                    //there is no way to determine usable space...
                    usableSpace = 0;
                }
                else {
                    final FileStore fileStore = Files.getFileStore(path);
                    usableSpace = fileStore.getUsableSpace();
                }
            }
            catch (Exception ex) {
                logger.error("getFreeDiskSpace Failed", ex);
            }
        }
        return usableSpace;
    }

    /**
     * Calculate free disk space on volume and check if the movies can be safely downloaded.
     */
    private void calculateAndCheckDiskSpace() {
        var fgColor = UIManager.getColor(KEY_LABEL_FOREGROUND);
        if (fgColor != null) {
            jRadioButtonAufloesungHd.setForeground(fgColor);
            jRadioButtonAufloesungHoch.setForeground(fgColor);
            jRadioButtonAufloesungKlein.setForeground(fgColor);
        }

        try {
            var filmBorder = (TitledBorder) jPanelSize.getBorder();
            long usableSpace = getFreeDiskSpace(cbPathTextComponent.getText());
            if (usableSpace > 0) {
                filmBorder.setTitle(TITLED_BORDER_STRING + " [ Freier Speicherplatz: " + FileUtils.humanReadableByteCountBinary(usableSpace) + " ]");
            }
            else {
                filmBorder.setTitle(TITLED_BORDER_STRING);
            }
            //border needs to be repainted after update...
            jPanelSize.repaint();

            // jetzt noch prüfen, obs auf die Platte passt
            usableSpace /= FileSize.ONE_MiB;
            if (usableSpace > 0) {
                int size;
                if (!dateiGroesse_HQ.isEmpty()) {
                    size = Integer.parseInt(dateiGroesse_HQ);
                    if (size > usableSpace) {
                        jRadioButtonAufloesungHd.setForeground(Color.red);
                    }
                }
                if (!dateiGroesse_Hoch.isEmpty()) {
                    size = Integer.parseInt(dateiGroesse_Hoch);
                    if (size > usableSpace) {
                        jRadioButtonAufloesungHoch.setForeground(Color.red);
                    }
                }
                if (!dateiGroesse_Klein.isEmpty()) {
                    size = Integer.parseInt(dateiGroesse_Klein);
                    if (size > usableSpace) {
                        jRadioButtonAufloesungKlein.setForeground(Color.red);
                    }
                }
            }
        }
        catch (Exception ex) {
            logger.error("calculateAndCheckDiskSpace()", ex);
        }
    }

    private boolean isHighQualityRequested() {
        return active_pSet.arr[DatenPset.PROGRAMMSET_AUFLOESUNG].equals(FilmResolution.Enum.HIGH_QUALITY.toString())
                && film.isHighQuality();
    }

    private boolean isLowQualityRequested() {
        return active_pSet.arr[DatenPset.PROGRAMMSET_AUFLOESUNG].equals(FilmResolution.Enum.LOW.toString()) &&
                !film.getLowQualityUrl().isEmpty();
    }

    /**
     * Setup the resolution radio buttons based on available download URLs.
     */
    private void setupResolutionButtons() {
        active_pSet = listeSpeichern.get(jComboBoxPset.getSelectedIndex());

        prepareResolutionButtons();

        prepareSubtitleCheckbox();
        setNameFilm();
    }

    private void setupInfoFileCreationCheckBox() {
        //disable for Livestreams as they do not contain useful data, even if pset wants it...
        final boolean isLivestream = film.isLivestream();
        jCheckBoxInfodatei.setEnabled(!isLivestream);
        if (!isLivestream) {
            jCheckBoxInfodatei.setSelected(active_pSet.shouldCreateInfofile());
        }
        else
            jCheckBoxInfodatei.setSelected(false);
    }

    private void prepareResolutionButtons() {
        requestedResolution.ifPresent(it -> highQualityMandated = it == FilmResolution.Enum.HIGH_QUALITY);
        if (highQualityMandated || isHighQualityRequested()) {
            jRadioButtonAufloesungHd.setSelected(true);
        }
        else if (isLowQualityRequested()) {
            jRadioButtonAufloesungKlein.setSelected(true);
        }
        else {
            jRadioButtonAufloesungHoch.setSelected(true);
        }
    }

    private void prepareSubtitleCheckbox() {
        if (!film.hasSubtitle()) {
            jCheckBoxSubtitle.setEnabled(false);
        }
        else {
            jCheckBoxSubtitle.setSelected(active_pSet.shouldDownloadSubtitle());
        }
    }

    /**
     * Return the resolution string based on selected {@link javax.swing.JRadioButton}.
     *
     * @return The resolution as a string.
     */
    private FilmResolution.Enum getFilmResolution() {
        if (jRadioButtonAufloesungHd.isSelected()) {
            return FilmResolution.Enum.HIGH_QUALITY;
        }
        else if (jRadioButtonAufloesungKlein.isSelected()) {
            return FilmResolution.Enum.LOW;
        }
        else {
            return FilmResolution.Enum.NORMAL;
        }
    }

    private String getFilmSize() {
        if (jRadioButtonAufloesungHd.isSelected()) {
            return dateiGroesse_HQ;
        }
        else if (jRadioButtonAufloesungKlein.isSelected()) {
            return dateiGroesse_Klein;
        }
        else {
            return dateiGroesse_Hoch;
        }
    }

    private boolean check() {
        var ok = false;
        String pfad = Objects.requireNonNull(jComboBoxPfad.getSelectedItem()).toString();
        String name = jTextFieldName.getText();
        if (datenDownload != null) {
            if (pfad.isEmpty() || name.isEmpty()) {
                MVMessageDialog.showMessageDialog(this, "Pfad oder Name ist leer", "Fehlerhafter Pfad/Name!", JOptionPane.ERROR_MESSAGE);
            }
            else {
                if (!pfad.substring(pfad.length() - 1).equals(File.separator)) {
                    pfad += File.separator;
                }
                if (GuiFunktionenProgramme.checkPathWriteable(pfad)) {
                    ok = true;
                }
                else {
                    MVMessageDialog.showMessageDialog(this, "Pfad ist nicht beschreibbar", "Fehlerhafter Pfad!", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
        return ok;
    }

    private void addDownloadToQueue() {
        Daten.getInstance().getListeDownloads().addMitNummer(datenDownload);
        MessageBus.getMessageBus().publishAsync(new DownloadListChangedEvent());

        if (jCheckBoxStarten.isSelected()) {
            datenDownload.startDownload();
        }
    }

    /**
     * Store download in list and start immediately if requested.
     */
    private void saveDownload() {
        // jetzt wird mit den angegebenen Pfaden gearbeitet
        datenDownload = new DatenDownload(active_pSet, film, DatenDownload.QUELLE_DOWNLOAD, null, jTextFieldName.getText(), Objects.requireNonNull(jComboBoxPfad.getSelectedItem()).toString(), getFilmResolution().toString());
        datenDownload.setGroesse(getFilmSize());
        datenDownload.arr[DatenDownload.DOWNLOAD_INFODATEI] = Boolean.toString(jCheckBoxInfodatei.isSelected());
        datenDownload.arr[DatenDownload.DOWNLOAD_SUBTITLE] = Boolean.toString(jCheckBoxSubtitle.isSelected());

        addDownloadToQueue();

        dispose();
    }

    private static class DialogPositionComponentListener extends ComponentAdapter {
        @Override
        public void componentResized(ComponentEvent e) {
            storeWindowPosition(e);
        }

        @Override
        public void componentMoved(ComponentEvent e) {
            storeWindowPosition(e);
        }

        private void storeWindowPosition(ComponentEvent e) {
            var config = ApplicationConfiguration.getConfiguration();
            var component = e.getComponent();

            var dims = component.getSize();
            var loc = component.getLocation();
            try {
                config.lock(LockMode.WRITE);
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.WIDTH, dims.width);
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.HEIGHT, dims.height);
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.X, loc.x);
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.Y, loc.y);
            }
            finally {
                config.unlock(LockMode.WRITE);
            }
        }
    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    // Generated using JFormDesigner non-commercial license
    private void initComponents() {
        var panel2 = new JPanel();
        var label2 = new JLabel();
        lblSender = new JLabel();
        var label3 = new JLabel();
        var scrollPane1 = new JScrollPane();
        jTextFieldSender = new MultilineLabel();
        var buttonPanel = new JPanel();
        jCheckBoxStarten = new JCheckBox();
        var panel1 = new JPanel();
        jButtonOk = new JButton();
        jButtonAbbrechen = new JButton();
        var jPanel1 = new JPanel();
        var jLabelSet = new JLabel();
        jComboBoxPset = new javax.swing.JComboBox<>();
        var jLabel1 = new JLabel();
        var panel3 = new JPanel();
        jComboBoxPfad = new javax.swing.JComboBox<>();
        jButtonZiel = new JButton();
        jButtonDelHistory = new JButton();
        var jLabel4 = new JLabel();
        jTextFieldName = new JTextField();
        jPanelSize = new JPanel();
        var jPanel3 = new JPanel();
        btnRequestLiveInfo = new JButton();
        lblBusyIndicator = new JXBusyLabel();
        var jPanel5 = new JPanel();
        lblStatus = new JLabel();
        lblAudioInfo = new JLabel();
        var jPanel6 = new JPanel();
        jRadioButtonAufloesungHd = new JRadioButton();
        jRadioButtonAufloesungHoch = new JRadioButton();
        jRadioButtonAufloesungKlein = new JRadioButton();
        var jPanel2 = new JPanel();
        jCheckBoxInfodatei = new JCheckBox();
        jCheckBoxPfadSpeichern = new JCheckBox();
        jCheckBoxSubtitle = new JCheckBox();

        //======== this ========
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Film speichern");
        setMinimumSize(new Dimension(690, 470));
        setPreferredSize(new Dimension(690, 470));
        setMaximumSize(new Dimension(1024, 800));
        var contentPane = getContentPane();
        contentPane.setLayout(new MigLayout(
            new LC().insets("5").hideMode(3).gridGap("5", "5"),
            // columns
            new AC()
                .grow().fill(),
            // rows
            new AC()
                .fill().gap()
                .fill().gap()
                .fill().gap()
                .fill().gap()
                .fill()));

        //======== panel2 ========
        {
            panel2.setBorder(new TitledBorder("Film:"));
            panel2.setLayout(new MigLayout(
                new LC().insets("5").hideMode(3).gridGap("5", "5"),
                // columns
                new AC()
                    .fill().gap()
                    .grow().fill(),
                // rows
                new AC()
                    .gap()
                    .fill()));

            //---- label2 ----
            label2.setText("Sender:");
            label2.setFont(label2.getFont().deriveFont(label2.getFont().getStyle() | Font.BOLD));
            panel2.add(label2, new CC().cell(0, 0));

            //---- lblSender ----
            lblSender.setText("text");
            lblSender.setFont(lblSender.getFont().deriveFont(lblSender.getFont().getStyle() | Font.BOLD));
            panel2.add(lblSender, new CC().cell(1, 0));

            //---- label3 ----
            label3.setText("Titel:");
            label3.setFont(label3.getFont().deriveFont(label3.getFont().getStyle() | Font.BOLD));
            label3.setHorizontalAlignment(SwingConstants.RIGHT);
            panel2.add(label3, new CC().cell(0, 1));

            //======== scrollPane1 ========
            {
                scrollPane1.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);

                //---- jTextFieldSender ----
                jTextFieldSender.setEditable(false);
                jTextFieldSender.setFont(jTextFieldSender.getFont().deriveFont(jTextFieldSender.getFont().getStyle() | Font.BOLD));
                jTextFieldSender.setText("ARD: Tatort, ...");
                scrollPane1.setViewportView(jTextFieldSender);
            }
            panel2.add(scrollPane1, new CC().cell(1, 1));
        }
        contentPane.add(panel2, new CC().cell(0, 0));

        //======== buttonPanel ========
        {
            buttonPanel.setLayout(new MigLayout(
                new LC().insets("5").hideMode(3).gridGap("5", "5"),
                // columns
                new AC()
                    .fill().gap()
                    .grow().align("right"),
                // rows
                new AC()
                    .fill()));

            //---- jCheckBoxStarten ----
            jCheckBoxStarten.setSelected(true);
            jCheckBoxStarten.setText("Download sofort starten");
            buttonPanel.add(jCheckBoxStarten, new CC().cell(0, 0));

            //======== panel1 ========
            {

                //---- jButtonOk ----
                jButtonOk.setText("Ok");

                //---- jButtonAbbrechen ----
                jButtonAbbrechen.setText("Abbrechen");

                GroupLayout panel1Layout = new GroupLayout(panel1);
                panel1.setLayout(panel1Layout);
                panel1Layout.setHorizontalGroup(
                    panel1Layout.createParallelGroup()
                        .addGroup(panel1Layout.createSequentialGroup()
                            .addContainerGap()
                            .addComponent(jButtonOk, GroupLayout.PREFERRED_SIZE, 93, GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jButtonAbbrechen)
                            .addContainerGap())
                );
                panel1Layout.linkSize(SwingConstants.HORIZONTAL, new Component[] {jButtonAbbrechen, jButtonOk});
                panel1Layout.setVerticalGroup(
                    panel1Layout.createParallelGroup()
                        .addGroup(panel1Layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(panel1Layout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                                .addComponent(jButtonOk)
                                .addComponent(jButtonAbbrechen))
                            .addContainerGap())
                );
            }
            buttonPanel.add(panel1, new CC().cell(1, 0));
        }
        contentPane.add(buttonPanel, new CC().cell(0, 4));

        //======== jPanel1 ========
        {
            jPanel1.setBorder(new TitledBorder("text"));
            jPanel1.setLayout(new MigLayout(
                new LC().insets("5").hideMode(3),
                // columns
                new AC()
                    .fill().gap()
                    .grow().fill(),
                // rows
                new AC()
                    .gap()
                    .gap()
                    ));

            //---- jLabelSet ----
            jLabelSet.setText("Set:");
            jPanel1.add(jLabelSet, new CC().cell(0, 0));
            jPanel1.add(jComboBoxPset, new CC().cell(1, 0));

            //---- jLabel1 ----
            jLabel1.setText("Zielpfad:");
            jPanel1.add(jLabel1, new CC().cell(0, 1));

            //======== panel3 ========
            {
                panel3.setLayout(new MigLayout(
                    new LC().insets("0").hideMode(3),
                    // columns
                    new AC()
                        .size("::530").grow().fill().gap()
                        .fill().gap()
                        .fill(),
                    // rows
                    new AC()
                        ));

                //---- jComboBoxPfad ----
                jComboBoxPfad.setEditable(true);
                panel3.add(jComboBoxPfad, new CC().cell(0, 0).growX());

                //---- jButtonZiel ----
                jButtonZiel.setText("F");
                jButtonZiel.setToolTipText("Zielpfad ausw\u00e4hlen");
                panel3.add(jButtonZiel, new CC().cell(1, 0));

                //---- jButtonDelHistory ----
                jButtonDelHistory.setText("H");
                jButtonDelHistory.setToolTipText("History l\u00f6schen");
                panel3.add(jButtonDelHistory, new CC().cell(2, 0));
            }
            jPanel1.add(panel3, new CC().cell(1, 1));

            //---- jLabel4 ----
            jLabel4.setText("Dateiname:");
            jPanel1.add(jLabel4, new CC().cell(0, 2));
            jPanel1.add(jTextFieldName, new CC().cell(1, 2));
        }
        contentPane.add(jPanel1, new CC().cell(0, 1));

        //======== jPanelSize ========
        {
            jPanelSize.setBorder(new TitledBorder("Download-Qualit\u00e4t"));
            jPanelSize.setLayout(new MigLayout(
                new LC().insets("0").hideMode(3).gridGap("5", "5"),
                // columns
                new AC()
                    .grow().fill(),
                // rows
                new AC()
                    .fill().gap()
                    .fill()));

            //======== jPanel3 ========
            {
                jPanel3.setLayout(new FlowLayout(FlowLayout.LEFT));

                //---- btnRequestLiveInfo ----
                btnRequestLiveInfo.setText("Codec-Details abrufen...");
                jPanel3.add(btnRequestLiveInfo);
                jPanel3.add(lblBusyIndicator);

                //======== jPanel5 ========
                {
                    jPanel5.setLayout(new GridLayout(2, 1));

                    //---- lblStatus ----
                    lblStatus.setText("status");
                    jPanel5.add(lblStatus);

                    //---- lblAudioInfo ----
                    lblAudioInfo.setText("audio");
                    jPanel5.add(lblAudioInfo);
                }
                jPanel3.add(jPanel5);
            }
            jPanelSize.add(jPanel3, new CC().cell(0, 1));

            //======== jPanel6 ========
            {
                jPanel6.setLayout(new FlowLayout());

                //---- jRadioButtonAufloesungHd ----
                jRadioButtonAufloesungHd.setText("H\u00f6chste/Hoch");
                jPanel6.add(jRadioButtonAufloesungHd);

                //---- jRadioButtonAufloesungHoch ----
                jRadioButtonAufloesungHoch.setText("Mittel");
                jPanel6.add(jRadioButtonAufloesungHoch);

                //---- jRadioButtonAufloesungKlein ----
                jRadioButtonAufloesungKlein.setText("Niedrig");
                jPanel6.add(jRadioButtonAufloesungKlein);
            }
            jPanelSize.add(jPanel6, new CC().cell(0, 0).alignX("left").growX(0));
        }
        contentPane.add(jPanelSize, new CC().cell(0, 3));

        //======== jPanel2 ========
        {
            jPanel2.setBorder(new TitledBorder("Optionen"));
            jPanel2.setLayout(new GridLayout(2, 2));

            //---- jCheckBoxInfodatei ----
            jCheckBoxInfodatei.setText("Lege Infodatei an");
            jCheckBoxInfodatei.setToolTipText("Erzeugt eine Infodatei im Format \"Infodatei.txt\"");
            jPanel2.add(jCheckBoxInfodatei);

            //---- jCheckBoxPfadSpeichern ----
            jCheckBoxPfadSpeichern.setText("Zielpfad speichern");
            jPanel2.add(jCheckBoxPfadSpeichern);

            //---- jCheckBoxSubtitle ----
            jCheckBoxSubtitle.setText("Untertitel speichern: \"Filmname.xxx\"");
            jPanel2.add(jCheckBoxSubtitle);
        }
        contentPane.add(jPanel2, new CC().cell(0, 2));
        pack();
        setLocationRelativeTo(getOwner());

        //---- buttonGroup1 ----
        var buttonGroup1 = new ButtonGroup();
        buttonGroup1.add(jRadioButtonAufloesungHd);
        buttonGroup1.add(jRadioButtonAufloesungHoch);
        buttonGroup1.add(jRadioButtonAufloesungKlein);
    }// </editor-fold>//GEN-END:initComponents

    // Variables declaration - do not modify//GEN-BEGIN:variables
    // Generated using JFormDesigner non-commercial license
    private JLabel lblSender;
    private MultilineLabel jTextFieldSender;
    private JCheckBox jCheckBoxStarten;
    private JButton jButtonOk;
    private JButton jButtonAbbrechen;
    private JComboBox<String> jComboBoxPset;
    private JComboBox<String> jComboBoxPfad;
    private JButton jButtonZiel;
    private JButton jButtonDelHistory;
    private JTextField jTextFieldName;
    private JPanel jPanelSize;
    private JButton btnRequestLiveInfo;
    private JXBusyLabel lblBusyIndicator;
    private JLabel lblStatus;
    private JLabel lblAudioInfo;
    private JRadioButton jRadioButtonAufloesungHd;
    private JRadioButton jRadioButtonAufloesungHoch;
    private JRadioButton jRadioButtonAufloesungKlein;
    private JCheckBox jCheckBoxInfodatei;
    private JCheckBox jCheckBoxPfadSpeichern;
    private JCheckBox jCheckBoxSubtitle;
    // End of variables declaration//GEN-END:variables
}
