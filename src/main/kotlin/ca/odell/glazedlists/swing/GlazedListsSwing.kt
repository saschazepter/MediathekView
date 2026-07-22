/* Glazed Lists                                                 (c) 2003-2013 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.swing

import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.ThresholdList
import ca.odell.glazedlists.TransformedList
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.impl.swing.DefaultTableModelEventAdapterFactory
import ca.odell.glazedlists.impl.swing.LowerThresholdRangeModel
import ca.odell.glazedlists.impl.swing.ManyToOneTableModelEventAdapterFactory
import ca.odell.glazedlists.impl.swing.SwingThreadProxyEventList
import ca.odell.glazedlists.impl.swing.UpperThresholdRangeModel
import javax.swing.BoundedRangeModel

/** A factory for creating objects to be used with Glazed Lists and Swing. */
class GlazedListsSwing private constructor() {
    init {
        throw UnsupportedOperationException()
    }

    companion object {
        /** Wraps [source] in an EventList that fires updates on the Swing event dispatch thread. */
        @JvmStatic
        fun <E> swingThreadProxyList(source: EventList<E>): TransformedList<E, E> =
            SwingThreadProxyEventList(source)

        /** Returns whether [list] fires all updates on the Swing event dispatch thread. */
        @JvmStatic
        fun isSwingThreadProxyList(list: EventList<*>?): Boolean =
            list is SwingThreadProxyEventList<*>

        /** Creates a model that manipulates the lower bound of [target]. */
        @JvmStatic
        fun lowerRangeModel(target: ThresholdList<*>): BoundedRangeModel =
            LowerThresholdRangeModel(target)

        /** Creates a model that manipulates the upper bound of [target]. */
        @JvmStatic
        fun upperRangeModel(target: ThresholdList<*>): BoundedRangeModel =
            UpperThresholdRangeModel(target)

        /** Creates a table model backed directly by [source]. */
        @JvmStatic
        fun <E : Any> eventTableModel(
            source: EventList<E>,
            tableFormat: TableFormat<in E>,
        ): AdvancedTableModel<E> = DefaultEventTableModel(source, tableFormat)

        /** Creates a table model backed by a Swing-thread proxy of [source]. */
        @JvmStatic
        fun <E : Any> eventTableModelWithThreadProxyList(
            source: EventList<E>,
            tableFormat: TableFormat<in E>,
        ): AdvancedTableModel<E> =
            DefaultEventTableModel(createSwingThreadProxyList(source), true, tableFormat)

        /** Creates a table model using [eventAdapterFactory] to translate list events. */
        @JvmStatic
        fun <E : Any> eventTableModel(
            source: EventList<E>,
            tableFormat: TableFormat<in E>,
            eventAdapterFactory: TableModelEventAdapter.Factory<E>,
        ): AdvancedTableModel<E> {
            val result = DefaultEventTableModel(source, tableFormat)
            result.eventAdapter = eventAdapterFactory.create(result)
            return result
        }

        /**
         * Creates a table model backed by a Swing-thread proxy of [source] and using
         * [eventAdapterFactory] to translate list events.
         */
        @JvmStatic
        fun <E : Any> eventTableModelWithThreadProxyList(
            source: EventList<E>,
            tableFormat: TableFormat<in E>,
            eventAdapterFactory: TableModelEventAdapter.Factory<E>,
        ): AdvancedTableModel<E> {
            val result = DefaultEventTableModel(createSwingThreadProxyList(source), true, tableFormat)
            result.eventAdapter = eventAdapterFactory.create(result)
            return result
        }

        /** Creates a reflective table model for the named bean properties. */
        @JvmStatic
        fun <E : Any> eventTableModel(
            source: EventList<E>,
            propertyNames: Array<String>,
            columnLabels: Array<String>,
            writable: BooleanArray,
        ): AdvancedTableModel<E> =
            eventTableModel(source, GlazedLists.tableFormat(propertyNames, columnLabels, writable))

        /** Creates a reflective table model backed by a Swing-thread proxy of [source]. */
        @JvmStatic
        fun <E : Any> eventTableModelWithThreadProxyList(
            source: EventList<E>,
            propertyNames: Array<String>,
            columnLabels: Array<String>,
            writable: BooleanArray,
        ): AdvancedTableModel<E> =
            eventTableModelWithThreadProxyList(
                source,
                GlazedLists.tableFormat(propertyNames, columnLabels, writable),
            )

        /** Returns the default factory for translating list events to table-model events. */
        @JvmStatic
        fun <E> defaultEventAdapterFactory(): TableModelEventAdapter.Factory<E> =
            DefaultTableModelEventAdapterFactory.getInstance()

        /** Returns the factory that translates each list event to at most one table-model event. */
        @JvmStatic
        fun <E> manyToOneEventAdapterFactory(): TableModelEventAdapter.Factory<E> =
            ManyToOneTableModelEventAdapterFactory.getInstance()

        /** Creates a selection model backed directly by [source]. */
        @JvmStatic
        fun <E> eventSelectionModel(source: EventList<E>): AdvancedListSelectionModel<E> =
            DefaultEventSelectionModel(source)

        /** Creates a selection model backed by a Swing-thread proxy of [source]. */
        @JvmStatic
        fun <E> eventSelectionModelWithThreadProxyList(
            source: EventList<E>,
        ): AdvancedListSelectionModel<E> =
            DefaultEventSelectionModel(createSwingThreadProxyList(source), true)

        /** Creates a list model backed directly by [source]. */
        @JvmStatic
        fun <E> eventListModel(source: EventList<E>): DefaultEventListModel<E> =
            DefaultEventListModel(source)

        /** Creates a list model backed by a Swing-thread proxy of [source]. */
        @JvmStatic
        fun <E> eventListModelWithThreadProxyList(source: EventList<E>): DefaultEventListModel<E> =
            DefaultEventListModel(createSwingThreadProxyList(source), true)

        /** Creates a combo-box model backed directly by [source]. */
        @JvmStatic
        fun <E> eventComboBoxModel(source: EventList<E>): DefaultEventComboBoxModel<E> =
            DefaultEventComboBoxModel(source)

        /** Creates a combo-box model backed by a Swing-thread proxy of [source]. */
        @JvmStatic
        fun <E> eventComboBoxModelWithThreadProxyList(
            source: EventList<E>,
        ): DefaultEventComboBoxModel<E> =
            DefaultEventComboBoxModel(createSwingThreadProxyList(source), true)

        private fun <E> createSwingThreadProxyList(source: EventList<E>): EventList<E> {
            source.readWriteLock.readLock().lock()
            return try {
                swingThreadProxyList(source)
            } finally {
                source.readWriteLock.readLock().unlock()
            }
        }
    }
}