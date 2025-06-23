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

package mediathek.gui.tabs.tab_livestreams

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import mediathek.gui.tabs.tab_livestreams.services.ShowService
import mediathek.gui.tabs.tab_livestreams.services.StreamService
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.awt.BorderLayout
import java.awt.Desktop
import java.time.Instant
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.JScrollPane
import javax.swing.Timer

class LivestreamPanel : JPanel(BorderLayout()), CoroutineScope by MainScope() {

    private val listModel = LivestreamListModel()
    private val list = JList(listModel)
    private val streamService: StreamService
    private val showService: ShowService

    private val refreshTimer = Timer(10000) { checkForExpiredShows() } // alle 10s prüfen

    init {
        val mapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        streamService = Retrofit.Builder()
            .baseUrl("https://api.zapp.mediathekview.de/")
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build()
            .create(StreamService::class.java)

        showService = Retrofit.Builder()
            .baseUrl("https://api.zapp.mediathekview.de/")
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build()
            .create(ShowService::class.java)

        list.cellRenderer = LivestreamRenderer()
        list.addMouseListener(object : java.awt.event.MouseAdapter() {
            override fun mouseClicked(e: java.awt.event.MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = list.selectedValue ?: return
                    Desktop.getDesktop().browse(java.net.URI(selected.streamUrl))
                }
            }
        })

        add(JScrollPane(list), BorderLayout.CENTER)

        ladeLivestreams()

        refreshTimer.start()
    }

    private fun ladeLivestreams() {
        launch(Dispatchers.IO) {
            try {
                val streams = streamService.getStreams()
                val entries = streams.map { (key, info) ->
                    LivestreamEntry(key, info.name, info.streamUrl)
                }
                withContext(Dispatchers.Swing) {
                    listModel.setData(entries)
                    ladeShowsFuerAlle()
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun ladeShowsFuerAlle() {
        for (i in 0 until listModel.size) {
            val entry = listModel.getElementAt(i)
            ladeShowFuerEintrag(entry, i)
        }
    }

    private fun ladeShowFuerEintrag(entry: LivestreamEntry, index: Int) {
        launch(Dispatchers.IO) {
            try {
                val showResponse = showService.getShow(entry.key)
                val aktuelleShow = showResponse.shows.firstOrNull()
                if (aktuelleShow != null) {
                    entry.show = aktuelleShow
                    withContext(Dispatchers.Swing) {
                        listModel.updateEntry(index, entry)
                    }
                }
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }
    }

    private fun checkForExpiredShows() {
        val now = Instant.now()
        for (i in 0 until listModel.size) {
            val entry = listModel.getElementAt(i)
            if (entry.show?.endTime?.isBefore(now) == true) {
                ladeShowFuerEintrag(entry, i)
            } else {
                listModel.updateEntry(i, entry) // Fortschritt aktualisieren
            }
        }
    }
}
