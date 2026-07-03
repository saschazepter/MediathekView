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

package mediathek.mainwindow

import kotlinx.coroutines.*
import mediathek.config.StandardLocations
import mediathek.config.application.ApplicationConfiguration
import mediathek.daten.ListeFilme
import mediathek.filmlisten.FilmCatalog
import mediathek.filmlisten.reader.FilmListReader
import mediathek.gui.messages.FilmListReadStartEvent
import mediathek.gui.messages.FilmListReadStopEvent
import mediathek.tool.MessageBus
import org.apache.logging.log4j.LogManager

class StartupFilmlistPreload private constructor(
    private val scope: CoroutineScope,
    private val job: Deferred<Result<Unit>>,
) : AutoCloseable {
    suspend fun await(): Result<Unit> = job.await()

    override fun close() {
        scope.cancel()
    }

    companion object {
        private val logger = LogManager.getLogger(StartupFilmlistPreload::class.java)

        fun start(filmCatalog: FilmCatalog): StartupFilmlistPreload {
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
            val job = scope.async {
                runCatching {
                    readLocalFilmlist(filmCatalog)
                }.onFailure { exception ->
                    logger.error("Early startup filmlist preload failed", exception)
                }
            }
            return StartupFilmlistPreload(scope, job)
        }

        internal fun readLocalFilmlist(filmCatalog: FilmCatalog) {
            logger.trace("Reading local filmlist")
            MessageBus.messageBus.publishAsync(FilmListReadStartEvent())
            try {
                val loadedFilms = ListeFilme()
                FilmListReader().use { reader ->
                    val loadNumDays = ApplicationConfiguration.getInstance().filmListLoadNumDays
                    reader.readFilmListe(StandardLocations.getFilmlistFilePathString(), loadedFilms, loadNumDays)
                }
                synchronized(filmCatalog.allFilms) {
                    filmCatalog.allFilms.clear()
                    filmCatalog.allFilms.addAll(loadedFilms)
                    filmCatalog.allFilms.metaData = loadedFilms.metaData
                }
            } finally {
                MessageBus.messageBus.publishAsync(FilmListReadStopEvent())
            }
        }
    }
}
