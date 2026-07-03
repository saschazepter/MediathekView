package mediathek.windows

import mediathek.config.Daten
import mediathek.mainwindow.MainWindowDarkModeActionPlacement
import mediathek.mainwindow.MediathekGui
import mediathek.mainwindow.StartupFilmlistPreload
import mediathek.shutdown.WindowsComputerShutdown
import mediathek.tool.notification.WinNotificationCenter

class MediathekGuiWindows(
    daten: Daten,
    startupFilmlistPreload: StartupFilmlistPreload? = null,
) : MediathekGui(
    daten,
    ::WinNotificationCenter,
    WindowsComputerShutdown(),
    { frame -> WindowsDownloadProgressIndicator(frame, daten.downloads) },
    MainWindowDarkModeActionPlacement.MENU_BAR,
    startupFilmlistPreload,
)
