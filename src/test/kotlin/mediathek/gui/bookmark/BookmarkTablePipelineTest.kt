package mediathek.gui.bookmark

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.ObservableElementList
import ca.odell.glazedlists.SortedList
import ca.odell.glazedlists.impl.beans.BeanTableFormat
import ca.odell.glazedlists.swing.DefaultEventSelectionModel
import ca.odell.glazedlists.swing.GlazedListsSwing
import mediathek.tool.withWriteLock
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import javax.swing.SwingUtilities

class BookmarkTablePipelineTest {
    @Test
    fun `existing bookmarks are present when table pipeline is created`() {
        val source = BasicEventList<BookmarkData>().apply {
            repeat(6) { add(BookmarkData()) }
        }
        val pipeline = BookmarkTablePipeline(source)
        val observed = pipeline.observedBookmarks
        val sorted = pipeline.sortedBookmarks
        val format = BeanTableFormat(
            BookmarkData::class.java,
            arrayOf("seen"),
            arrayOf("Gesehen"),
        )
        val swingBookmarks = GlazedListsSwing.swingThreadProxyList(sorted)
        val model = GlazedListsSwing.eventTableModel(swingBookmarks, format)

        assertEquals(6, source.size)
        assertEquals(6, observed.size)
        assertEquals(6, sorted.size)
        assertEquals(6, model.rowCount)

        model.dispose()
        swingBookmarks.dispose()
        sorted.dispose()
        observed.dispose()
        source.dispose()
    }

    @Test
    fun `shared Swing proxy keeps selection updates on the EDT`() {
        val first = BookmarkData().apply { bookmarkAdded = LocalDate.of(2026, 1, 2) }
        val selectedBookmark = BookmarkData().apply { bookmarkAdded = LocalDate.of(2026, 1, 3) }
        val source = BasicEventList<BookmarkData>().apply { addAll(listOf(first, selectedBookmark)) }
        val observed = ObservableElementList(source, GlazedLists.observableConnector())
        val sorted = SortedList(observed, BookmarkAddedAtComparator())
        val format = BeanTableFormat(
            BookmarkData::class.java,
            arrayOf("seen"),
            arrayOf("Gesehen"),
        )
        val swingBookmarks = GlazedListsSwing.swingThreadProxyList(sorted)
        val model = GlazedListsSwing.eventTableModel(swingBookmarks, format)
        val selectionModel = DefaultEventSelectionModel(swingBookmarks)
        val selectionChanged = CountDownLatch(1)
        val selectionChangedOnEdt = AtomicBoolean()

        SwingUtilities.invokeAndWait {
            selectionModel.setSelectionInterval(1, 1)
            selectionModel.addListSelectionListener { event ->
                if (!event.valueIsAdjusting) {
                    selectionChangedOnEdt.set(SwingUtilities.isEventDispatchThread())
                    selectionChanged.countDown()
                }
            }
        }

        try {
            Thread.ofVirtual().start {
                source.withWriteLock {
                    add(BookmarkData().apply { bookmarkAdded = LocalDate.of(2026, 1, 1) })
                }
            }.join()

            assertTrue(selectionChanged.await(5, TimeUnit.SECONDS))
            assertTrue(selectionChangedOnEdt.get())
            SwingUtilities.invokeAndWait {
                assertEquals(3, model.rowCount)
                assertSame(selectedBookmark, selectionModel.selected.single())
            }
        } finally {
            SwingUtilities.invokeAndWait {
                selectionModel.dispose()
                model.dispose()
                swingBookmarks.dispose()
                sorted.dispose()
                observed.dispose()
                source.dispose()
            }
        }
    }
}
