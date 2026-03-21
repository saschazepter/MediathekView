package mediathek.gui.bandwidth

import mediathek.tool.withLock
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.sync.LockMode
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import javax.swing.JDialog

internal class WriteConfigComponentListener(private val config: Configuration, private val dialog: JDialog) :
    ComponentAdapter() {
    override fun componentResized(e: ComponentEvent) {
        config.withLock(LockMode.WRITE) {
            val size = dialog.size
            setProperty(BandwidthDialog.CONFIG_WIDTH, size.width)
            setProperty(BandwidthDialog.CONFIG_HEIGHT, size.height)
        }
    }

    override fun componentMoved(e: ComponentEvent) {
        config.withLock(LockMode.WRITE) {
            val location = dialog.location
            setProperty(BandwidthDialog.CONFIG_X, location.x)
            setProperty(BandwidthDialog.CONFIG_Y, location.y)
        }
    }
}
