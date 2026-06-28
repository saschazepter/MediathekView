package mediathek.controller.starter

import mediathek.config.Daten
import mediathek.daten.DownloadInfos
import mediathek.daten.ListeDownloads

class DownloadServices(daten: Daten) {
    val queue: ListeDownloads = ListeDownloads(daten)
    val buttonQueue: ListeDownloads = ListeDownloads(daten)
    val info: DownloadInfos = DownloadInfos(daten)
    val starter: DownloadStartCoordinator = DownloadStartCoordinator(daten)

    fun shutdown() {
        starter.shutdown()
    }
}
