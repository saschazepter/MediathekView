package mediathek.gui.bookmark

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.ObservableElementList
import ca.odell.glazedlists.SortedList
import ca.odell.glazedlists.impl.beans.BeanTableFormat
import ca.odell.glazedlists.swing.GlazedListsSwing
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BookmarkTablePipelineTest {
    @Test
    fun `existing bookmarks are present when table pipeline is created`() {
        val source = BasicEventList<BookmarkData>().apply {
            repeat(6) { add(BookmarkData()) }
        }
        val observed = ObservableElementList(source, GlazedLists.observableConnector())
        val sorted = SortedList(observed, BookmarkAddedAtComparator())
        val format = BeanTableFormat(
            BookmarkData::class.java,
            arrayOf("seen"),
            arrayOf("Gesehen"),
        )
        val model = GlazedListsSwing.eventTableModelWithThreadProxyList(sorted, format)

        assertEquals(6, source.size)
        assertEquals(6, observed.size)
        assertEquals(6, sorted.size)
        assertEquals(6, model.rowCount)

        model.dispose()
        sorted.dispose()
        observed.dispose()
    }
}
