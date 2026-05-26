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

import mediathek.gui.messages.TableModelChangeEvent
import mediathek.gui.tabs.tab_film.filter.FilmFilterController
import mediathek.gui.tabs.tab_film.helpers.GuiModelHelperFactory
import mediathek.tool.MessageBus
import mediathek.tool.table.MVFilmTable
import org.apache.logging.log4j.LogManager
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import javax.swing.SwingUtilities
import javax.swing.table.TableModel

class FilmTableReloader(private val host: Host) {
    interface Host {
        fun table(): MVFilmTable

        fun searchFieldData(): SearchFieldData

        fun filterController(): FilmFilterController

        fun tableModelExecutor(): Executor

        fun setSelectionUpdatesSuspended(suspended: Boolean)

        fun updateStartInfoProperty()

        fun updateFilmData()
    }

    private var modelFuture: CompletableFuture<TableModel>? = null
    private var pendingTableReload = false
    private var pendingTableReloadFromSearchField = false

    fun loadTable() {
        loadTable(false)
    }

    fun loadTable(fromSearchField: Boolean) {
        val currentModelFuture = modelFuture
        if (currentModelFuture != null && !currentModelFuture.isDone) {
            pendingTableReload = true
            pendingTableReloadFromSearchField = pendingTableReloadFromSearchField or fromSearchField
            return
        }

        val messageBus = MessageBus.messageBus
        messageBus.publish(TableModelChangeEvent(true, fromSearchField))

        host.setSelectionUpdatesSuspended(true)
        host.table().getSpalten()
        host.table().isEnabled = false

        val decoratedPool = host.tableModelExecutor()
        modelFuture = CompletableFuture.supplyAsync(
            {
                val helper = GuiModelHelperFactory.createGuiModelHelper(host.searchFieldData(), host.filterController())
                helper.filteredTableModel
            },
            decoratedPool
        )
        modelFuture?.whenCompleteAsync(
            { model, thrown ->
                if (thrown == null) {
                    SwingUtilities.invokeLater {
                        host.table().model = model
                        host.table().isEnabled = true
                        host.updateStartInfoProperty()
                        host.table().setSpalten()
                        host.updateFilmData()
                        host.setSelectionUpdatesSuspended(false)
                        host.table().scrollToSelection()
                        messageBus.publish(TableModelChangeEvent(false, fromSearchField))
                        triggerPendingTableReloadIfNecessary()
                    }
                } else {
                    logger.error("Model filtering failed!", thrown)
                    SwingUtilities.invokeLater {
                        host.table().isEnabled = true
                        host.updateStartInfoProperty()
                        host.table().setSpalten()
                        host.updateFilmData()
                        host.setSelectionUpdatesSuspended(false)
                        messageBus.publish(TableModelChangeEvent(false, fromSearchField))
                        triggerPendingTableReloadIfNecessary()
                    }
                }
            },
            decoratedPool
        )
    }

    private fun triggerPendingTableReloadIfNecessary() {
        if (!pendingTableReload) {
            return
        }

        val reloadFromSearchField = pendingTableReloadFromSearchField
        pendingTableReload = false
        pendingTableReloadFromSearchField = false
        loadTable(reloadFromSearchField)
    }

    private companion object {
        private val logger = LogManager.getLogger()
    }
}
