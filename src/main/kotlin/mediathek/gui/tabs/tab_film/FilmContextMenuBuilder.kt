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

import mediathek.config.Daten
import mediathek.daten.DatenFilm
import mediathek.daten.blacklist.BlacklistRule
import java.awt.event.ActionListener
import java.util.Optional
import javax.swing.JMenu
import javax.swing.JMenuItem
import javax.swing.JPopupMenu

class FilmContextMenuBuilder(
    private val host: TableContextMenuHandler.Host,
    private val daten: Daten,
    private val aboWithoutTitleAction: ActionListener,
    private val aboWithTitleAction: ActionListener,
    private val addBlacklistRuleForSelectedFilm: ((DatenFilm) -> Unit) -> Unit,
    private val addFilmSpecificContextActions: (JPopupMenu, DatenFilm) -> Unit,
    private val addPrintAndInfoActions: (JPopupMenu, Optional<DatenFilm>) -> Unit,
    private val addFileAndDuplicateActions: (JPopupMenu, DatenFilm) -> Unit,
) {
    fun createContextMenu(selectedFilm: Optional<DatenFilm>): JPopupMenu =
        JPopupMenu().apply {
            addPrimaryContextActions(this, selectedFilm)
            addFilmProgramsMenu(this)
            addBlacklistMenu(this)
            selectedFilm.ifPresent { film -> addFilmSpecificContextActions(this, film) }
            addPrintAndInfoActions(this, selectedFilm)
            selectedFilm.ifPresent { film -> addFileAndDuplicateActions(this, film) }
        }

    private fun addPrimaryContextActions(popupMenu: JPopupMenu, selectedFilm: Optional<DatenFilm>) {
        val actions = host.actions()
        popupMenu.add(actions.playFilm)
        popupMenu.add(actions.saveFilm)

        val bookmarkMenuItem = JMenuItem(actions.bookmarkAddFilm)
        popupMenu.add(bookmarkMenuItem)
        popupMenu.addSeparator()
        addAboMenu(popupMenu, selectedFilm)
        updateBookmarkMenuItem(popupMenu, bookmarkMenuItem, selectedFilm)
    }

    private fun addAboMenu(popupMenu: JPopupMenu, selectedFilm: Optional<DatenFilm>) {
        val submenuAbo = JMenu("Abo")
        popupMenu.add(submenuAbo)

        val itemAbo = JMenuItem("Abo mit Sender und Thema anlegen")
        val itemAboMitTitel = JMenuItem("Abo mit Sender und Thema und Titel anlegen")

        selectedFilm.ifPresent { film -> configureAboMenuItems(film, itemAbo, itemAboMitTitel) }

        submenuAbo.add(itemAbo)
        submenuAbo.add(itemAboMitTitel)
    }

    private fun configureAboMenuItems(
        film: DatenFilm,
        itemAbo: JMenuItem,
        itemAboMitTitel: JMenuItem,
    ) {
        if (daten.listeAbo.getAboFuerFilm_schnell(film, false) != null) {
            itemAbo.isEnabled = false
            itemAboMitTitel.isEnabled = false
        } else {
            itemAbo.addActionListener(aboWithoutTitleAction)
            itemAboMitTitel.addActionListener(aboWithTitleAction)
        }
    }

    private fun updateBookmarkMenuItem(
        popupMenu: JPopupMenu,
        bookmarkMenuItem: JMenuItem,
        selectedFilm: Optional<DatenFilm>,
    ) {
        selectedFilm.ifPresent { film ->
            if (film.isLivestream) {
                popupMenu.remove(bookmarkMenuItem)
            } else {
                bookmarkMenuItem.text = if (film.isBookmarked) {
                    "Film aus Merkliste entfernen"
                } else {
                    "Film merken"
                }
            }
        }
    }

    private fun addFilmProgramsMenu(popupMenu: JPopupMenu) {
        val submenu = JMenu("Film mit Set starten")
        popupMenu.add(submenu)
        val liste = Daten.getInstance().listePset.listeButton
        for (pset in liste) {
            if (pset.listeProg.isEmpty() && pset.name.isEmpty()) {
                continue
            }

            val item = JMenuItem(pset.name)
            pset.foregroundColor.ifPresent(item::setForeground)
            if (pset.listeProg.isNotEmpty()) {
                item.addActionListener { host.startFilmWithPset(pset) }
            }
            submenu.add(item)
        }
    }

    private fun addBlacklistMenu(popupMenu: JPopupMenu) {
        val submenuBlack = JMenu("Blacklist")
        popupMenu.add(submenuBlack)

        val itemBlackSender = JMenuItem("Sender in die Blacklist einfügen")
        itemBlackSender.addActionListener {
            addBlacklistRuleForSelectedFilm { film ->
                daten.listeBlacklist.add(BlacklistRule(film.sender, "", "", ""))
            }
        }
        submenuBlack.add(itemBlackSender)

        val itemBlackThema = JMenuItem("Thema in die Blacklist einfügen")
        itemBlackThema.addActionListener {
            addBlacklistRuleForSelectedFilm { film ->
                daten.listeBlacklist.add(BlacklistRule("", film.thema, "", ""))
            }
        }
        submenuBlack.add(itemBlackThema)

        val itemAddTitleToBlacklist = JMenuItem("Titel in die Blacklist einfügen")
        itemAddTitleToBlacklist.addActionListener {
            addBlacklistRuleForSelectedFilm { film ->
                daten.listeBlacklist.add(BlacklistRule("", "", film.title, ""))
            }
        }
        submenuBlack.add(itemAddTitleToBlacklist)

        val itemBlackSenderThema = JMenuItem("Sender und Thema in die Blacklist einfügen")
        itemBlackSenderThema.addActionListener {
            addBlacklistRuleForSelectedFilm { film ->
                daten.listeBlacklist.add(BlacklistRule(film.sender, film.thema, "", ""))
            }
        }
        submenuBlack.add(itemBlackSenderThema)
    }
}
