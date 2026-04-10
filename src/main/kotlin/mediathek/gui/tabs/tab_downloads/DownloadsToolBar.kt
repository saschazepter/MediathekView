package mediathek.gui.tabs.tab_downloads

import javax.swing.Action
import javax.swing.JToolBar

class DownloadsToolBar(
    refreshDownloadListAction: Action,
    startAllDownloadsAction: Action,
    playDownloadAction: Action,
    deferDownloadsAction: Action,
    deleteDownloadsAction: Action,
    cleanupDownloadListAction: Action
) : JToolBar() {
    init {
        isFloatable = true
        name = "Downloads"

        add(refreshDownloadListAction)
        add(startAllDownloadsAction)
        add(playDownloadAction)
        add(deferDownloadsAction)
        add(deleteDownloadsAction)
        add(cleanupDownloadListAction)
    }
}
