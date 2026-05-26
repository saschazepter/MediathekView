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

package mediathek.gui.tabs.tab_film.view

import mediathek.daten.DatenPset
import mediathek.gui.tabs.tab_film.PsetButtonsPanel
import mediathek.gui.tabs.tab_film.actions.FilmUiActions
import java.util.function.Consumer
import java.util.function.Supplier
import javax.swing.JCheckBoxMenuItem
import javax.swing.JTabbedPane

class FilmViewHostAdapter(
    private val psetButtonsTab: JTabbedPane,
    private val psetButtonsPanel: Supplier<PsetButtonsPanel?>,
    private val setPsetButtonsPanel: Consumer<PsetButtonsPanel>,
    private val showButtonsMenuItem: Supplier<JCheckBoxMenuItem>,
    private val showDescriptionMenuItem: Supplier<JCheckBoxMenuItem>,
    private val actions: Supplier<FilmUiActions>,
    private val setDescriptionTabVisible: Consumer<Boolean>,
    private val startFilmWithPset: Consumer<DatenPset>,
) : FilmViewController.Host {
    override fun psetButtonsTab(): JTabbedPane = psetButtonsTab

    override fun psetButtonsPanel(): PsetButtonsPanel? = psetButtonsPanel.get()

    override fun setPsetButtonsPanel(panel: PsetButtonsPanel) {
        setPsetButtonsPanel.accept(panel)
    }

    override fun showButtonsMenuItem(): JCheckBoxMenuItem = showButtonsMenuItem.get()

    override fun showDescriptionMenuItem(): JCheckBoxMenuItem = showDescriptionMenuItem.get()

    override fun actions(): FilmUiActions = actions.get()

    override fun setDescriptionTabVisible(visible: Boolean) {
        setDescriptionTabVisible.accept(visible)
    }

    override fun startFilmWithPset(pset: DatenPset) {
        startFilmWithPset.accept(pset)
    }
}
