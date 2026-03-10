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

package mediathek.swingaudiothek.ui

import mediathek.swingaudiothek.model.AudioEntry
import org.jdesktop.swingx.JXHyperlink
import java.awt.BorderLayout
import java.awt.Desktop
import java.awt.Font
import java.net.URI
import java.time.format.DateTimeFormatter
import javax.swing.*

class AudiothekDetailsPanel : JPanel(BorderLayout(0, 8)) {
    private val detailsTitle = JLabel("Kein Eintrag ausgewählt")
    private val detailsMeta = JLabel(" ")
    private val websiteLink = JXHyperlink().apply {
        text = "Link zur Webseite"
        isVisible = false
    }
    private val descriptionArea = JTextArea()
    private var websiteUrl: URI? = null

    init {
        border = BorderFactory.createTitledBorder("Details")
        detailsTitle.font = detailsTitle.font.deriveFont(Font.BOLD, 16f)

        descriptionArea.lineWrap = true
        descriptionArea.wrapStyleWord = true
        descriptionArea.isEditable = false
        websiteLink.addActionListener {
            websiteUrl?.let(::openExternal)
        }

        val detailHeader = JPanel(BorderLayout(0, 6))
        detailHeader.add(detailsTitle, BorderLayout.NORTH)
        detailHeader.add(detailsMeta, BorderLayout.CENTER)
        detailHeader.add(websiteLink, BorderLayout.SOUTH)

        add(detailHeader, BorderLayout.NORTH)
        add(JScrollPane(descriptionArea), BorderLayout.CENTER)
    }

    fun showEntry(entry: AudioEntry?) {
        if (entry == null) {
            detailsTitle.text = "Kein Eintrag ausgewählt"
            detailsMeta.text = " "
            websiteUrl = null
            websiteLink.isClicked = false
            websiteLink.isVisible = false
            descriptionArea.text = ""
            return
        }

        val published = entry.publishedAt?.format(DETAIL_DATE_FORMAT)
        detailsTitle.text = entry.title.ifBlank { "(ohne Titel)" }
        detailsMeta.text = buildString {
            append(entry.channel.ifBlank { "Unbekannter Sender" })
            if (entry.theme.isNotBlank()) {
                append(" | ").append(entry.theme)
            }
            if (published != null) {
                append(" | ").append(published)
            }
            if (entry.durationMinutes != null) {
                append(" | ").append(entry.durationMinutes).append(" Min")
            }
            if (entry.isNew) {
                append(" | neu")
            }
            if (entry.isPodcast) {
                append(" | Podcast")
            }
            if (entry.isDuplicate) {
                append(" | doppelt")
            }
        }
        websiteUrl = entry.websiteUrl
        websiteLink.isClicked = false
        websiteLink.isVisible = websiteUrl != null
        descriptionArea.text = entry.description.ifBlank { "Keine Beschreibung vorhanden." }
    }

    private fun openExternal(url: URI) {
        runCatching {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(url)
            }
        }
    }

    companion object {
        private val DETAIL_DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")
    }
}
