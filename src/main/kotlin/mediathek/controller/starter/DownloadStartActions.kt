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

package mediathek.controller.starter

import mediathek.config.Daten
import mediathek.controller.history.SeenHistoryController
import mediathek.daten.DatenDownload
import mediathek.gui.messages.StartEvent
import mediathek.tool.MessageBus

object DownloadStartActions {
    fun start(daten: Daten, download: DatenDownload) {
        startAll(daten, listOf(download))
    }

    fun startAll(daten: Daten, downloads: Iterable<DatenDownload>) {
        SeenHistoryController(daten.listeBookmarkList).use { historyController ->
            for (download in downloads) {
                download.runtime.startRun()
                historyController.markSeen(download.film)
            }
        }

        MessageBus.messageBus.publishAsync(StartEvent())
    }
}
