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
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URI
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.*

class LivestreamsPanel : JPanel(), CoroutineScope by MainScope() {
    private val listModel = StreamListModel()
    private val list = JList(listModel)
    private val reloadButton = JButton("Aktualisieren")

    private val service: StreamService
    private val showService: ShowService
    private val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm").withZone(ZoneId.systemDefault())

    init {
        layout = BorderLayout()

        list.cellRenderer = StreamListCellRenderer()
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = list.selectedValue ?: return
                    Desktop.getDesktop().browse(URI(selected.streamUrl))
                }
            }
        })
        add(JScrollPane(list), BorderLayout.CENTER)
        add(reloadButton, BorderLayout.SOUTH)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.zapp.mediathekview.de/") // Anpassen!
            .addConverterFactory(JacksonConverterFactory.create())
            .build()

        service = retrofit.create(StreamService::class.java)

        val mapper = ObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        showService = Retrofit.Builder()
            .baseUrl("https://api.zapp.mediathekview.de/")
            .addConverterFactory(JacksonConverterFactory.create(mapper))
            .build()
            .create(ShowService::class.java)

        reloadButton.addActionListener { ladeAktuelleSendung("das_erste") }

        ladeDaten()
    }

    companion object {
        private val LOG: Logger = LogManager.getLogger()
    }

    fun ladeDaten() {
        reloadButton.isEnabled = false
        listModel.clear()

        launch(Dispatchers.IO) {
            try {
                val result = service.getStreams()
                withContext(Dispatchers.Swing) {
                    listModel.setData(result.values.toList())
                    reloadButton.isEnabled = true
                }
            } catch (ex: Exception) {
                LOG.error("Failed to load Livestreams tab", ex)
            }
        }
    }

    private fun ladeAktuelleSendung(key: String) {
        reloadButton.isEnabled = false

        launch(Dispatchers.IO) {
            try {
                val response = showService.getShow(key)
                val ersteSendung = response.shows.firstOrNull()

                withContext(Dispatchers.Swing) {
                    reloadButton.isEnabled = true
                    if (ersteSendung != null) {
                        JOptionPane.showMessageDialog(
                            this@LivestreamsPanel,
                            """
                            Aktuelle Sendung:
                            Titel: ${ersteSendung.title}
                            Untertitel: ${ersteSendung.subtitle}
                            Beschreibung: ${ersteSendung.description}
                            Start: ${formatter.format(ersteSendung.startTime)}
                            Ende: ${formatter.format(ersteSendung.endTime)}
                            """.trimIndent()
                        )
                    } else {
                        JOptionPane.showMessageDialog(this@LivestreamsPanel, "Keine Sendung gefunden.")
                    }
                }
            } catch (ex: Exception) {
                withContext(Dispatchers.Swing) {
                    reloadButton.isEnabled = true
                    JOptionPane.showMessageDialog(
                        this@LivestreamsPanel,
                        "Fehler beim Abrufen: ${ex.localizedMessage}"
                    )
                }
            }
        }
    }
}