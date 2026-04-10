package mediathek.gui.tabs.tab_downloads

import mediathek.config.Daten
import mediathek.daten.DownloadStartInfo
import mediathek.gui.messages.UpdateStatusBarLeftDisplayEvent
import mediathek.tool.MessageBus.messageBus
import net.engio.mbassy.listener.Handler
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport

class DownloadStartInfoProperty {
    private val pcs = PropertyChangeSupport(this)

    var info: DownloadStartInfo = Daten.getInstance().listeDownloads.starts
        set(value) {
            val oldValue = field
            field = value
            pcs.firePropertyChange("info", oldValue, field)
        }

    init {
        messageBus.subscribe(this)
    }

    @Suppress("UNUSED_PARAMETER")
    @Handler
    private fun handleLeftDisplayUpdate(event: UpdateStatusBarLeftDisplayEvent) {
        info = Daten.getInstance().listeDownloads.starts
    }

    fun addStartInfoChangeListener(listener: PropertyChangeListener) {
        pcs.addPropertyChangeListener(listener)
    }
}
