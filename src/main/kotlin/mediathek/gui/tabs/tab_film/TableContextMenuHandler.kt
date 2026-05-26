/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.gui.tabs.tab_film

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import mediathek.config.Daten
import mediathek.config.Konstanten
import mediathek.config.StandardLocations
import mediathek.controller.history.SeenHistoryController
import mediathek.controller.starter.Start
import mediathek.daten.DatenFilm
import mediathek.daten.DatenPset
import mediathek.filmlisten.writer.FilmListWriter
import mediathek.gui.actions.CreateNewAboAction
import mediathek.gui.duplicates.details.DuplicateFilmDetailsDialog
import mediathek.mainwindow.MediathekGui
import mediathek.tool.ApplicationConfiguration
import mediathek.tool.FileDialogs
import mediathek.tool.MVInfoFile
import mediathek.tool.table.MVFilmTable
import org.apache.logging.log4j.LogManager
import java.awt.Point
import java.awt.event.*
import java.awt.print.PrinterException
import java.util.*
import javax.swing.*

/**
 * Implements the context menu for tab film.
 */
class TableContextMenuHandler(
    private val host: Host,
) : MouseAdapter() {
    interface Host {
        fun table(): MVFilmTable
        fun getCurrentlySelectedFilm(): Optional<DatenFilm>
        fun getFilm(row: Int): Optional<DatenFilm>
        fun playSelectedFilm()
        fun saveSelectedFilm()
        fun startFilmWithPset(pSet: DatenPset)
        fun setSelectionUpdatesSuspended(suspended: Boolean)
        fun gui(): MediathekGui
        fun actions(): FilmUiActions
    }

    private val daten = Daten.getInstance()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    private val createAboAction = CreateNewAboAction(daten.listeAbo) { host.gui() }
    private val beobPrint = BeobPrint()
    private val beobAbo = BeobAbo(false)
    private val beobAboMitTitel = BeobAbo(true)
    private val unseenActionListener = BeobHistory(false)
    private val seenActionListener = BeobHistory(true)
    private val jDownloadHelper = JDownloadHelper()
    private val pyLoadHelper = PyLoadHelper()
    private val filmSpecificContextMenuBuilder = FilmSpecificContextMenuBuilder(host, jDownloadHelper, pyLoadHelper)
    private val contextMenuBuilder = FilmContextMenuBuilder(
        host,
        daten,
        beobAbo,
        beobAboMitTitel,
        this::addBlacklistRuleForSelectedFilm,
        filmSpecificContextMenuBuilder,
        this::addPrintAndInfoActions,
        this::addFileAndDuplicateActions,
    )
    private var popupPoint: Point? = null

    override fun mouseClicked(event: MouseEvent) {
        if (event.button == MouseEvent.BUTTON1) {
            if (event.clickCount == 1) {
                popupPoint = event.point
                val point = popupPoint ?: return
                val row = host.table().rowAtPoint(point)
                val column = host.table().columnAtPoint(point)
                if (row >= 0) {
                    buttonTable(row, column)
                }
            } else if (event.clickCount > 1) {
                host.gui().filmInfoDialog?.let { infoDialog ->
                    if (!infoDialog.isVisible) {
                        infoDialog.showInfo()
                    }
                }
            }
        }
    }

    override fun mousePressed(event: MouseEvent) {
        if (event.isPopupTrigger) {
            showMenu(event)
        }
    }

    override fun mouseReleased(event: MouseEvent) {
        if (event.isPopupTrigger) {
            showMenu(event)
        }
    }

    private fun buttonTable(row: Int, column: Int) {
        if (row == -1) {
            return
        }

        when (host.table().convertColumnIndexToModel(column)) {
            DatenFilm.FILM_ABSPIELEN -> host.getCurrentlySelectedFilm().ifPresent { film ->
                var dontPlay = false
                val download = daten.listeDownloadsButton.getDownloadUrlFilm(film.urlNormalQuality)
                if (download != null && download.start != null && download.start.status == Start.STATUS_RUN) {
                    dontPlay = true
                    daten.listeDownloadsButton.delDownloadButton(film.urlNormalQuality)
                }
                if (!dontPlay) {
                    host.playSelectedFilm()
                }
            }

            DatenFilm.FILM_AUFZEICHNEN -> host.saveSelectedFilm()
            DatenFilm.FILM_MERKEN -> host.getCurrentlySelectedFilm().ifPresent { film ->
                if (!film.isLivestream) {
                    if (film.isBookmarked) {
                        host.actions().bookmarkRemoveFilm.actionPerformed(null)
                    } else {
                        host.actions().bookmarkAddFilm.actionPerformed(null)
                    }
                }
            }
        }
    }

    private fun showMenu(event: MouseEvent) {
        popupPoint = event.point
        val point = popupPoint ?: return
        val row = host.table().rowAtPoint(point)
        if (row < 0) {
            return
        }
        host.table().setRowSelectionInterval(row, row)

        val popupMenu = contextMenuBuilder.createContextMenu(host.getFilm(row))
        popupMenu.show(event.component, event.x, event.y)
    }

    private fun addPrintAndInfoActions(popupMenu: JPopupMenu, selectedFilm: Optional<DatenFilm>) {
        val printTableMenuItem = JMenuItem("Tabelle drucken")
        printTableMenuItem.addActionListener(beobPrint)
        popupMenu.add(printTableMenuItem)

        popupMenu.add(host.actions().showFilmInformation)
        selectedFilm.ifPresent { film -> setupHistoryContextActions(popupMenu, film) }
    }

    private fun addFileAndDuplicateActions(popupMenu: JPopupMenu, film: DatenFilm) {
        if (!film.isLivestream) {
            popupMenu.addSeparator()
            popupMenu.add(createInfoFileMenuItem(film))
        }

        if (film.isDuplicate) {
            popupMenu.addSeparator()
            popupMenu.add(createDuplicateDetailsMenuItem(film))
        }

        if (!film.isLivestream) {
            popupMenu.addSeparator()
            popupMenu.add(createRemoveDuplicatesMenuItem(film))
        }
    }

    private fun createInfoFileMenuItem(film: DatenFilm): JMenuItem =
        JMenuItem("Infodatei erzeugen...").apply {
            addActionListener {
                val file = FileDialogs.chooseSaveFileLocation(MediathekGui.ui(), "Infodatei speichern", "")
                if (file != null) {
                    try {
                        MVInfoFile().writeManualInfoFile(film, file.toPath())
                    } catch (e: Exception) {
                        throw RuntimeException(e)
                    }
                }
            }
        }

    private fun createDuplicateDetailsMenuItem(film: DatenFilm): JMenuItem =
        JMenuItem("Zusammengehörige Filme anzeigen...").apply {
            addActionListener {
                DuplicateFilmDetailsDialog(MediathekGui.ui(), film).isVisible = true
            }
        }

    private fun createRemoveDuplicatesMenuItem(film: DatenFilm): JMenuItem =
        JMenuItem("Duplikate entfernen...").apply {
            addActionListener { performDuplicateRemoval(film) }
        }

    private fun performDuplicateRemoval(film: DatenFilm) {
        val completeFilmList = daten.listeFilme
        val filteredFilmList = daten.listeBlacklist
        val duplicateList = ArrayList(
            completeFilmList.parallelStream()
                .filter { it.sender.equals(film.sender, ignoreCase = true) }
                .filter { it.thema.equals(film.thema, ignoreCase = true) }
                .filter { it.title.equals(film.title, ignoreCase = true) }
                .filter { it.urlNormalQuality.equals(film.urlNormalQuality, ignoreCase = true) }
                .toList(),
        )
        val filmCount = duplicateList.size

        if (filmCount <= 1) {
            JOptionPane.showMessageDialog(
                host.gui(),
                "Es wurden keine Duplikate gefunden.",
                Konstanten.PROGRAMMNAME,
                JOptionPane.INFORMATION_MESSAGE,
            )
            return
        }

        val duplicateCount = filmCount - 1
        val duplicateString = if (duplicateCount == 1) "Duplikat" else "Duplikate"
        val message = "Es wurden $duplicateCount $duplicateString gefunden.\nMöchten Sie diese entfernen?"
        val result = JOptionPane.showConfirmDialog(
            host.gui(),
            message,
            Konstanten.PROGRAMMNAME,
            JOptionPane.YES_NO_OPTION,
        )
        if (result != JOptionPane.YES_OPTION) {
            return
        }

        duplicateList.remove(film)
        completeFilmList.removeAll(duplicateList.toSet())

        uiScope.launch {
            val writeResult = withContext(Dispatchers.IO) {
                runCatching {
                    FilmListWriter(false).writeFilmList(
                        StandardLocations.getFilmlistFilePathString(),
                        completeFilmList,
                    )
                }
            }

            writeResult
                .onSuccess {
                    filteredFilmList.filterListAndNotifyListeners()
                    JOptionPane.showMessageDialog(
                        host.gui(),
                        "Duplikate wurden entfernt.",
                        Konstanten.PROGRAMMNAME,
                        JOptionPane.INFORMATION_MESSAGE,
                    )
                }
                .onFailure { error ->
                    logger.error("Could not persist duplicate-removal changes.", error)
                    JOptionPane.showMessageDialog(
                        host.gui(),
                        "Duplikate konnten nicht gespeichert werden.",
                        Konstanten.PROGRAMMNAME,
                        JOptionPane.ERROR_MESSAGE,
                    )
                }
        }
    }

    private fun setupHistoryContextActions(popupMenu: JPopupMenu, film: DatenFilm) {
        if (!film.isLivestream) {
            SeenHistoryController().use { history ->
                val historyMenuItem = if (history.hasBeenSeen(film)) {
                    JMenuItem("Film als ungesehen markieren").apply {
                        addActionListener(unseenActionListener)
                    }
                } else {
                    JMenuItem("Film als gesehen markieren").apply {
                        addActionListener(seenActionListener)
                    }
                }
                popupMenu.add(historyMenuItem)
            }
        }
    }

    private fun selectedFilmAtPopupPoint(): DatenFilm? {
        val point = popupPoint ?: return null
        val row = host.table().rowAtPoint(point)
        if (row == -1) {
            return null
        }
        return host.getFilm(row).orElse(null)
    }

    private inner class BeobHistory(
        private val seen: Boolean,
    ) : ActionListener {
        private fun updateHistory(film: DatenFilm) {
            SeenHistoryController().use { history ->
                if (seen) {
                    history.markSeen(film)
                } else {
                    history.markUnseen(film)
                }
            }
        }

        override fun actionPerformed(event: ActionEvent?) {
            selectedFilmAtPopupPoint()?.let(::updateHistory)
        }
    }

    private inner class BeobPrint : ActionListener {
        override fun actionPerformed(event: ActionEvent?) {
            try {
                host.table().print()
            } catch (ex: PrinterException) {
                logger.error(ex)
            }
        }
    }

    private inner class BeobAbo(
        private val mitTitel: Boolean,
    ) : ActionListener {
        override fun actionPerformed(event: ActionEvent?) {
            selectedFilmAtPopupPoint()?.let { film ->
                host.setSelectionUpdatesSuspended(true)
                try {
                    val datenAbo = daten.listeAbo.getAboFuerFilm_schnell(film, false)
                    if (datenAbo != null) {
                        daten.listeAbo.aboLoeschen(datenAbo)
                    } else {
                        createAboAction.createAbo(
                            aboname = film.thema,
                            filmSender = film.sender,
                            filmThema = film.thema,
                            filmTitel = if (mitTitel) film.title else "",
                        )
                    }
                } finally {
                    host.setSelectionUpdatesSuspended(false)
                }
            }
        }
    }

    private fun turnOnBlacklist() {
        ApplicationConfiguration.getConfiguration().setProperty(ApplicationConfiguration.BLACKLIST_IS_ON, true)
    }

    private fun addBlacklistRuleForSelectedFilm(blacklistRuleAppender: (DatenFilm) -> Unit) {
        selectedFilmAtPopupPoint()?.let { film ->
            turnOnBlacklist()
            blacklistRuleAppender(film)
        }
    }

    companion object {
        private val logger = LogManager.getLogger()
    }
}
