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

package mediathek.gui.tabs.tab_downloads

import ca.odell.glazedlists.GlazedLists
import ca.odell.glazedlists.swing.GlazedListsSwing
import mediathek.tool.ApplicationConfiguration
import org.apache.commons.configuration2.Configuration

class DownloadsFilterController(
    private val toolBar: DownloadsDisplayFilterToolBar,
    private val config: Configuration,
    private val onFilterChanged: Runnable
) {
    var displayFilter: DisplayFilter = DisplayFilter.all()
        private set
    var viewFilter: ViewFilter = ViewFilter.all()
        private set

    fun install() {
        setupDisplayCategories()
        setupViewCategories()
    }

    private fun setupDisplayCategories() {
        val displaySelectionList = GlazedLists.eventListOf(
            DisplayFilter.ALL,
            DisplayFilter.DOWNLOADS_ONLY,
            DisplayFilter.ABOS_ONLY
        )
        val comboBox = toolBar.displayCategoriesComboBox
        comboBox.model = GlazedListsSwing.eventComboBoxModelWithThreadProxyList(displaySelectionList)
        displayFilter = DisplayFilter.from(config.getString(ApplicationConfiguration.DOWNLOAD_DISPLAY_FILTER, DisplayFilter.ALL))
        comboBox.model.selectedItem = displayFilter.selectedItem()
        comboBox.addActionListener {
            displayFilter = DisplayFilter.from(comboBox.model.selectedItem)
            config.setProperty(ApplicationConfiguration.DOWNLOAD_DISPLAY_FILTER, displayFilter.selectedItem())
            onFilterChanged.run()
        }
    }

    private fun setupViewCategories() {
        val viewSelectionList = GlazedLists.eventListOf(
            ViewFilter.ALL,
            ViewFilter.NOT_STARTED,
            ViewFilter.STARTED,
            ViewFilter.WAITING,
            ViewFilter.RUN_ONLY,
            ViewFilter.FINISHED_ONLY
        )
        val comboBox = toolBar.viewComboBox
        comboBox.model = GlazedListsSwing.eventComboBoxModelWithThreadProxyList(viewSelectionList)
        viewFilter = ViewFilter.from(config.getString(ApplicationConfiguration.DOWNLOAD_VIEW_FILTER, ViewFilter.ALL))
        comboBox.model.selectedItem = viewFilter.selectedItem()
        comboBox.addActionListener {
            viewFilter = ViewFilter.from(comboBox.model.selectedItem)
            config.setProperty(ApplicationConfiguration.DOWNLOAD_VIEW_FILTER, viewFilter.selectedItem())
            onFilterChanged.run()
        }
    }
}
