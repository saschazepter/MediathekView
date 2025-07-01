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
import mediathek.tool.EscapeKeyHandler
import org.apache.logging.log4j.LogManager
import java.awt.Color
import java.awt.Frame
import javax.swing.UIManager
import kotlin.coroutines.cancellation.CancellationException

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
        EscapeKeyHandler.installHandler(this, java.lang.Runnable { this.dispose() })

        setupUI()

        setupMinimumSizeForOs()
        restoreWindowSizeFromConfig() //only install on windows and linux, macOS works...
        installMinResizePreventer()

        setLocationRelativeTo(parent)

        addComponentListener(DialogPositionComponentListener())

        btnRequestLiveInfo.addActionListener { fetchLiveFilmInfoCoroutine() }
    }


    override fun dispose() {
        coroutineScope.cancel()
        super.dispose()
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