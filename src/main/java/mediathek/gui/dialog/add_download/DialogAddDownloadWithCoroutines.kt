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
import mediathek.config.MVConfig
import mediathek.tool.ApplicationConfiguration
import mediathek.tool.EscapeKeyHandler
import mediathek.tool.GuiFunktionenProgramme
import mediathek.tool.SVGIconUtilities
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
import javax.swing.DefaultComboBoxModel
import javax.swing.UIManager
import kotlin.coroutines.cancellation.CancellationException
import kotlin.math.max

class DialogAddDownloadWithCoroutines(
    parent: Frame,
    film: mediathek.daten.DatenFilm,
    pSet: mediathek.daten.DatenPset?,
    requestedResolution: java.util.Optional<mediathek.daten.FilmResolution.Enum>
) : DialogAddDownload(parent, film, pSet, requestedResolution) {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    companion object {
        private val logger = LogManager.getLogger()
        private const val NO_DATA_AVAILABLE: String = "Keine Daten verfügbar."

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

        btnRequestLiveInfo.addActionListener { fetchLiveFilmInfoCoroutine() }

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

    private fun setupFilmQualityRadioButtons() {
        val listener: ActionListener? = ActionListener {
            setNameFilm()
            lblStatus.setText("")
            lblAudioInfo.setText("")
            lblBusyIndicator.setBusy(false)
            lblBusyIndicator.isVisible = false
            coroutineScope.cancel()
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
        jCheckBoxPfadSpeichern.addActionListener(ActionListener { `_`: ActionEvent? ->
            config.setProperty(
                ApplicationConfiguration.DOWNLOAD_SHOW_LAST_USED_PATH,
                jCheckBoxPfadSpeichern.isSelected()
            )
        })
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

    private fun fetchLiveFilmInfoCoroutine() {
        btnRequestLiveInfo.isEnabled = false
        lblBusyIndicator.isVisible = true
        lblBusyIndicator.isBusy = true
        lblStatus.text = ""
        lblAudioInfo.text = ""

        coroutineScope.launch {
            try {
                val url = film.getUrlFuerAufloesung(filmResolution)

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
}