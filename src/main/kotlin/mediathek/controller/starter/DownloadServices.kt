package mediathek.controller.starter

import mediathek.config.CommandLineOptions
import mediathek.config.Daten
import mediathek.config.application.ApplicationConfiguration
import mediathek.daten.DatenDownload
import mediathek.daten.DatenFilm
import mediathek.daten.DownloadInfos
import mediathek.daten.DownloadSource
import mediathek.daten.ListeDownloads
import mediathek.gui.dialog.MissingProgramSetDialog
import mediathek.tool.datum.DateUtil
import java.time.LocalDate
import java.util.function.Predicate
import javax.swing.JFrame

class DownloadServices(
    private val daten: Daten,
) {
    val queue: ListeDownloads = ListeDownloads(daten)
    val buttonQueue: ListeDownloads = ListeDownloads(daten)
    val info: DownloadInfos = DownloadInfos(daten)
    val starter: DownloadStartCoordinator = DownloadStartCoordinator(daten)

    fun refreshAboDownloads() {
        queue.abosAuffrischen()
    }

    fun searchAboDownloads(parent: JFrame?): List<DatenDownload> = synchronized(queue) {
        // in der Filmliste nach passenden Filmen suchen und
        // in die Liste der Downloads eintragen
        val downloadUrls = HashSet<String>()
        val addedDownloads = mutableListOf<DatenDownload>()
        // mit den bereits enthaltenen URL füllen
        queue.forEach { download -> downloadUrls.add(download.downloadUrl) }

        // prüfen ob in "alle Filme" oder nur "nach Blacklist" gesucht werden soll
        val checkWithBlackList = ApplicationConfiguration.getInstance().blacklistApplyToAbo
        val defaultPset = daten.programSets.list.getPsetAbo("")
        val today = LocalDate.now(DateUtil.MV_DEFAULT_TIMEZONE)

        val aboHistoryController = daten.abos.historyController
        val listeFilme = daten.filmCatalog.allFilms
        val blacklistFilter: Predicate<DatenFilm> = if (checkWithBlackList) {
            daten.blacklist.createDownloadsPredicate()
        } else {
            Predicate { true }
        }

        for (film in listeFilme) {
            val abo = daten.abos.findAboForFilm(film, true) ?: continue
            if (!abo.isActive) {
                continue
            }
            if (checkWithBlackList && !blacklistFilter.test(film)) {
                // Blacklist auch bei Abos anwenden
                continue
            }
            if (aboHistoryController.urlExists(film.urlNormalQuality)) {
                // ist schon mal geladen worden
                continue
            }

            val pset = if (abo.psetName.isEmpty()) defaultPset else daten.programSets.list.getPsetAbo(abo.psetName)
            if (pset != null) {
                // mit der tatsächlichen URL prüfen, ob die URL schon in der Downloadliste ist
                val downloadUrl = film.getUrlFuerAufloesung(pset.aufloesung)
                if (!downloadUrls.add(downloadUrl)) {
                    continue
                }

                // diesen Film in die Downloadliste eintragen
                abo.downloadDate = today
                if (abo.psetName != pset.name) {
                    // nur den Namen anpassen, falls geändert
                    abo.psetName = pset.name
                }

                // dann in die Liste schreiben
                val download = DatenDownload(pset, film, DownloadSource.ABO, abo, "", "", "")
                queue.add(download)
                addedDownloads.add(download)
            } else {
                if (parent == null || CommandLineOptions.isDownloadAndQuit()) {
                    throw IllegalStateException("Kein Programmset für Abo \"${abo.name}\" konfiguriert.")
                }
                MissingProgramSetDialog.showMissingAboProgramSet(parent, daten)
                break
            }
        }

        if (addedDownloads.isNotEmpty()) {
            queue.listeNummerieren()
        }
        addedDownloads
    }

    fun shutdown() {
        starter.shutdown()
    }
}
