/*
 * Created by JFormDesigner on Sat Apr 27 12:49:11 CEST 2024
 */

package mediathek.gui.tabs.tab_film

import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.swing.GlazedListsSwing
import mediathek.tool.ApplicationConfiguration
import org.apache.commons.configuration2.sync.LockMode
import org.apache.logging.log4j.LogManager
import java.awt.Window
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.util.NoSuchElementException
import javax.swing.JMenuItem

class EditHistoryDialog(
    owner: Window,
    menuItem: JMenuItem,
    private val eventList: EventList<String>,
) : EditHistoryDialogBase(owner) {
    private val keyAdapter = DeleteKeyAdapter()
    private var keyAdapterInstalled = false

    init {
        menuItem.isEnabled = false
        addWindowListener(object : WindowAdapter() {
            override fun windowClosed(event: WindowEvent) {
                menuItem.isEnabled = true
                savePosition()
            }
        })

        list.model = GlazedListsSwing.eventListModelWithThreadProxyList(eventList)
        list.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                adjustButtons()
            }
        }
        adjustButtons()

        btnDeleteEntries.addActionListener { deleteEntries() }
        btnUp.addActionListener {
            val idx = moveEntry(list.selectedIndex) { it - 1 }
            list.selectedIndex = idx
        }
        btnDown.addActionListener {
            val idx = moveEntry(list.selectedIndex) { it + 1 }
            list.selectedIndex = idx
        }

        restorePosition()
    }

    private fun deleteEntries() {
        val listModel = list.model
        val changeList = list.selectedIndices.map { listModel.getElementAt(it) }

        val lock = eventList.readWriteLock.writeLock()
        lock.lock()
        try {
            changeList.forEach(eventList::remove)
        } finally {
            lock.unlock()
        }
    }

    private fun moveEntry(idx: Int, operator: (Int) -> Int): Int {
        val lock = eventList.readWriteLock.writeLock()
        lock.lock()
        try {
            val obj = eventList[idx]
            eventList.removeAt(idx)
            val newIdx = operator(idx)
            eventList.add(newIdx, obj)
            return newIdx
        } finally {
            lock.unlock()
        }
    }

    private fun restorePosition() {
        val config = ApplicationConfiguration.getConfiguration()
        try {
            config.lock(LockMode.READ)
            val x = config.getInt(CONFIG_X)
            val y = config.getInt(CONFIG_Y)
            val width = config.getInt(CONFIG_WIDTH)
            val height = config.getInt(CONFIG_HEIGHT)

            setSize(width, height)
            setLocation(x, y)
        } catch (_: NoSuchElementException) {
        } catch (ex: Exception) {
            logger.error("Unhandled exception", ex)
        } finally {
            config.unlock(LockMode.READ)
        }
    }

    private fun savePosition() {
        val config = ApplicationConfiguration.getConfiguration()
        try {
            config.lock(LockMode.WRITE)
            val size = size
            val location = location
            config.setProperty(CONFIG_WIDTH, size.width)
            config.setProperty(CONFIG_HEIGHT, size.height)
            config.setProperty(CONFIG_X, location.x)
            config.setProperty(CONFIG_Y, location.y)
        } finally {
            config.unlock(LockMode.WRITE)
        }
    }

    private fun adjustButtons() {
        val itemCount = list.selectionModel.selectedItemsCount
        val singleSelection = itemCount == 1
        btnDeleteEntries.isEnabled = itemCount > 0
        btnUp.isEnabled = singleSelection
        btnDown.isEnabled = singleSelection
        if (singleSelection) {
            val idx = list.selectionModel.leadSelectionIndex
            if (idx == 0) {
                btnUp.isEnabled = false
            }
            if (idx == list.model.size - 1) {
                btnDown.isEnabled = false
            }
        }
        setupKeyListener(itemCount)
    }

    private fun setupKeyListener(itemCount: Int) {
        if (itemCount > 0 && !keyAdapterInstalled) {
            list.addKeyListener(keyAdapter)
            keyAdapterInstalled = true
        } else if (itemCount == 0 && keyAdapterInstalled) {
            list.removeKeyListener(keyAdapter)
            keyAdapterInstalled = false
        }
    }

    private inner class DeleteKeyAdapter : KeyAdapter() {
        override fun keyReleased(event: KeyEvent) {
            if (event.keyCode == KeyEvent.VK_DELETE) {
                event.consume()
                deleteEntries()
            }
        }
    }

    private companion object {
        private const val CONFIG_X = "edit_history.x"
        private const val CONFIG_Y = "edit_history.y"
        private const val CONFIG_HEIGHT = "edit_history.height"
        private const val CONFIG_WIDTH = "edit_history.width"
        private val logger = LogManager.getLogger()
    }
}
