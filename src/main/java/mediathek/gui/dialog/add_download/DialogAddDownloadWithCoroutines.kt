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
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.swing.Swing
import mediathek.config.Daten
import mediathek.config.MVColor
import mediathek.config.MVConfig
import mediathek.daten.*
import mediathek.gui.messages.DownloadListChangedEvent
import mediathek.mainwindow.MediathekGui
import mediathek.tool.*
import mediathek.tool.MessageBus.messageBus
import org.apache.commons.configuration2.sync.LockMode
import org.apache.commons.lang3.SystemUtils
import org.apache.logging.log4j.LogManager
import java.awt.*
import java.awt.event.ActionListener
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.text.JTextComponent
import kotlin.coroutines.cancellation.CancellationException

class DialogAddDownloadWithCoroutines(
    parent: Frame,
    private val film: DatenFilm,
    /**
     * The currently selected pSet or null when no selection.
     */
    private var activeProgramSet: DatenPset,
    private val requestedResolution: Optional<FilmResolution.Enum>
) : DialogAddDownload(parent) {
    private sealed interface LiveInfoCommand {
        data object Cancel : LiveInfoCommand
        data class Fetch(val resolution: FilmResolution.Enum) : LiveInfoCommand
    }

    private data class LiveInfoText(
        val video: String = "",
        val audio: String = ""
    )

    private data class ResolutionSizes(
        val high: String = "",
        val normal: String = "",
        val low: String = ""
    ) {
        fun forResolution(resolution: FilmResolution.Enum): String {
            return when (resolution) {
                FilmResolution.Enum.HIGH_QUALITY -> high
                FilmResolution.Enum.LOW -> low
                else -> normal
            }
        }
    }

    private data class ResolutionButtonLabels(
        val high: String,
        val normal: String,
        val low: String
    )

    private data class StoredDialogPosition(
        val x: Int,
        val y: Int
    )

    private val uiScopeDelegate = lazy(LazyThreadSafetyMode.NONE) {
        CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    }
    private val uiScope by uiScopeDelegate
    private val liveInfoRequests = MutableSharedFlow<LiveInfoCommand>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    private var highQualityMandated: Boolean = false
    private var stopBeob = false
    private var nameGeaendert = false
    private var ffprobePath: Path? = null
    private var orgPfad = ""
    private val appConfig get() = ApplicationConfiguration.getConfiguration()
    private val listeSpeichern: ListePset = Daten.listePset.listeSpeichern
    private lateinit var resolutionButtonLabels: ResolutionButtonLabels
    private lateinit var cbPathTextComponent: JTextComponent
    private lateinit var datenDownload: DatenDownload
    private var resolutionSizes = ResolutionSizes()

    companion object {
        private val logger = LogManager.getLogger()
        private const val MINIMUM_DIALOG_WIDTH = 720
        private const val NO_DATA_AVAILABLE = "Keine Daten verfügbar."
        private const val TITLED_BORDER_STRING = "Download-Qualität"
        private const val KEY_LABEL_FOREGROUND: String = "Label.foreground"
        private const val KEY_TEXTFIELD_BACKGROUND: String = "TextField.background"
        private val LIVE_INFO_PLACEHOLDER = LiveInfoText(
            video = "Video: 1920x1080, 2220 kBit/s, 50 fps (avg), H.264",
            audio = "Audio: 48000 Hz, 128 kBit/s, AAC (Advanced Audio Coding)"
        )

        @JvmStatic
        fun saveComboPfad(jcb: JComboBox<String>, orgPath: String) {
            val pfade = mutableListOf<String>()
            val s = jcb.selectedItem?.toString().orEmpty()

            if (s != orgPath || ApplicationConfiguration.getConfiguration()
                    .getBoolean(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, true)
            ) {
                pfade.add(s)
            }

            for (i in 0 until jcb.itemCount) {
                val item = jcb.getItemAt(i)
                if (item != orgPath && item !in pfade) {
                    pfade.add(item)
                }
            }

            if (pfade.isNotEmpty()) {
                val joined = pfade
                    .filter { it.isNotEmpty() }
                    .take(mediathek.config.Konstanten.MAX_PFADE_DIALOG_DOWNLOAD)
                    .joinToString("<>")
                MVConfig.add(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN, joined)
            }
        }

        @JvmStatic
        fun setModelPfad(pfad: String, jcb: JComboBox<String>) {
            val pfade = mutableListOf<String>()
            val showLastUsedPath = ApplicationConfiguration.getConfiguration()
                .getBoolean(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, true)

            // Wenn gewünscht, den letzten verwendeten Pfad an den Anfang setzen
            if (!showLastUsedPath && pfad.isNotEmpty()) {
                pfade.add(pfad)
            }

            val gespeichertePfade = MVConfig.get(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN)
            if (gespeichertePfade.isNotEmpty()) {
                val p = gespeichertePfade.split("<>")
                for (s in p) {
                    if (s !in pfade) {
                        pfade.add(s)
                    }
                }
            }

            // aktueller Pfad ans Ende setzen
            if (showLastUsedPath && pfad.isNotEmpty() && pfad !in pfade) {
                pfade.add(pfad)
            }

            jcb.model = DefaultComboBoxModel(pfade.toTypedArray())
        }

    }

    init {
        configureWindowDefaults()
        initializeUi()
        bindUi()
        initializeDialogSize(parent)
        registerWindowPositionTracking()
        startCoroutineBindings()
        btnDownloadImmediately.requestFocus()
    }

    override fun dispose() {
        if (uiScopeDelegate.isInitialized()) {
            uiScope.cancel()
        }
        super.dispose()
    }

    private fun configureWindowDefaults() {
        rootPane.defaultButton = btnDownloadImmediately
        EscapeKeyHandler.installHandler(this) { dispose() }
    }

    private fun initializeUi() {
        setupBusyIndicator()
        stabilizeLiveInfoArea()
        detectFfprobeExecutable()
        setupZielButton()
        setupPSetComboBox()
        resolutionButtonLabels = ResolutionButtonLabels(
            high = jRadioButtonAufloesungHd.text,
            normal = jRadioButtonAufloesungHoch.text,
            low = jRadioButtonAufloesungKlein.text
        )
        setupFilmHeader()
        setupFilmQualityRadioButtons()
        setupDeleteHistoryButton()
        setupPfadSpeichernCheckBox()
        applySelectedProgramSet()
        setupNameTextField()
        setupPathEditor()
        nameGeaendert = false
    }

    private fun bindUi() {
        btnDownloadImmediately.addActionListener { prepareDownload(true) }
        btnQueueDownload.addActionListener { prepareDownload(false) }
        jButtonAbbrechen.addActionListener { dispose() }
        btnRequestLiveInfo.addActionListener {
            liveInfoRequests.tryEmit(LiveInfoCommand.Fetch(getFilmResolution()))
        }
    }

    private fun initializeDialogSize(parent: Frame) {
        updateMinimumSizeFromPackedLayout()
        constrainPackedSizeToScreen()
        removeStoredWindowSizeFromConfig()
        restoreWindowPositionFromConfig(parent)
    }

    private fun registerWindowPositionTracking() {
        addComponentListener(DialogPositionComponentListener())
    }

    private fun startCoroutineBindings() {
        startLiveInfoCollector()
        startPathObservation()
        loadInitialBackgroundData()
    }

    private fun startLiveInfoCollector() {
        uiScope.launch {
            liveInfoRequests.collectLatest { command ->
                when (command) {
                    LiveInfoCommand.Cancel -> resetLiveInfoDisplay()
                    is LiveInfoCommand.Fetch -> fetchLiveFilmInfo(command.resolution)
                }
            }
        }
    }

    private fun loadInitialBackgroundData() {
        uiScope.launch {
            applyResolutionSizes(loadResolutionSizes())
            calculateAndCheckDiskSpaceAsync()
        }
    }

    private fun stabilizeLiveInfoArea() {
        val originalText = LiveInfoText(lblStatus.text, lblAudioInfo.text)
        showLiveInfo(LIVE_INFO_PLACEHOLDER)

        lblStatus.preferredSize = lblStatus.preferredSize
        lblStatus.minimumSize = lblStatus.preferredSize
        lblAudioInfo.preferredSize = lblAudioInfo.preferredSize
        lblAudioInfo.minimumSize = lblAudioInfo.preferredSize

        showLiveInfo(originalText)
    }

    private fun updateMinimumSizeFromPackedLayout() {
        pack()
        minimumSize = Dimension(
            MINIMUM_DIALOG_WIDTH.coerceAtLeast(size.width),
            size.height
        )
    }

    private fun constrainPackedSizeToScreen() {
        val usableBounds = getUsableScreenBounds()
        val boundedWidth = size.width.coerceAtMost(usableBounds.width)
        val boundedHeight = size.height.coerceAtMost(usableBounds.height)
        if (boundedWidth != size.width || boundedHeight != size.height) {
            size = Dimension(boundedWidth, boundedHeight)
        }
        minimumSize = Dimension(
            minimumSize.width.coerceAtMost(usableBounds.width),
            minimumSize.height.coerceAtMost(usableBounds.height)
        )
    }

    private fun restoreWindowPositionFromConfig(parent: Frame) {
        val storedPosition = readStoredDialogPosition()
        if (storedPosition == null) {
            setLocationRelativeTo(parent)
        } else {
            applyStoredPosition(storedPosition)
        }
    }

    private fun removeStoredWindowSizeFromConfig() {
        withConfigLock(LockMode.WRITE) {
            appConfig.clearProperty(ApplicationConfiguration.AddDownloadDialog.WIDTH)
            appConfig.clearProperty(ApplicationConfiguration.AddDownloadDialog.HEIGHT)
        }
    }

    private fun readStoredDialogPosition(): StoredDialogPosition? {
        return try {
            withConfigLock(LockMode.READ) {
                StoredDialogPosition(
                    x = appConfig.getInt(ApplicationConfiguration.AddDownloadDialog.X),
                    y = appConfig.getInt(ApplicationConfiguration.AddDownloadDialog.Y)
                )
            }
        } catch (_: NoSuchElementException) {
            null
        }
    }

    private fun applyStoredPosition(position: StoredDialogPosition) {
        val usableBounds = getUsableScreenBounds()
        val boundedWidth = width.coerceAtMost(usableBounds.width)
        val boundedHeight = height.coerceAtMost(usableBounds.height)
        val maxX = usableBounds.x + usableBounds.width - boundedWidth
        val maxY = usableBounds.y + usableBounds.height - boundedHeight
        val boundedX = position.x.coerceIn(usableBounds.x, maxX)
        val boundedY = position.y.coerceIn(usableBounds.y, maxY)
        setLocation(boundedX, boundedY)
    }

    private fun getUsableScreenBounds(): Rectangle {
        val maximumWindowBounds = GraphicsEnvironment.getLocalGraphicsEnvironment().maximumWindowBounds
        return graphicsConfiguration?.bounds?.intersection(maximumWindowBounds) ?: maximumWindowBounds
    }

    private fun prepareDownload(startAutomatically: Boolean) {
        if (check()) {
            saveComboPfad(jComboBoxPfad, orgPfad)
            saveDownload(startAutomatically)
        }
    }

    /**
     * Store download in list and start immediately if requested.
     */
    private fun saveDownload(startAutomatically: Boolean) {
        datenDownload = DatenDownload(
            activeProgramSet,
            film,
            DatenDownload.QUELLE_DOWNLOAD,
            null,
            jTextFieldName.text,
            jComboBoxPfad.selectedItem?.toString() ?: "",
            getFilmResolution().toString()
        ).apply {
            setGroesse(getFilmSize())
            arr[DatenDownload.DOWNLOAD_INFODATEI] = jCheckBoxInfodatei.isSelected.toString()
            arr[DatenDownload.DOWNLOAD_SUBTITLE] = jCheckBoxSubtitle.isSelected.toString()
        }

        addDownloadToQueue(startAutomatically)
        dispose()
    }

    /**
     * Setup the resolution radio buttons based on available download URLs.
     */
    private fun applySelectedProgramSet() {
        activeProgramSet = listeSpeichern[jComboBoxPset.getSelectedIndex()]
        selectResolution()
        updateSubtitleCheckbox()
        updateInfoFileCreationCheckBox()
        setNameFilm()
    }

    private fun selectResolution() {
        requestedResolution.ifPresent { highQualityMandated = it == FilmResolution.Enum.HIGH_QUALITY }

        when {
            highQualityMandated || isHighQualityRequested() -> jRadioButtonAufloesungHd.isSelected = true
            isLowQualityRequested() -> jRadioButtonAufloesungKlein.isSelected = true
            else -> jRadioButtonAufloesungHoch.isSelected = true
        }
    }

    private fun updateInfoFileCreationCheckBox() {
        jCheckBoxInfodatei.apply {
            if (film.isLivestream) {
                //disable for Livestreams as they do not contain useful data, even if pset wants it...
                isEnabled = false
                isSelected = false
            } else {
                isEnabled = true
                isSelected = activeProgramSet.shouldCreateInfofile()
            }
        }
    }

    private fun isLowQualityRequested(): Boolean {
        return activeProgramSet.aufloesung == FilmResolution.Enum.LOW &&
                film.lowQualityUrl.isNotEmpty()
    }

    private fun isHighQualityRequested(): Boolean {
        return activeProgramSet.aufloesung == FilmResolution.Enum.HIGH_QUALITY
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

    private fun updateSubtitleCheckbox() {
        if (!film.hasSubtitle()) {
            jCheckBoxSubtitle.setEnabled(false)
        } else {
            jCheckBoxSubtitle.setSelected(activeProgramSet.shouldDownloadSubtitle())
        }
    }

    private fun setNameFilm() {
        // beim ersten Mal werden die Standardpfade gesucht
        if (!nameGeaendert) {
            // nur wenn vom Benutzer noch nicht geändert!
            pausePathObservation {
                datenDownload = DatenDownload(
                    activeProgramSet,
                    film,
                    DatenDownload.QUELLE_DOWNLOAD,
                    null,
                    "",
                    "",
                    getFilmResolution().toString()
                )

                if (datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_DATEINAME].isEmpty()) {
                    // dann wird nicht gespeichert → eigentlich falsche Seteinstellungen?
                    jTextFieldName.apply {
                        isEnabled = false
                        text = ""
                    }
                    jComboBoxPfad.apply {
                        isEnabled = false
                        model = DefaultComboBoxModel(arrayOf(""))
                    }
                    jButtonZiel.isEnabled = false
                } else {
                    jTextFieldName.apply {
                        isEnabled = true
                        text = datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_DATEINAME]
                    }
                    jComboBoxPfad.isEnabled = true
                    jButtonZiel.isEnabled = true
                    setModelPfad(datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD], jComboBoxPfad)
                    orgPfad = datenDownload.arr[DatenDownload.DOWNLOAD_ZIEL_PFAD]
                }
            }
        }
    }

    private fun addDownloadToQueue(startAutomatically: Boolean) {
        Daten.getInstance().listeDownloads.addMitNummer(datenDownload)
        messageBus.publishAsync(DownloadListChangedEvent())

        if (startAutomatically) {
            datenDownload.startDownload()
        }
    }

    private fun getFilmSize(): String {
        return resolutionSizes.forResolution(getFilmResolution())
    }


    private fun check(): Boolean {
        val pfadRaw = jComboBoxPfad.selectedItem?.toString() ?: return false
        val name = jTextFieldName.text

        //if (datenDownload == null) return false

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
        val listener = ActionListener {
            setNameFilm()
            resetLiveInfoDisplay()
            liveInfoRequests.tryEmit(LiveInfoCommand.Cancel)
        }
        jRadioButtonAufloesungHd.addActionListener(listener)
        jRadioButtonAufloesungHd.setEnabled(film.highQualityUrl.isNotEmpty())

        jRadioButtonAufloesungKlein.addActionListener(listener)
        jRadioButtonAufloesungKlein.setEnabled(film.lowQualityUrl.isNotEmpty())

        jRadioButtonAufloesungHoch.addActionListener(listener)
        jRadioButtonAufloesungHoch.setSelected(true)
    }

    private fun setupPSetComboBox() {
        val model = DefaultComboBoxModel(listeSpeichern.getObjectDataCombo())
        jComboBoxPset.apply {
            // disable when only one entry...
            setEnabled(listeSpeichern.size > 1)
            setModel(model)
            setSelectedItem(activeProgramSet.name)
            addActionListener { applySelectedProgramSet() }
        }
    }

    private fun setupFilmHeader() {
        lblSenderIcon.setMaxIconSize(Dimension(64, 64))
        lblSenderIcon.setSender(film.sender)
        updateSenderLabelSizing()
        lblFilmTitle.text = film.title
        lblFilmTitle.toolTipText = film.title
        lblFilmThema.text = film.thema
        lblFilmThema.toolTipText = film.thema
        constrainHeaderLabelWidthForPacking(lblFilmTitle)
        constrainHeaderLabelWidthForPacking(lblFilmThema)
    }

    private fun updateSenderLabelSizing() {
        if (lblSenderIcon.icon != null) {
            val iconSize = lblSenderIcon.preferredSize
            lblSenderIcon.minimumSize = iconSize
            lblSenderIcon.preferredSize = iconSize
            lblSenderIcon.maximumSize = iconSize
            return
        }

        val preferredSize = lblSenderIcon.preferredSize
        val preferredHeight = preferredSize.height
        lblSenderIcon.minimumSize = Dimension(0, preferredHeight)
        lblSenderIcon.preferredSize = preferredSize
        lblSenderIcon.maximumSize = Dimension(Int.MAX_VALUE, preferredHeight)
    }

    private fun constrainHeaderLabelWidthForPacking(label: JLabel) {
        val preferredHeight = label.preferredSize.height
        val constrainedSize = Dimension(0, preferredHeight)
        label.minimumSize = constrainedSize
        label.preferredSize = constrainedSize
    }

    private fun setupDeleteHistoryButton() {
        jButtonDelHistory.apply {
            setText("")
            setIcon(SVGIconUtilities.createSVGIcon("icons/fontawesome/trash-can.svg"))
            addActionListener {
                MVConfig.add(MVConfig.Configs.SYSTEM_DIALOG_DOWNLOAD__PFADE_ZUM_SPEICHERN, "")
                jComboBoxPfad.setModel(DefaultComboBoxModel(arrayOf<String?>(orgPfad)))
            }
        }
    }

    private fun setupPfadSpeichernCheckBox() {
        jCheckBoxPfadSpeichern.apply {
            setSelected(appConfig.getBoolean(ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH, true))
            addActionListener {
                appConfig.setProperty(
                    ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH,
                    jCheckBoxPfadSpeichern.isSelected
                )
            }
        }
    }

    private fun detectFfprobeExecutable() {
        try {
            ffprobePath = GuiFunktionenProgramme.findExecutableOnPath("ffprobe").parent
        } catch (_: Exception) {
            logger.error("ffprobe not found on system.")
            lblBusyIndicator.apply {
                isVisible = true
                isBusy = false
                setText("Hilfsprogramm nicht gefunden!")
                setForeground(Color.RED)
            }
            btnRequestLiveInfo.setEnabled(false)
        }
    }

    private fun setupBusyIndicator() {
        lblBusyIndicator.apply {
            text = ""
            isBusy = false
            isVisible = false
        }
        showLiveInfo(LiveInfoText())
    }

    private suspend fun fetchLiveFilmInfo(resolution: FilmResolution.Enum) {
        val executablePath = ffprobePath ?: return

        btnRequestLiveInfo.isEnabled = false
        lblBusyIndicator.apply {
            isVisible = true
            isBusy = true
        }
        showLiveInfo(LiveInfoText())

        try {
            val url = film.getUrlFuerAufloesung(resolution)

            val result = runInterruptible(Dispatchers.IO) {
                FFprobe.atPath(executablePath)
                    .setShowStreams(true)
                    .setInput(url)
                    .execute()
            }

            showLiveInfo(buildLiveInfoText(result))
        } catch (_: CancellationException) {
            showLiveInfo(LiveInfoText())
        } catch (ex: JaffreeAbnormalExitException) {
            showLiveInfoError(getJaffreeErrorString(ex))
        } catch (_: Exception) {
            showLiveInfoError("Unbekannter Fehler aufgetreten.")
        } finally {
            resetBusyIndicator()
            btnRequestLiveInfo.isEnabled = true
        }
    }

    private fun buildLiveInfoText(result: FFprobeResult): LiveInfoText {
        val audioStream = result.streams.find { it.codecType == StreamType.AUDIO }
        val videoStream = result.streams.find { it.codecType == StreamType.VIDEO }

        return LiveInfoText(
            video = videoStream?.let {
                val frameRate = it.avgFrameRate.toInt()
                val codecName = getVideoCodecName(it)
                getVideoInfoString(it, frameRate, codecName)
            } ?: NO_DATA_AVAILABLE,
            audio = audioStream?.let { getAudioInfo(it, it.sampleRate) } ?: NO_DATA_AVAILABLE
        )
    }

    private fun resetBusyIndicator() {
        lblBusyIndicator.apply {
            isVisible = false
            isBusy = false
        }
    }

    private fun resetLiveInfoDisplay() {
        resetBusyIndicator()
        showLiveInfo(LiveInfoText())
    }

    private fun showLiveInfo(liveInfoText: LiveInfoText) {
        val labelForeground = UIManager.getColor(KEY_LABEL_FOREGROUND)
        lblStatus.foreground = labelForeground
        lblAudioInfo.foreground = labelForeground
        lblStatus.text = liveInfoText.video
        lblAudioInfo.text = liveInfoText.audio
    }

    private fun showLiveInfoError(message: String) {
        lblStatus.foreground = Color.RED
        lblStatus.text = message
        lblAudioInfo.foreground = UIManager.getColor(KEY_LABEL_FOREGROUND)
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

    private fun safeProcessBitRate(inBitRate: Int?): Int = (inBitRate ?: 0) / 1000

    private fun getJaffreeErrorString(ex: JaffreeAbnormalExitException): String {
        return ex.processErrorLogMessages
            .firstOrNull()
            ?.message
            ?.substringAfterLast(':')
            ?.trim()
            ?.takeIf { it.startsWith("Server returned ") }
            ?.removePrefix("Server returned ")
            ?.trim()
            ?: "Unbekannter Fehler aufgetreten."
    }

    /**
     * Calculate free disk space on volume and check if the movies can be safely downloaded.
     */
    private fun calculateAndCheckDiskSpace(usableSpaceBytes: Long) {
        UIManager.getColor(KEY_LABEL_FOREGROUND)?.let { fgColor ->
            jRadioButtonAufloesungHd.foreground = fgColor
            jRadioButtonAufloesungHoch.foreground = fgColor
            jRadioButtonAufloesungKlein.foreground = fgColor
        }

        try {
            val filmBorder = jPanelSize.border as? javax.swing.border.TitledBorder ?: return
            var usableSpace = usableSpaceBytes

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
                if (resolutionSizes.high.isNotEmpty()) {
                    val size = resolutionSizes.high.toIntOrNull() ?: 0
                    if (size > usableSpace) {
                        jRadioButtonAufloesungHd.foreground = Color.RED
                    }
                }
                if (resolutionSizes.normal.isNotEmpty()) {
                    val size = resolutionSizes.normal.toIntOrNull() ?: 0
                    if (size > usableSpace) {
                        jRadioButtonAufloesungHoch.foreground = Color.RED
                    }
                }
                if (resolutionSizes.low.isNotEmpty()) {
                    val size = resolutionSizes.low.toIntOrNull() ?: 0
                    if (size > usableSpace) {
                        jRadioButtonAufloesungKlein.foreground = Color.RED
                    }
                }
            }
        } catch (ex: Exception) {
            logger.error("calculateAndCheckDiskSpace()", ex)
        }
    }

    private suspend fun calculateAndCheckDiskSpaceAsync(currentPath: String = cbPathTextComponent.text) {
        val usableSpace = withContext(Dispatchers.IO) {
            getFreeDiskSpace(currentPath)
        }
        calculateAndCheckDiskSpace(usableSpace)
    }

    private fun setupPathEditor() {
        cbPathTextComponent = jComboBoxPfad.editor.editorComponent as JTextComponent
        cbPathTextComponent.isOpaque = true
    }

    @OptIn(FlowPreview::class)
    private fun startPathObservation() {
        uiScope.launch {
            cbPathTextComponent.textChanges()
                .debounce(250)
                .distinctUntilChanged()
                .collectLatest(::handlePathChange)
        }
    }

    private suspend fun handlePathChange(currentPath: String) {
        if (stopBeob) {
            return
        }
        nameGeaendert = true
        if (!SystemUtils.IS_OS_WINDOWS) {
            fileNameCheck(currentPath)
        }
        calculateAndCheckDiskSpaceAsync(currentPath)
    }

    private fun JTextComponent.textChanges(): Flow<String> = callbackFlow {
        val currentDocument = document
        val listener = object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent?) {
                trySend(text)
            }

            override fun removeUpdate(e: DocumentEvent?) {
                trySend(text)
            }

            override fun changedUpdate(e: DocumentEvent?) {
                trySend(text)
            }
        }

        currentDocument.addDocumentListener(listener)
        trySend(text)
        awaitClose { currentDocument.removeDocumentListener(listener) }
    }

    private fun fileNameCheck(filePath: String) {
        val editor = jComboBoxPfad.editor.editorComponent
        if (filePath != FilenameUtils.checkFilenameForIllegalCharacters(filePath, true)) {
            editor.background = MVColor.DOWNLOAD_FEHLER.color
        } else {
            editor.background = UIManager.getColor(KEY_TEXTFIELD_BACKGROUND)
        }
    }

    private fun setupNameTextField() {
        jTextFieldName.document.addDocumentListener(object : DocumentListener {

            override fun insertUpdate(e: DocumentEvent?) = handleTextChange()
            override fun removeUpdate(e: DocumentEvent?) = handleTextChange()
            override fun changedUpdate(e: DocumentEvent?) = handleTextChange()

            private fun handleTextChange() {
                if (!stopBeob) {
                    nameGeaendert = true
                    if (jTextFieldName.text != FilenameUtils.checkFilenameForIllegalCharacters(
                            jTextFieldName.text,
                            false
                        )
                    ) {
                        jTextFieldName.background = MVColor.DOWNLOAD_FEHLER.color
                    } else {
                        jTextFieldName.background = UIManager.getDefaults().getColor(KEY_TEXTFIELD_BACKGROUND)
                    }
                }
            }
        })
    }

    private fun pausePathObservation(block: () -> Unit) {
        stopBeob = true
        try {
            block()
        } finally {
            stopBeob = false
        }
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
        jButtonZiel.apply {
            icon = SVGIconUtilities.createSVGIcon("icons/fontawesome/folder-open.svg")
            text = ""
            addActionListener {
                val initialDirectory = (jComboBoxPfad.selectedItem as? String).orEmpty()
                FileDialogs.chooseDirectoryLocation(
                    MediathekGui.ui(),
                    "Film speichern",
                    initialDirectory
                )?.absolutePath?.let { selectedDirectory ->
                    SwingUtilities.invokeLater {
                        jComboBoxPfad.apply {
                            addItem(selectedDirectory)
                            selectedItem = selectedDirectory
                        }
                    }
                }
            }
        }
    }

    private fun fetchFileSizeForQuality(resolution: FilmResolution.Enum): String {
        return runCatching {
            val url = film.getUrlFuerAufloesung(resolution)
            film.getFileSizeForUrl(url, true)
        }.onFailure { logger.error("Failed to retrieve file size for $resolution", it) }
            .getOrDefault("")
    }

    private fun fetchFileSizeForNormalQuality(): String {
        return runCatching {
            film.getFileSizeForUrl(film.urlNormalQuality, true)
        }.onFailure { logger.error("Failed to retrieve normal quality size", it) }
            .getOrDefault("")
    }

    private suspend fun loadResolutionSizes(): ResolutionSizes {
        return try {
            withContext(Dispatchers.IO) {
                supervisorScope {
                    val sizes = awaitAll(
                        async { fetchFileSizeForQuality(FilmResolution.Enum.HIGH_QUALITY) },
                        async { fetchFileSizeForNormalQuality() },
                        async { fetchFileSizeForQuality(FilmResolution.Enum.LOW) }
                    )
                    ResolutionSizes(
                        high = sizes[0],
                        normal = sizes[1],
                        low = sizes[2]
                    )
                }
            }
        } catch (ex: CancellationException) {
            throw ex
        } catch (ex: Exception) {
            logger.error("Error occurred while fetching file sizes", ex)
            ResolutionSizes()
        }
    }

    private fun applyResolutionSizes(sizes: ResolutionSizes) {
        resolutionSizes = sizes
        jRadioButtonAufloesungHd.text = formatResolutionLabel(
            resolutionButtonLabels.high,
            sizes.high.takeIf { jRadioButtonAufloesungHd.isEnabled }
        )
        jRadioButtonAufloesungHoch.text = formatResolutionLabel(
            resolutionButtonLabels.normal,
            sizes.normal
        )
        jRadioButtonAufloesungKlein.text = formatResolutionLabel(
            resolutionButtonLabels.low,
            sizes.low.takeIf { jRadioButtonAufloesungKlein.isEnabled }
        )
    }

    private fun formatResolutionLabel(baseLabel: String, sizeInMb: String?): String {
        return if (sizeInMb.isNullOrEmpty()) {
            baseLabel
        } else {
            "$baseLabel   [ $sizeInMb MB ]"
        }
    }
}

private inline fun <T> withConfigLock(lockMode: LockMode, action: () -> T): T {
    val configuration = ApplicationConfiguration.getConfiguration()
    try {
        configuration.lock(lockMode)
        return action()
    } finally {
        configuration.unlock(lockMode)
    }
}

private class DialogPositionComponentListener : ComponentAdapter() {
    override fun componentMoved(e: ComponentEvent) {
        withConfigLock(LockMode.WRITE) {
            val location = e.component.location
            ApplicationConfiguration.getConfiguration().setProperty(ApplicationConfiguration.AddDownloadDialog.X, location.x)
            ApplicationConfiguration.getConfiguration().setProperty(ApplicationConfiguration.AddDownloadDialog.Y, location.y)
        }
    }
}
