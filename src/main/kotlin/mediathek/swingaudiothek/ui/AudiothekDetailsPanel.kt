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
            websiteLink.setClicked(false)
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
        websiteLink.setClicked(false)
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
