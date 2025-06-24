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

import mediathek.gui.tabs.SenderIconLabel
import mediathek.tool.datum.DateUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JPanel
import javax.swing.ListCellRenderer

class LivestreamRenderer : JPanel(), ListCellRenderer<LivestreamEntry> {

    private val lblSender = SenderIconLabel()
    private val listCell = ListCell()
    private val senderMap = mutableMapOf<String, String>()
    private val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(DateUtil.MV_DEFAULT_TIMEZONE)

    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        minimumSize = Dimension(100, 200)

        initComponents()
        setupSenderMap()
    }

    private fun initComponents() {
        val hPanel = JPanel()
        hPanel.isOpaque = false
        hPanel.layout = FlowLayout(FlowLayout.LEFT)

        hPanel.add(lblSender)
        add(listCell, BorderLayout.CENTER)
    }

    private fun setupSenderMap() {
        senderMap.put("rbb Fernsehen Brandenburg", "RBB")
        senderMap.put("rbb Fernsehen Berlin", "RBB")
        senderMap.put("MDR Sachsen", "MDR")
        senderMap.put("MDR Sachsen-Anhalt", "MDR")
        senderMap.put("MDR Thüringen", "MDR")
        senderMap.put("3sat", "3Sat")
        senderMap.put("ARTE", "ARTE.DE")
        senderMap.put("BR Nord", "BR")
        senderMap.put("BR Süd", "BR")
        senderMap.put("Radio Bremen", "Radio Bremen TV")
        senderMap.put("NDR Hamburg", "NDR")
        senderMap.put("NDR Schleswig-Holstein", "NDR")
        senderMap.put("NDR Mecklenburg-Vorpommern", "NDR")
        senderMap.put("NRD Niedersachsen", "NDR")
        senderMap.put("Das Erste", "ARD")
        senderMap.put("SWR Baden-Württemberg", "SWR")
        senderMap.put("SWR Rheinland-Pfalz", "SWR")
        senderMap.put("phoenix", "PHOENIX")
    }

    /**
     * Remove soft hyphens and control characters from string.
     */
    private fun sanitizeName(name: String): String {
        return name.replace(Regex("[\\u00AD\\p{Cf}]"), "")
    }

    override fun getListCellRendererComponent(
        list: JList<out LivestreamEntry>,
        value: LivestreamEntry,
        index: Int,
        isSelected: Boolean,
        cellHasFocus: Boolean
    ): Component {

        var senderName = sanitizeName(value.streamName)
        val senderMapName = senderMap[senderName]
        if (senderMapName != null) {
            senderName = senderMapName
        }

        lblSender.setSender(senderName)
        val show = value.show

        if (show != null && show.startTime.isBefore(Instant.now()) && show.endTime.isAfter(Instant.now())) {
            if (show.subtitle != null) {
                listCell.lblTitle.text = show.title
                listCell.lblSubtitle.text = show.subtitle
            } else {
                listCell.lblTitle.text = show.title
                listCell.lblSubtitle.text = ""
            }
            val zeitraum = formatter.format(show.startTime) + " - " + formatter.format(show.endTime)
            listCell.lblZeitraum.text = zeitraum

            val total = show.endTime.epochSecond - show.startTime.epochSecond
            val elapsed = Instant.now().epochSecond - show.startTime.epochSecond
            listCell.progressBar.maximum = total.toInt()
            listCell.progressBar.value = elapsed.toInt()
        } else {
            listCell.lblTitle.text = "Keine Sendung oder außerhalb des Zeitraums"
            listCell.lblSubtitle.text = ""
            listCell.lblZeitraum.text = ""
            listCell.progressBar.maximum = 100
            listCell.progressBar.value = 0
        }

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground

        lblSender.foreground = foreground
        listCell.lblTitle.foreground = foreground
        listCell.lblSubtitle.foreground = foreground
        listCell.lblZeitraum.foreground = foreground

        return this
    }
}
