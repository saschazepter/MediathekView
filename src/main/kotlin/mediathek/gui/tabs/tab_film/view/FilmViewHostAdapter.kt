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
import javax.swing.JCheckBoxMenuItem
import javax.swing.JTabbedPane

class FilmViewHostAdapter(
    private val psetButtonsTab: JTabbedPane,
    private val psetButtonsPanelProvider: () -> PsetButtonsPanel?,
    private val setPsetButtonsPanelAction: (PsetButtonsPanel) -> Unit,
    private val showButtonsMenuItemProvider: () -> JCheckBoxMenuItem,
    private val showDescriptionMenuItemProvider: () -> JCheckBoxMenuItem,
    private val actionsProvider: () -> FilmUiActions,
    private val setDescriptionTabVisibleAction: (Boolean) -> Unit,
    private val startFilmWithPsetAction: (DatenPset) -> Unit,
) : FilmViewController.Host {
    override fun psetButtonsTab(): JTabbedPane = psetButtonsTab

    override fun psetButtonsPanel(): PsetButtonsPanel? = psetButtonsPanelProvider()

    override fun setPsetButtonsPanel(panel: PsetButtonsPanel) {
        setPsetButtonsPanelAction(panel)
    }

    override fun showButtonsMenuItem(): JCheckBoxMenuItem = showButtonsMenuItemProvider()

    override fun showDescriptionMenuItem(): JCheckBoxMenuItem = showDescriptionMenuItemProvider()

    override fun actions(): FilmUiActions = actionsProvider()

    override fun setDescriptionTabVisible(visible: Boolean) {
        setDescriptionTabVisibleAction(visible)
    }

    override fun startFilmWithPset(pset: DatenPset) {
        startFilmWithPsetAction(pset)
    }
}
