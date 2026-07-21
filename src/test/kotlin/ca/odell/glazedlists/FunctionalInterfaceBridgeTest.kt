package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.event.ListEventListener
import ca.odell.glazedlists.matchers.Matcher
import ca.odell.glazedlists.matchers.MatcherEditor
import ca.odell.glazedlists.swing.SortableRenderer
import ca.odell.glazedlists.swing.TableModelEventAdapter
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.util.function.BiConsumer
import java.util.function.Consumer
import java.util.function.Function
import java.util.function.ToIntFunction
import javax.swing.Icon
import javax.swing.table.AbstractTableModel

internal class FunctionalInterfaceBridgeTest {
    @Test
    fun functionListAcceptsJdkFunctionsDirectly() {
        val source = BasicEventList<String>().apply { add("aa") }
        val forward = Function<String, Int>(String::length)
        val reverse = Function<Int, String> { "x".repeat(it) }

        val mapped = FunctionList(source, forward, reverse)

        assertSame(forward, mapped.forwardFunction)
        assertSame(reverse, mapped.reverseFunction)
        assertEquals(listOf(2), mapped.toList())

        mapped.add(3)

        assertEquals(listOf("aa", "xxx"), source)
    }

    @Test
    fun advancedFunctionRetainsItsLifecycleHooks() {
        val source = BasicEventList<String>().apply { add("aa") }
        val reevaluations = mutableListOf<Pair<String, Int>>()
        val disposals = mutableListOf<Pair<String, Int>>()
        val function = object : FunctionList.AdvancedFunction<String, Int> {
            override fun apply(sourceValue: String): Int = sourceValue.length

            override fun reevaluate(sourceValue: String, transformedValue: Int): Int {
                reevaluations += sourceValue to transformedValue
                return sourceValue.length
            }

            override fun dispose(sourceValue: String, transformedValue: Int) {
                disposals += sourceValue to transformedValue
            }
        }
        val mapped = FunctionList(source, function)

        source[0] = "bbbb"
        source.clear()

        assertSame(function, mapped.forwardFunction)
        assertEquals(listOf("bbbb" to 2), reevaluations)
        assertEquals(listOf("bbbb" to 4), disposals)
    }

    @Test
    fun extractionInterfacesDelegateThroughJdkConsumers() {
        val values = mutableListOf<String>()
        val textFilterator = TextFilterator<String> { target, element -> target += "text:$element" }
        val textFilterable = TextFilterable { target -> target += "self" }

        textFilterator.getFilterStrings(values, "one")
        (textFilterator as BiConsumer<MutableList<String>, String>).accept(values, "two")
        textFilterable.getFilterStrings(values)
        (textFilterable as Consumer<MutableList<String>>).accept(values)

        assertEquals(listOf("text:one", "text:two", "self", "self"), values)
    }

    @Test
    fun mappingInterfacesDelegateThroughJdkFunctions() {
        val model = CollectionList.Model<String, Int> { parent -> parent.indices.toList() }
        val evaluator = ThresholdList.Evaluator<String>(String::length)

        assertEquals(listOf(0, 1, 2), model.getChildren("abc"))
        assertEquals(listOf(0, 1, 2), (model as Function<String, List<Int>>).apply("abc"))
        assertEquals(3, evaluator.evaluate("abc"))
        assertEquals(3, (evaluator as ToIntFunction<String>).applyAsInt("abc"))
    }

    @Test
    fun listenerInterfacesDelegateThroughJdkConsumers() {
        val source = BasicEventList<String>()
        lateinit var listEvent: ListEvent<String>
        source.addListEventListener { listEvent = it }
        source.add("value")

        var receivedListEvent: ListEvent<String>? = null
        val listListener = ListEventListener<String> { receivedListEvent = it }
        (listListener as Consumer<ListEvent<String>>).accept(listEvent)
        assertSame(listEvent, receivedListEvent)

        val matcher = Matcher<String> { true }
        val matcherEditor = MatcherEditor.fromMatcher(matcher)
        val matcherEvent = MatcherEditor.Event(matcherEditor, MatcherEditor.Event.CHANGED, matcher)
        var receivedMatcherEvent: MatcherEditor.Event<String>? = null
        val matcherListener = MatcherEditor.Listener<String> { receivedMatcherEvent = it }
        (matcherListener as Consumer<MatcherEditor.Event<String>>).accept(matcherEvent)
        assertSame(matcherEvent, receivedMatcherEvent)

        val edit = TestEdit()
        var receivedEdit: UndoRedoSupport.Edit? = null
        val undoListener = UndoRedoSupport.Listener { receivedEdit = it }
        (undoListener as Consumer<UndoRedoSupport.Edit>).accept(edit)
        assertSame(edit, receivedEdit)

        var receivedIcon: Icon? = TestIcon
        val renderer = SortableRenderer { receivedIcon = it }
        (renderer as Consumer<Icon?>).accept(null)
        assertEquals(null, receivedIcon)
    }

    @Test
    fun tableAdapterFactoryDelegatesThroughJdkFunction() {
        val tableModel = TestTableModel()
        val adapter = TestTableModelEventAdapter()
        val factory = TableModelEventAdapter.Factory<String> { adapter }

        assertSame(adapter, factory.create(tableModel))
        assertSame(adapter, (factory as Function<AbstractTableModel, TableModelEventAdapter<String>>).apply(tableModel))
    }

    private class TestEdit : UndoRedoSupport.Edit {
        override fun undo() = Unit
        override fun canUndo() = true
        override fun redo() = Unit
        override fun canRedo() = false
    }

    private object TestIcon : Icon {
        override fun paintIcon(component: java.awt.Component?, graphics: java.awt.Graphics?, x: Int, y: Int) = Unit
        override fun getIconWidth() = 1
        override fun getIconHeight() = 1
    }

    private class TestTableModel : AbstractTableModel() {
        override fun getRowCount() = 0
        override fun getColumnCount() = 0
        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any? = null
    }

    private class TestTableModelEventAdapter : TableModelEventAdapter<String> {
        override fun listChanged(listChanges: ListEvent<String>) = Unit
        override fun fireTableStructureChanged() = Unit
        override fun fireTableDataChanged() = Unit
        override fun fireTableChanged(startIndex: Int, endIndex: Int, listChangeType: Int) = Unit
    }
}
