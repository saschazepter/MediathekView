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

package mediathek.config

import mediathek.controller.starter.DownloadServices
import mediathek.daten.ProgramSetRepository
import mediathek.daten.abo.AboServices
import mediathek.daten.blacklist.BlacklistServices
import mediathek.filmlisten.FilmCatalog
import mediathek.filmlisten.FilmeLaden
import mediathek.gui.bookmark.BookmarkServices

class Daten {
    val programSets: ProgramSetRepository = ProgramSetRepository()
    val filmCatalog: FilmCatalog = FilmCatalog()
    val filmListLoader: FilmeLaden = FilmeLaden(this)
    val downloads: DownloadServices = DownloadServices(this)
    val blacklist: BlacklistServices = BlacklistServices(filmCatalog)
    val bookmarks: BookmarkServices = BookmarkServices(filmCatalog.allFilms)
    val abos: AboServices = AboServices(filmCatalog.allFilms)

    val configurationPersistence: DatenConfigurationPersistence = DatenConfigurationPersistence(
        programSets = programSets,
        downloads = downloads,
        blacklist = blacklist,
        abos = abos,
        bookmarks = bookmarks,
    )
}
