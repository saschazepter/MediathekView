/*
 * MediathekView
 * Copyright (C) 2008 W. Xaver
 * W.Xaver[at]googlemail.com
 * http://zdfmediathk.sourceforge.net/
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package mediathek.daten

import mediathek.config.Konstanten
import mediathek.config.application.ApplicationConfiguration
import mediathek.controller.starter.DownloadLifecycleActions
import mediathek.controller.starter.DownloadStartActions
import mediathek.controller.starter.StartStatus
import mediathek.gui.messages.ButtonStartEvent
import mediathek.tool.MessageBus
import mediathek.tool.models.TModelDownload
import java.util.*

class ListeDownloads : LinkedList<DatenDownload>() {
    @Synchronized
    fun addMitNummer(download: DatenDownload) {
        add(download)
        listeNummerieren()
    }

    @Synchronized
    fun getDownloadUrlFilm(urlFilm: String): DatenDownload? =
        firstOrNull { download -> download.filmUrl == urlFilm }

    @Synchronized
    fun getModel(tModel: TModelDownload, filter: DownloadListFilter) {
        DownloadTableModelUpdater.reload(tModel, this, filter)
    }

    @Synchronized
    fun setModelProgress(tModel: TModelDownload) {
        DownloadTableModelUpdater.updateProgress(tModel)
    }

    @Synchronized
    fun listeNummerieren() {
        var index = 1
        for (download in this) {
            download.nr = index++
        }
    }

    @Synchronized
    fun buttonStartsPutzen() {
        // Starts durch Button die fertig sind, löschen
        var found = false
        val iterator = iterator()
        while (iterator.hasNext()) {
            val download = iterator.next()
            if (download.runtime.runState?.isAtLeastFinished == true && download.quelle == DownloadSource.BUTTON) {
                // dann ist er fertig oder abgebrochen
                iterator.remove()
                found = true
            }
        }
        if (found) {
            MessageBus.messageBus.publishAsync(ButtonStartEvent())
        }
    }

    @get:Synchronized
    val nextStart: DatenDownload?
        get() {
            // get: erstes passendes Element der Liste zurückgeben oder null
            // und versuchen dass bei mehreren laufenden Downloads ein anderer Sender gesucht wird
            val maxNumDownloads = ApplicationConfiguration.getInstance().maxSimultaneousDownloads
            if (isNotEmpty() && canStartMore(maxNumDownloads)) {
                return nextPossibleDownload()
            }

            return null
        }

    @get:Synchronized
    val restartDownload: DatenDownload?
        get() {
            // Versuch einen Fehlgeschlagenen Download zu finden um ihn wieder zu starten
            // die Fehler laufen aber einzeln, vorsichtshalber
            if (!canStartMore(1)) {
                return null
            }
            for (download in this) {
                val state = download.runtime.runState ?: continue

                if (state.status == StartStatus.ERROR && state.countRestarted < Konstanten.MAX_DOWNLOAD_RESTARTS) {
                    val restarted = state.countRestarted
                    if (download.art == DownloadType.DIRECT) {
                        DownloadLifecycleActions.reset(download)
                        DownloadStartActions.start(download)
                        download.runtime.runState?.countRestarted = restarted + 1
                        return download
                    }
                }
            }
            return null
        }

    private fun canStartMore(max: Int): Boolean {
        var count = 0
        for (download in this) {
            if (download.runtime.runState?.isRunning == true) {
                ++count
                if (count >= max) {
                    return false
                }
            }
        }
        return true
    }

    private fun nextPossibleDownload(): DatenDownload? =
        firstOrNull { download -> download.runtime.runState?.status == StartStatus.INITIALIZED }
}
