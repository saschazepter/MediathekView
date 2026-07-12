package ca.odell.glazedlists

import ca.odell.glazedlists.impl.filter.SearchTerm
import ca.odell.glazedlists.impl.filter.TextMatchers
import ca.odell.glazedlists.impl.filter.TextSearchStrategy
import ca.odell.glazedlists.matchers.TextMatcherEditor
import ca.odell.glazedlists.swing.EventTableColumnModel
import ca.odell.glazedlists.swing.TextComponentMatcherEditor
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.beans.PropertyChangeEvent
import javax.swing.JTextField
import javax.swing.SwingUtilities
import javax.swing.event.ChangeEvent
import javax.swing.event.ListSelectionEvent
import javax.swing.event.TableColumnModelEvent
import javax.swing.event.TableColumnModelListener
import javax.swing.table.TableColumn
import javax.swing.text.PlainDocument

internal class ProbableBugsBehaviorTest {
    @Test
    fun sequenceListAcceptsAnyComparatorMagnitude() {
        val source = BasicEventList<Int>().apply { addAll(listOf(5, 25)) }
        val sequencer = object : SequenceList.Sequencer<Int> {
            override fun previous(value: Int): Int = Math.floorDiv(value - 1, 10) * 10

            override fun next(value: Int): Int = (Math.floorDiv(value, 10) + 1) * 10
        }
        val comparator = Comparator<Int> { left, right -> left.compareTo(right) * 7 }

        SequenceList(source, sequencer, comparator).use { sequence ->
            assertEquals(listOf(0, 10, 20, 30), sequence.toList())
        }
    }

    @Test
    fun textMatcherNormalizationDoesNotSkipAdjacentRedundantTerms() {
        val positive = arrayOf("a", "ab", "abc").map { SearchTerm<Any>(it) }.toTypedArray()
        val negative = arrayOf("a", "ab", "abc")
            .map { SearchTerm<Any>(it, isNegated = true, isRequired = false, field = null) }
            .toTypedArray()

        val strategy = TextMatcherEditor.IDENTICAL_STRATEGY as TextSearchStrategy.Factory
        val normalizedPositive = TextMatchers.normalizeSearchTerms(positive, strategy)
        val normalizedNegative = TextMatchers.normalizeSearchTerms(negative, strategy)

        assertEquals(listOf("abc"), normalizedPositive.map { it.text })
        assertEquals(listOf("a"), normalizedNegative.map { it.text })
    }

    @Test
    fun columnModelRecognizesEquivalentNonInternedPropertyNames() {
        SwingUtilities.invokeAndWait {
            val source = BasicEventList<TableColumn>().apply { add(TableColumn()) }
            val model = EventTableColumnModel(source)
            try {
                var marginChanges = 0
                model.addColumnModelListener(object : TableColumnModelListener {
                    override fun columnMarginChanged(event: ChangeEvent) {
                        marginChanges++
                    }

                    override fun columnAdded(event: TableColumnModelEvent) = Unit
                    override fun columnRemoved(event: TableColumnModelEvent) = Unit
                    override fun columnMoved(event: TableColumnModelEvent) = Unit
                    override fun columnSelectionChanged(event: ListSelectionEvent) = Unit
                })

                model.propertyChange(
                    PropertyChangeEvent(source[0], charArrayOf('w', 'i', 'd', 't', 'h').concatToString(), 75, 100),
                )

                assertEquals(1, marginChanges)
            } finally {
                model.dispose()
            }
        }
    }

    @Test
    fun textComponentEditorRecognizesEquivalentNonInternedDocumentPropertyName() {
        SwingUtilities.invokeAndWait {
            val field = NonInterningTextField()
            val editor = TextComponentMatcherEditor<String>(field, { strings, value -> strings += value })
            try {
                val replacement = PlainDocument()
                var matcherChanges = 0
                editor.addMatcherEditorListener { matcherChanges++ }

                field.document = replacement
                replacement.insertString(0, "new filter", null)

                assertEquals(1, matcherChanges)
            } finally {
                editor.dispose()
            }
        }
    }

    private class NonInterningTextField : JTextField() {
        override fun firePropertyChange(propertyName: String?, oldValue: Any?, newValue: Any?) {
            super.firePropertyChange(propertyName?.toCharArray()?.concatToString(), oldValue, newValue)
        }
    }
}
