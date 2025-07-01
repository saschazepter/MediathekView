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

package mediathek.gui.dialog.add_download

import com.github.kokorin.jaffree.StreamType
import com.github.kokorin.jaffree.ffprobe.FFprobe
import com.github.kokorin.jaffree.ffprobe.FFprobeResult
import com.github.kokorin.jaffree.ffprobe.Stream
import com.github.kokorin.jaffree.process.JaffreeAbnormalExitException
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import mediathek.config.Daten
import mediathek.config.MVColor
import mediathek.config.MVConfig
import mediathek.daten.DatenPset
import mediathek.daten.FilmResolution
import mediathek.gui.messages.DownloadListChangedEvent
import mediathek.mainwindow.MediathekGui
import mediathek.tool.*
import mediathek.tool.MessageBus.messageBus
import org.apache.commons.configuration2.sync.LockMode
import org.apache.commons.lang3.SystemUtils
import org.apache.logging.log4j.LogManager
import java.awt.Color
import java.awt.Dimension
import java.awt.Frame
import java.awt.event.ActionEvent
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import javax.swing.DefaultComboBoxModel
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

class DialogAddDownloadWithCoroutines(
    parent: Frame,
    film: mediathek.daten.DatenFilm,
    pSet: DatenPset?,
    requestedResolution: java.util.Optional<FilmResolution.Enum>
) : DialogAddDownload(parent, film, pSet, requestedResolution) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    private var liveInfoJob: Job? = null
    private var highQualityMandated: Boolean = false

    companion object {
        private val logger = LogManager.getLogger()
        private const val NO_DATA_AVAILABLE = "Keine Daten verfügbar."
        private const val TITLED_BORDER_STRING = "Download-Qualität"
        private var stopBeob = false
        private var nameGeaendert = false
        private var ffprobePath: Path? = null
        private var orgPfad = ""

    }

    init {
        getRootPane().setDefaultButton(jButtonOk)
        EscapeKeyHandler.installHandler(this) { this.dispose() }

        setupUI()

        setupMinimumSizeForOs()
        restoreWindowSizeFromConfig() //only install on windows and linux, macOS works...
        installMinResizePreventer()

        setLocationRelativeTo(parent)

        addComponentListener(DialogPositionComponentListener())

        btnRequestLiveInfo.addActionListener {
            liveInfoJob?.cancel()
            liveInfoJob = coroutineScope.launch {
                fetchLiveFilmInfoCoroutine()
            }
        }

        jButtonOk.requestFocus()
    }

    override fun dispose() {
        coroutineScope.cancel()
        super.dispose()
    }

    private fun setupUI() {
        setupBusyIndicator()
        detectFfprobeExecutable()

        // launch async tasks first
        launchResolutionFutures()

        jCheckBoxStarten.setSelected(MVConfig.get(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD_D_STARTEN).toBoolean())
        jCheckBoxStarten.addActionListener { _: ActionEvent? ->
            MVConfig.add(
                MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD_D_STARTEN,
                jCheckBoxStarten.isSelected.toString()
            )
        }

        setupZielButton()

        jButtonOk.addActionListener { _: ActionEvent? ->
            if (check()) {
                saveComboPfad(jComboBoxPfad, orgPfad)
                saveDownload()
            }
        }

        jButtonAbbrechen.addActionListener { _: ActionEvent? -> dispose() }

        setupPSetComboBox()
        setupSenderTextField()
        setupNameTextField()
        setupPathTextComponent()

        setupFilmQualityRadioButtons()

        setupDeleteHistoryButton()
        setupPfadSpeichernCheckBox()

        waitForFileSizeFutures()

        setupResolutionButtons()
        setupInfoFileCreationCheckBox()

        calculateAndCheckDiskSpace()
        nameGeaendert = false
    }

    /**
     * Store download in list and start immediately if requested.
     */
    private fun saveDownload() {
        datenDownload = mediathek.daten.DatenDownload(
            active_pSet,
            film,
            mediathek.daten.DatenDownload.QUELLE_DOWNLOAD,
            null,
            jTextFieldName.text,
            jComboBoxPfad.selectedItem?.toString() ?: "",
            getFilmResolution().toString()
        ).apply {
            setGroesse(getFilmSize())
            arr[mediathek.daten.DatenDownload.DOWNLOAD_INFODATEI] = jCheckBoxInfodatei.isSelected.toString()
            arr[mediathek.daten.DatenDownload.DOWNLOAD_SUBTITLE] = jCheckBoxSubtitle.isSelected.toString()
        }

        addDownloadToQueue()
        dispose()
    }

    /**
     * Setup the resolution radio buttons based on available download URLs.
     */
    private fun setupResolutionButtons() {
        active_pSet = listeSpeichern[jComboBoxPset.getSelectedIndex()]

        prepareResolutionButtons()

        prepareSubtitleCheckbox()
        setNameFilm()
    }

    private fun prepareResolutionButtons() {
        requestedResolution.ifPresent { highQualityMandated = it == FilmResolution.Enum.HIGH_QUALITY }

        when {
            highQualityMandated || isHighQualityRequested() -> jRadioButtonAufloesungHd.isSelected = true
            isLowQualityRequested() -> jRadioButtonAufloesungKlein.isSelected = true
            else -> jRadioButtonAufloesungHoch.isSelected = true
        }
    }

    private fun setupInfoFileCreationCheckBox() {
        //disable for Livestreams as they do not contain useful data, even if pset wants it...
        jCheckBoxInfodatei.setEnabled(!film.isLivestream)
        if (!film.isLivestream) {
            jCheckBoxInfodatei.setSelected(active_pSet.shouldCreateInfofile())
        } else jCheckBoxInfodatei.setSelected(false)
    }

    private fun isLowQualityRequested(): Boolean {
        return active_pSet.arr[DatenPset.PROGRAMMSET_AUFLOESUNG] == FilmResolution.Enum.LOW.toString() &&
                !film.lowQualityUrl.isEmpty()
    }

    private fun isHighQualityRequested(): Boolean {
        return active_pSet.arr[DatenPset.PROGRAMMSET_AUFLOESUNG] == FilmResolution.Enum.HIGH_QUALITY.toString()
                && film.isHighQuality
    }

    /**
     * Return the resolution based on selected RadioButton.
     */
    private fun getFilmResolution(): FilmResolution.Enum {
        return when {
            jRadioButtonAufloesungHd.isSelected -> FilmResolution.Enum.HIGH_QUALITY
            jRadioButtonAufloesungKlein.isSelected -> FilmResolution.Enum.LOW
            else -> FilmResolution.Enum.NORMAL
        }
    }

    private fun prepareSubtitleCheckbox() {
        if (!film.hasSubtitle()) {
            jCheckBoxSubtitle.setEnabled(false)
        } else {
            jCheckBoxSubtitle.setSelected(active_pSet.shouldDownloadSubtitle())
        }
    }

    private fun setNameFilm() {
        // beim ersten Mal werden die Standardpfade gesucht
        if (!nameGeaendert) {
            // nur wenn vom Benutzer noch nicht geändert!
            stopBeob = true

            datenDownload = mediathek.daten.DatenDownload(
                active_pSet,
                film,
                mediathek.daten.DatenDownload.QUELLE_DOWNLOAD,
                null,
                "",
                "",
                getFilmResolution().toString()
            )

            if (datenDownload.arr[mediathek.daten.DatenDownload.DOWNLOAD_ZIEL_DATEINAME].isEmpty()) {
                // dann wird nicht gespeichert → eigentlich falsche Seteinstellungen?
                jTextFieldName.isEnabled = false
                jComboBoxPfad.isEnabled = false
                jButtonZiel.isEnabled = false
                jTextFieldName.text = ""
                jComboBoxPfad.model = DefaultComboBoxModel(arrayOf(""))
            } else {
                jTextFieldName.isEnabled = true
                jComboBoxPfad.isEnabled = true
                jButtonZiel.isEnabled = true
                jTextFieldName.text = datenDownload.arr[mediathek.daten.DatenDownload.DOWNLOAD_ZIEL_DATEINAME]
                setModelPfad(datenDownload.arr[mediathek.daten.DatenDownload.DOWNLOAD_ZIEL_PFAD], jComboBoxPfad)
                orgPfad = datenDownload.arr[mediathek.daten.DatenDownload.DOWNLOAD_ZIEL_PFAD]
            }

            stopBeob = false
        }
    }

    private fun addDownloadToQueue() {
        Daten.getInstance().listeDownloads.addMitNummer(datenDownload)
        messageBus.publishAsync(DownloadListChangedEvent())

        if (jCheckBoxStarten.isSelected) {
            datenDownload.startDownload()
        }
    }

    private fun getFilmSize(): String {
        return when {
            jRadioButtonAufloesungHd.isSelected -> dateiGroesse_HQ
            jRadioButtonAufloesungKlein.isSelected -> dateiGroesse_Klein
            else -> dateiGroesse_Hoch
        }
    }


    private fun check(): Boolean {
        val pfadRaw = jComboBoxPfad.selectedItem?.toString() ?: return false
        val name = jTextFieldName.text

        if (datenDownload == null) return false

        if (pfadRaw.isEmpty() || name.isEmpty()) {
            MVMessageDialog.showMessageDialog(
                this,
                "Pfad oder Name ist leer",
                "Fehlerhafter Pfad/Name!",
                JOptionPane.ERROR_MESSAGE
            )
            return false
        }

        val pfad = if (pfadRaw.endsWith(File.separator)) pfadRaw else pfadRaw + File.separator

        return if (GuiFunktionenProgramme.checkPathWriteable(pfad)) {
            true
        } else {
            MVMessageDialog.showMessageDialog(
                this,
                "Pfad ist nicht beschreibbar",
                "Fehlerhafter Pfad!",
                JOptionPane.ERROR_MESSAGE
            )
            false
        }
    }

    private fun setupFilmQualityRadioButtons() {
        val listener: ActionListener? = ActionListener {
            setNameFilm()
            lblStatus.setText("")
            lblAudioInfo.setText("")
            lblBusyIndicator.setBusy(false)
            lblBusyIndicator.isVisible = false
            liveInfoJob?.cancel()
        }
        jRadioButtonAufloesungHd.addActionListener(listener)
        jRadioButtonAufloesungHd.setEnabled(!film.highQualityUrl.isEmpty())

        jRadioButtonAufloesungKlein.addActionListener(listener)
        jRadioButtonAufloesungKlein.setEnabled(!film.lowQualityUrl.isEmpty())

        jRadioButtonAufloesungHoch.addActionListener(listener)
        jRadioButtonAufloesungHoch.setSelected(true)
    }

    private fun setupPSetComboBox() {
        // disable when only one entry...
        if (listeSpeichern.size == 1) {
            jComboBoxPset.setEnabled(false)
        }

        val model = DefaultComboBoxModel(listeSpeichern.getObjectDataCombo())
        jComboBoxPset.setModel(model)

        if (active_pSet != null) {
            jComboBoxPset.setSelectedItem(active_pSet.name)
        } else {
            active_pSet = listeSpeichern[jComboBoxPset.getSelectedIndex()]
        }
        jComboBoxPset.addActionListener { _: ActionEvent? -> setupResolutionButtons() }
    }

    private fun setupSenderTextField() {
        jTextFieldSender.text = "${film.sender}: ${film.title}"
        jTextFieldSender.setBackground(UIManager.getColor("Label.background"))
    }

    private fun setupDeleteHistoryButton() {
        jButtonDelHistory.setText("")
        jButtonDelHistory.setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/trash-can.svg"))
        jButtonDelHistory.addActionListener { _: ActionEvent? ->
            MVConfig.add(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN, "")
            jComboBoxPfad.setModel(DefaultComboBoxModel(arrayOf<String?>(orgPfad)))
        }
    }

    private fun setupPfadSpeichernCheckBox() {
        val config = ApplicationConfiguration.getConfiguration()
        jCheckBoxPfadSpeichern.setSelected(
            config.getBoolean(
                ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH,
                true
            )
        )
        jCheckBoxPfadSpeichern.addActionListener { _: ActionEvent? ->
            config.setProperty(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, jCheckBoxPfadSpeichern.isSelected)
        }
    }

    private fun detectFfprobeExecutable() {
        try {
            ffprobePath = GuiFunktionenProgramme.findExecutableOnPath("ffprobe").parent
        } catch (ex: java.lang.Exception) {
            logger.error("ffprobe not found", ex)
            lblBusyIndicator.setText("Hilfsprogramm nicht gefunden!")
            lblBusyIndicator.setForeground(Color.RED)
            btnRequestLiveInfo.setEnabled(false)
        }
    }

    /** Prevents that a dialog can be resized smaller than its minimum dimensions.
     * Needed on Windows, but not macOS and Linux. */
    private fun installMinResizePreventer() {
        if (!SystemUtils.IS_OS_WINDOWS) return

        addComponentListener(object : ComponentAdapter() {
            override fun componentResized(e: ComponentEvent?) {
                val size = getSize()
                val w = max(size.width, minimumSize.width)
                val h = max(size.height, minimumSize.height)
                if (w != size.width || h != size.height) {
                    setSize(w, h)
                }
            }
        })
    }

    private fun restoreWindowSizeFromConfig() {
        val config = ApplicationConfiguration.getConfiguration()
        try {
            config.lock(LockMode.READ)
            val width = max(config.getInt(ApplicationConfiguration.AddDownloadDialog.WIDTH), MINIMUM_WIDTH)
            val height = max(config.getInt(ApplicationConfiguration.AddDownloadDialog.HEIGHT), MINIMUM_HEIGHT)
            val x = config.getInt(ApplicationConfiguration.AddDownloadDialog.X)
            val y = config.getInt(ApplicationConfiguration.AddDownloadDialog.Y)

            setBounds(x, y, width, height)
        } catch (_: NoSuchElementException) {
            //do not restore anything
        } finally {
            config.unlock(LockMode.READ)
        }
    }

    private fun setupBusyIndicator() {
        lblBusyIndicator.setText("")
        lblBusyIndicator.setBusy(false)
        lblBusyIndicator.isVisible = false
        lblStatus.setText("")
        lblAudioInfo.setText("")
    }

    private fun setupMinimumSizeForOs() {
        if (SystemUtils.IS_OS_WINDOWS) MINIMUM_HEIGHT -= 10
        else if (SystemUtils.IS_OS_LINUX) {
            MINIMUM_HEIGHT = 520
            MINIMUM_WIDTH = 800
        }
        minimumSize = Dimension(MINIMUM_WIDTH, MINIMUM_HEIGHT)
    }

    private suspend fun fetchLiveFilmInfoCoroutine() {
        btnRequestLiveInfo.isEnabled = false
        lblBusyIndicator.isVisible = true
        lblBusyIndicator.isBusy = true
        lblStatus.text = ""
        lblAudioInfo.text = ""

        try {
            val url = film.getUrlFuerAufloesung(getFilmResolution())

            val result = withContext(Dispatchers.IO) {
                FFprobe.atPath(ffprobePath)
                    .setShowStreams(true)
                    .setInput(url)
                    .execute()
            }

            processFFprobeResult(result)
        } catch (_: CancellationException) {
            clearInfo()
        } catch (ex: JaffreeAbnormalExitException) {
            setupLabels(getJaffreeErrorString(ex))
        } catch (_: Exception) {
            setupLabels("Unbekannter Fehler aufgetreten.")
        } finally {
            resetBusyLabelAndButton()
        }
    }

    private fun processFFprobeResult(result: FFprobeResult) {
        val audioStream = result.streams.find { it.codecType == StreamType.AUDIO }
        val videoStream = result.streams.find { it.codecType == StreamType.VIDEO }

        lblAudioInfo.foreground = UIManager.getColor(KEY_LABEL_FOREGROUND)
        lblAudioInfo.text = audioStream?.let { getAudioInfo(it, it.sampleRate) } ?: NO_DATA_AVAILABLE

        lblStatus.foreground = UIManager.getColor(KEY_LABEL_FOREGROUND)
        lblStatus.text = videoStream?.let {
            val frameRate = it.avgFrameRate.toInt()
            val codecName = getVideoCodecName(it)
            getVideoInfoString(it, frameRate, codecName)
        } ?: NO_DATA_AVAILABLE
    }

    private fun resetBusyLabelAndButton() {
        lblBusyIndicator.setBusy(false)
        lblBusyIndicator.isVisible = false
        btnRequestLiveInfo.setEnabled(true)
    }

    private fun clearInfo() {
        lblStatus.text = ""
        lblAudioInfo.text = ""
    }

    private fun setupLabels(text: String) {
        lblStatus.text = text
        lblStatus.foreground = Color.RED
        lblAudioInfo.text = ""
    }

    /**
     * Return only the first part of the long codec name.
     *
     * @param stream The video stream from ffprobe.
     * @return First entry of long codec name.
     */
    private fun getVideoCodecName(stream: Stream): String {
        logger.trace("video codec long name: ${stream.codecLongName}")
        return stream.codecLongName.split("/")
            .firstOrNull()
            ?.trim()
            ?: stream.codecLongName
    }

    private fun getAudioInfo(stream: Stream, sampleRate: Int?): String {
        val bitRate = safeProcessBitRate(stream.bitRate)
        return if (bitRate == 0) {
            "Audio: ${sampleRate ?: "?"} Hz, ${stream.codecLongName}"
        } else {
            "Audio: ${sampleRate ?: "?"} Hz, $bitRate kBit/s, ${stream.codecLongName}"
        }
    }

    private fun getVideoInfoString(stream: Stream, frameRate: Int, codecName: String?): String {
        val bitRate = safeProcessBitRate(stream.bitRate)
        return if (bitRate == 0) {
            "Video: ${stream.width}x${stream.height}, $frameRate fps (avg), $codecName"
        } else {
            "Video: ${stream.width}x${stream.height}, $bitRate kBit/s, $frameRate fps (avg), $codecName"
        }
    }

    private fun safeProcessBitRate(inBitRate: Int?): Int {
        return try {
            (inBitRate ?: 0) / 1000
        } catch (_: Exception) {
            0
        }
    }

    private fun getJaffreeErrorString(ex: JaffreeAbnormalExitException): String {
        return try {
            val msg = ex.processErrorLogMessages.first().message.split(":")
            val errMsg = msg.last().trim()
            if (errMsg.startsWith("Server returned ")) {
                errMsg.removePrefix("Server returned ").trim()
            } else {
                "Unbekannter Fehler aufgetreten."
            }
        } catch (_: Exception) {
            "Unbekannter Fehler aufgetreten."
        }
    }

    /**
     * Calculate free disk space on volume and check if the movies can be safely downloaded.
     */
    fun calculateAndCheckDiskSpace() {
        UIManager.getColor(KEY_LABEL_FOREGROUND)?.let { fgColor ->
            jRadioButtonAufloesungHd.foreground = fgColor
            jRadioButtonAufloesungHoch.foreground = fgColor
            jRadioButtonAufloesungKlein.foreground = fgColor
        }

        try {
            val filmBorder = jPanelSize.border as? javax.swing.border.TitledBorder ?: return
            var usableSpace = getFreeDiskSpace(cbPathTextComponent.text)

            filmBorder.title = if (usableSpace > 0) {
                "$TITLED_BORDER_STRING [ Freier Speicherplatz: ${FileUtils.humanReadableByteCountBinary(usableSpace)} ]"
            } else {
                TITLED_BORDER_STRING
            }

            // Border needs to be repainted after update...
            jPanelSize.repaint()

            // jetzt noch prüfen, obs auf die Platte passt
            usableSpace /= FileSize.ONE_MiB
            if (usableSpace > 0) {
                if (dateiGroesse_HQ.isNotEmpty()) {
                    val size = dateiGroesse_HQ.toIntOrNull() ?: 0
                    if (size > usableSpace) {
                        jRadioButtonAufloesungHd.foreground = Color.RED
                    }
                }
                if (dateiGroesse_Hoch.isNotEmpty()) {
                    val size = dateiGroesse_Hoch.toIntOrNull() ?: 0
                    if (size > usableSpace) {
                        jRadioButtonAufloesungHoch.foreground = Color.RED
                    }
                }
                if (dateiGroesse_Klein.isNotEmpty()) {
                    val size = dateiGroesse_Klein.toIntOrNull() ?: 0
                    if (size > usableSpace) {
                        jRadioButtonAufloesungKlein.foreground = Color.RED
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("calculateAndCheckDiskSpace()", ex)
        }
    }

    private fun setupPathTextComponent() {
        cbPathTextComponent = jComboBoxPfad.editor.editorComponent as JTextComponent
        cbPathTextComponent.isOpaque = true

        cbPathTextComponent.document.addDocumentListener(object : DocumentListener {
            private val MAX_PATH_LENGTH = 50

            override fun insertUpdate(e: DocumentEvent?) = tus()
            override fun removeUpdate(e: DocumentEvent?) = tus()
            override fun changedUpdate(e: DocumentEvent?) = tus()

            private fun truncate() {
                val text = cbPathTextComponent.text
                if (text.length > MAX_PATH_LENGTH) {
                    val shortText = "..." + text.takeLast(MAX_PATH_LENGTH)
                    SwingUtilities.invokeLater {
                        cbPathTextComponent.text = shortText
                    }
                }
            }

            private fun fileNameCheck(filePath: String) {
                val editor = jComboBoxPfad.editor.editorComponent
                if (filePath != FilenameUtils.checkDateiname(filePath, true)) {
                    editor.background = MVColor.DOWNLOAD_FEHLER.color
                } else {
                    editor.background = UIManager.getColor(KEY_TEXTFIELD_BACKGROUND)
                }
            }

            private fun tus() {
                if (!stopBeob) {
                    nameGeaendert = true
                    // do not perform check on Windows
                    if (SystemUtils.IS_OS_WINDOWS) {
                        (jComboBoxPfad.selectedItem as? String)?.let { fileNameCheck(it) }
                    }
                    calculateAndCheckDiskSpace()
                }
                truncate()
            }
        })
    }

    private fun setupNameTextField() {
        jTextFieldName.document.addDocumentListener(object : DocumentListener {

            override fun insertUpdate(e: DocumentEvent?) = tus()
            override fun removeUpdate(e: DocumentEvent?) = tus()
            override fun changedUpdate(e: DocumentEvent?) = tus()

            private fun tus() {
                if (!stopBeob) {
                    nameGeaendert = true
                    if (jTextFieldName.text != FilenameUtils.checkDateiname(jTextFieldName.text, false)) {
                        jTextFieldName.background = MVColor.DOWNLOAD_FEHLER.color
                    } else {
                        jTextFieldName.background = UIManager.getDefaults().getColor(KEY_TEXTFIELD_BACKGROUND)
                    }
                }
            }
        })
    }

    /**
     * Get the free disk space for a selected path.
     *
     * @return Free disk space in bytes.
     */
    private fun getFreeDiskSpace(strPath: String): Long {
        if (strPath.isEmpty()) return 0L

        return try {
            var path = Paths.get(strPath)
            while (path != null && Files.notExists(path)) {
                path = path.parent
            }

            if (path == null) {
                0L
            } else {
                Files.getFileStore(path).usableSpace
            }
        } catch (ex: Exception) {
            logger.error("getFreeDiskSpace Failed", ex)
            0L
        }
    }

    private fun setupZielButton() {
        jButtonZiel.icon = SVGIconUtilities.createSVGIcon("icons/fontawesome/folder-open.svg")
        jButtonZiel.text = ""

        jButtonZiel.addActionListener {
            val initialDirectory = (jComboBoxPfad.selectedItem as? String).orEmpty()

            FileDialogs.chooseDirectoryLocation(
                MediathekGui.ui(),
                "Film speichern",
                initialDirectory
            )?.let { directory ->
                val selectedDirectory = directory.absolutePath
                SwingUtilities.invokeLater {
                    jComboBoxPfad.addItem(selectedDirectory)
                    jComboBoxPfad.selectedItem = selectedDirectory
                }
            }
        }
    }

    private class DialogPositionComponentListener : ComponentAdapter() {

        override fun componentResized(e: ComponentEvent) {
            storeWindowPosition(e)
        }

        override fun componentMoved(e: ComponentEvent) {
            storeWindowPosition(e)
        }

        private fun storeWindowPosition(e: ComponentEvent) {
            val config = ApplicationConfiguration.getConfiguration()
            val component = e.component

            val dims = component.size
            val loc = component.location

            try {
                config.lock(LockMode.WRITE)
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.WIDTH, dims.width)
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.HEIGHT, dims.height)
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.X, loc.x)
                config.setProperty(ApplicationConfiguration.AddDownloadDialog.Y, loc.y)
            } finally {
                config.unlock(LockMode.WRITE)
            }
        }

    }


}