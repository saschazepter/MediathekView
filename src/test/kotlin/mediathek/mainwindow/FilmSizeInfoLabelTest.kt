package mediathek.mainwindow

import mediathek.daten.DatenFilm
import mediathek.filmlisten.FilmCatalog
import mediathek.gui.messages.FilmTableRowCountChangedEvent
import mediathek.tool.MessageBus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import javax.swing.SwingUtilities

internal class FilmSizeInfoLabelTest {
    @Test
    fun `committed filtered count updates label independently of supplier timing`() {
        val catalog = FilmCatalog().apply {
            repeat(10) { allFilms.add(DatenFilm()) }
        }
        val label = FilmSizeInfoLabel(catalog) { 10 }

        label.updateDisplayedFilmCount(3)

        assertEquals("3 Filme (Insgesamt: 10)", label.text)
    }

    @Test
    fun `committed row count event updates the subscribed label`() {
        val catalog = FilmCatalog().apply {
            repeat(10) { allFilms.add(DatenFilm()) }
        }
        val label = FilmSizeInfoLabel(catalog) { 10 }

        try {
            onEdt { label.addNotify() }

            MessageBus.messageBus.publish(FilmTableRowCountChangedEvent(4))
            onEdt { }

            assertEquals("4 Filme (Insgesamt: 10)", label.text)
        } finally {
            onEdt { label.removeNotify() }
        }
    }

    private fun onEdt(action: () -> Unit) {
        if (SwingUtilities.isEventDispatchThread()) {
            action()
        } else {
            SwingUtilities.invokeAndWait(action)
        }
    }
}
