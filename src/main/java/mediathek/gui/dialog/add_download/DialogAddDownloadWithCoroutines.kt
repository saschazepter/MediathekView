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
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
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

    init {
        // zusätzlichen ActionListener für Coroutines installieren
        btnRequestLiveInfo.addActionListener { fetchLiveFilmInfoCoroutine() }
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
            } catch (ex: CancellationException) {
                clearInfo()
            } catch (ex: Exception) {
                showError("Fehler: ${ex.message ?: "Unbekannt"}")
            } finally {
                resetBusyLabelAndButton()
            }
        }
    }

    private fun processFFprobeResult(result: FFprobeResult) {
        val audioStream = result.streams.find { it.codecType == StreamType.AUDIO }
        val videoStream = result.streams.find { it.codecType == StreamType.VIDEO }

        lblAudioInfo.foreground = UIManager.getColor(KEY_LABEL_FOREGROUND)
        lblAudioInfo.text = audioStream?.codecLongName ?: NO_DATA_AVAILABLE

        lblStatus.foreground = UIManager.getColor(KEY_LABEL_FOREGROUND)
        lblStatus.text = videoStream?.let { stream ->
            "Video: ${stream.width}x${stream.height}, ${stream.codecLongName}"
        } ?: NO_DATA_AVAILABLE
    }

    private fun clearInfo() {
        lblStatus.text = ""
        lblAudioInfo.text = ""
    }

    private fun showError(msg: String) {
        lblStatus.foreground = Color.RED
        lblStatus.text = msg
        lblAudioInfo.text = ""
    }

    override fun dispose() {
        coroutineScope.cancel()
        super.dispose()
    }
}