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

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import retrofit2.Retrofit
import retrofit2.converter.jackson.JacksonConverterFactory
import java.awt.BorderLayout
import javax.swing.*

class LivestreamsPanel : JPanel(), CoroutineScope by MainScope() {
    private val listModel = StreamListModel()
    private val list = JList(listModel)
    private val reloadButton = JButton("Aktualisieren")

    private val service: StreamService

    init {
        layout = BorderLayout()

        list.cellRenderer = StreamListCellRenderer()
        add(JScrollPane(list), BorderLayout.CENTER)
        add(reloadButton, BorderLayout.SOUTH)

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.zapp.mediathekview.de/") // Anpassen!
            .addConverterFactory(JacksonConverterFactory.create())
            .build()

        service = retrofit.create(StreamService::class.java)
        reloadButton.addActionListener { ladeDaten() }

        ladeDaten()
    }

    private fun ladeDaten() {
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
                withContext(Dispatchers.Swing) {
                    ex.printStackTrace()
                    JOptionPane.showMessageDialog(null, "Fehler: ${ex.message}")
                    reloadButton.isEnabled = true
                }
            }
        }
    }
}