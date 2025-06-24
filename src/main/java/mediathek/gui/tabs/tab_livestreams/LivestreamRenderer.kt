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

import mediathek.tool.datum.DateUtil
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import java.awt.Dimension
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit
import javax.swing.*

class LivestreamRenderer : JPanel(), ListCellRenderer<LivestreamEntry> {

    private val listCell = ListCell()
    private val senderMap = mapOf(
        "rbb Fernsehen Brandenburg" to "RBB",
        "rbb Fernsehen Berlin" to "RBB",
        "MDR Sachsen" to "MDR",
        "MDR Sachsen-Anhalt" to "MDR",
        "MDR Thüringen" to "MDR",
        "3sat" to "3Sat",
        "ARTE" to "ARTE.DE",
        "BR Nord" to "BR",
        "BR Süd" to "BR",
        "Radio Bremen" to "Radio Bremen TV",
        "NDR Hamburg" to "NDR",
        "NDR Schleswig-Holstein" to "NDR",
        "NDR Mecklenburg-Vorpommern" to "NDR",
        "NRD Niedersachsen" to "NDR",
        "Das Erste" to "ARD",
        "SWR Baden-Württemberg" to "SWR",
        "SWR Rheinland-Pfalz" to "SWR",
        "phoenix" to "PHOENIX"
    )

    private val formatter = DateTimeFormatter.ofPattern("HH:mm").withZone(DateUtil.MV_DEFAULT_TIMEZONE)

    init {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(5, 5, 5, 5)
        minimumSize = Dimension(100, 200)

        add(listCell, BorderLayout.CENTER)
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

        if (list.width <= 0 || list.height <= 0) {
            listCell.lblSender.text = ""
            listCell.lblTitle.text = ""
            listCell.lblSubtitle.text = ""
            listCell.lblZeitraum.text = ""
            listCell.progressBar.value = 0
            return this
        }

        val sanitized = sanitizeName(value.streamName)
        val senderName = senderMap[sanitized] ?: sanitized
        listCell.lblSender.setSender(senderName)

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

            val total = (show.endTime.epochSecond - show.startTime.epochSecond).coerceAtLeast(1)
            val elapsed = Instant.now().epochSecond - show.startTime.epochSecond
            val remaining = show.endTime.epochSecond - Instant.now().epochSecond

            listCell.progressBar.maximum = total.toInt()
            listCell.progressBar.value = elapsed.coerceAtLeast(0).toInt()

            if (remaining in 1..WARNUNG_RESTZEIT_SEKUNDEN) {
                listCell.progressBar.foreground = Color(255, 140, 0) // Orange
                listCell.lblZeitraum.foreground = Color(255, 140, 0)
            } else {
                listCell.progressBar.foreground = UIManager.getColor("ProgressBar.foreground")
                listCell.lblZeitraum.foreground = foreground
            }

        } else {
            listCell.lblTitle.text = "Keine Sendung oder außerhalb des Zeitraums"
            listCell.lblSubtitle.text = ""
            listCell.lblZeitraum.text = ""
            listCell.progressBar.maximum = 100
            listCell.progressBar.minimum = 0
            listCell.progressBar.value = 0
        }

        background = if (isSelected) list.selectionBackground else list.background
        foreground = if (isSelected) list.selectionForeground else list.foreground

        listCell.lblSender.foreground = foreground
        listCell.lblTitle.foreground = foreground
        listCell.lblSubtitle.foreground = foreground
        listCell.lblZeitraum.foreground = foreground

        return this
    }

    private val WARNUNG_RESTZEIT_SEKUNDEN = TimeUnit.SECONDS.convert(5, TimeUnit.MINUTES)
}
