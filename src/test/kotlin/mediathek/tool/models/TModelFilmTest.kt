package mediathek.tool.models

import mediathek.daten.DatenFilm
import mediathek.tool.datum.DatumFilm
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import javax.swing.event.TableModelEvent

internal class TModelFilmTest {
    @Test
    fun columnDescriptorsPreserveFilmTableMetadataAndValues() {
        val film = DatenFilm().apply {
            sender = "ARD"
            thema = "Thema"
            title = "Titel"
            urlNormalQuality = "https://example.invalid/video.mp4"
            setFilmLengthSeconds(42)
            setFileSize("17")
        }
        val model = TModelFilm()
        model.addAll(listOf(film))

        assertEquals(15, model.columnCount)
        assertEquals("Sender", model.getColumnName(FilmColumn.SENDER.index))
        assertEquals("Größe [MB]", model.getColumnName(FilmColumn.SIZE.index))
        assertEquals(Int::class.javaObjectType, model.getColumnClass(FilmColumn.DURATION.index))
        assertEquals(DatumFilm::class.java, model.getColumnClass(FilmColumn.DATE.index))
        assertEquals(Boolean::class.javaObjectType, model.getColumnClass(FilmColumn.HIGH_QUALITY.index))

        assertEquals("ARD", model.getValueAt(0, FilmColumn.SENDER.index))
        assertEquals(42, model.getValueAt(0, FilmColumn.DURATION.index))
        assertEquals(17, model.getValueAt(0, FilmColumn.SIZE.index))
        assertSame(film, model.getValueAt(0, FilmColumn.REF.index))
    }

    @Test
    fun addAllFiresInsertedRowsForActualInsertedRange() {
        val model = TModelFilm()
        val events = mutableListOf<TableModelEvent>()
        model.addTableModelListener { event -> events += event }

        model.addAll(listOf(DatenFilm(), DatenFilm()))

        val event = events.single()
        assertEquals(TableModelEvent.INSERT, event.type)
        assertEquals(0, event.firstRow)
        assertEquals(1, event.lastRow)
    }

    @Test
    fun addAllWithEmptyListDoesNotFireTableEvent() {
        val model = TModelFilm()
        val events = mutableListOf<TableModelEvent>()
        model.addTableModelListener { event -> events += event }

        model.addAll(emptyList())

        assertTrue(events.isEmpty())
    }

    @Test
    fun removeFilmsDeletesContiguousFilmsWithOneTableEvent() {
        val films = List(4) { DatenFilm() }
        val model = TModelFilm().apply { addAll(films) }
        val events = mutableListOf<TableModelEvent>()
        model.addTableModelListener { event -> events += event }

        model.removeFilms(films.subList(1, 3))

        assertEquals(2, model.rowCount)
        assertSame(films[0], model.getValueAt(0, FilmColumn.REF.index))
        assertSame(films[3], model.getValueAt(1, FilmColumn.REF.index))
        val event = events.single()
        assertEquals(TableModelEvent.DELETE, event.type)
        assertEquals(1, event.firstRow)
        assertEquals(2, event.lastRow)
    }

    @Test
    fun removeFilmsDeletesSeparatedFilmsFromHighestRowFirst() {
        val films = List(5) { DatenFilm() }
        val model = TModelFilm().apply { addAll(films) }
        val events = mutableListOf<TableModelEvent>()
        model.addTableModelListener { event -> events += event }

        model.removeFilms(listOf(films[1], films[3]))

        assertEquals(3, model.rowCount)
        assertEquals(listOf(3, 1), events.map { it.firstRow })
        assertTrue(events.all { it.type == TableModelEvent.DELETE && it.firstRow == it.lastRow })
    }
}
