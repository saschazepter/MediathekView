package mediathek.tool.notification

import mediathek.config.Konstanten
import java.awt.SystemTray
import java.awt.TrayIcon

class WinNotificationCenter : NotificationBackend {
    private val lifecycleLock = Any()
    private var trayIcon: TrayIcon? = null

    override fun publish(notification: NotificationMessage) {
        synchronized(lifecycleLock) {
            val currentTrayIcon = trayIcon ?: return
            val type = when (notification.type) {
                MessageType.INFO -> TrayIcon.MessageType.INFO
                MessageType.ERROR -> TrayIcon.MessageType.ERROR
            }
            currentTrayIcon.displayMessage(notification.title, notification.message, type)
        }
    }

    override fun close() {
        synchronized(lifecycleLock) {
            val currentTrayIcon = trayIcon ?: return
            trayIcon = null
            SystemTray.getSystemTray().remove(currentTrayIcon)
        }
    }

    init {
        check(SystemTray.isSupported()) { "System Tray is not supported" }
        val tray = SystemTray.getSystemTray()
        val newTrayIcon = TrayIcon(Konstanten.ICON_TRAY, "MediathekView ${Konstanten.MVVERSION}")
        newTrayIcon.isImageAutoSize = true

        tray.add(newTrayIcon)
        trayIcon = newTrayIcon
    }
}
