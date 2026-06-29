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
}
