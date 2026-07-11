package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.impl.ObservableConnector
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

internal class ObservableConnectorTest {
    @Test
    fun propertyChangesProduceListUpdatesUntilElementIsRemoved() {
        val bean = ObservableBean("before")
        val source = BasicEventList<ObservableBean>().apply { add(bean) }
        val observed = ObservableElementList(source, GlazedLists.observableConnector())
        val updatedElements = mutableListOf<ObservableBean>()
        observed.addListEventListener { changes ->
            while (changes.next()) {
                if (changes.type == ListEvent.UPDATE) updatedElements += observed[changes.index]
            }
        }

        bean.updateValue("after")

        assertEquals(1, updatedElements.size)
        assertSame(bean, updatedElements.single())

        observed.remove(bean)
        bean.updateValue("detached")

        assertEquals(1, updatedElements.size)
    }

    private class ObservableBean(initialValue: String) : ObservableConnector.PropertyChangeObservable {
        private val propertyChanges = PropertyChangeSupport(this)
        private var value = initialValue

        fun updateValue(newValue: String) {
            val oldValue = value
            value = newValue
            propertyChanges.firePropertyChange("value", oldValue, newValue)
        }

        override fun addPropertyChangeListener(listener: PropertyChangeListener) {
            propertyChanges.addPropertyChangeListener(listener)
        }

        override fun removePropertyChangeListener(listener: PropertyChangeListener) {
            propertyChanges.removePropertyChangeListener(listener)
        }
    }
}
