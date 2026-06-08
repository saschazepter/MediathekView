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

package mediathek.gui.dialogEinstellungen.blacklist

import mediathek.config.Daten
import mediathek.config.Konstanten
import mediathek.config.MVColor
import mediathek.config.MVConfig
import mediathek.daten.blacklist.BlacklistRule
import mediathek.filmeSuchen.ListenerFilmeLaden
import mediathek.filmeSuchen.ListenerFilmeLadenEvent
import mediathek.gui.dialog.DialogHilfe
import mediathek.gui.messages.BlacklistAboSettingChangedEvent
import mediathek.gui.messages.BlacklistChangedEvent
import mediathek.gui.messages.BlacklistStartSettingChangedEvent
import mediathek.tool.*
import net.engio.mbassy.listener.Handler
import org.apache.logging.log4j.LogManager
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.util.regex.PatternSyntaxException
import javax.swing.DefaultComboBoxModel
import javax.swing.JMenuItem
import javax.swing.JOptionPane
import javax.swing.JPopupMenu
import javax.swing.JTextField
import javax.swing.RowFilter
import javax.swing.SwingUtilities
import javax.swing.UIManager
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.table.TableModel
import javax.swing.table.TableRowSorter
import javax.swing.table.TableStringConverter

class PanelBlacklist(
    private val daten: Daten,
    private val parentComponent: javax.swing.JFrame?,
    private val name: String,
) : PanelBlacklistBase() {
    var ok: Boolean = false

    private val tableModel = BlacklistRuleTableModel(daten.listeBlacklist)
    private val filmLoadListener = object : ListenerFilmeLaden() {
        override fun fertig(event: ListenerFilmeLadenEvent) {
            comboThemaLaden()
        }
    }
    private var listenersRegistered = false

    init {
        jButtonHilfe.icon = SVGIconUtilities.createSVGIcon("icons/fontawesome/circle-question.svg")

        jButtonAendern.isEnabled = jTableBlacklist.selectionModel.selectedItemsCount == 1

        jTableBlacklist.model = tableModel

        tableModel.addTableModelListener { jButtonTabelleLoeschen.isEnabled = tableModel.rowCount != 0 }
        jTableBlacklist.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                jButtonAendern.isEnabled = jTableBlacklist.selectionModel.selectedItemsCount == 1

                if (jTableBlacklist.selectionModel.selectedItemsCount == 0) {
                    resetRuleEntryFields()
                }
            }
        }

        jCheckBoxGeo.addActionListener {
            ApplicationConfiguration.getInstance().blacklistDoNotShowGeoblockedFilms = jCheckBoxGeo.isSelected
            notifyBlacklistChanged()
        }

        initPanelState()
        initBehavior()

        setupTableFilter()

        lblNumEntries.text = jTableBlacklist.rowCount.toString()
        jTableBlacklist.model.addTableModelListener { lblNumEntries.text = jTableBlacklist.rowCount.toString() }
    }

    override fun addNotify() {
        super.addNotify()
        registerListeners()
    }

    override fun removeNotify() {
        unregisterListeners()
        super.removeNotify()
    }

    private fun registerListeners() {
        if (listenersRegistered) {
            return
        }
        MessageBus.messageBus.subscribe(this)
        daten.filmeLaden.addAdListener(filmLoadListener)
        listenersRegistered = true
    }

    private fun unregisterListeners() {
        if (!listenersRegistered) {
            return
        }
        MessageBus.messageBus.unsubscribe(this)
        daten.filmeLaden.removeAdListener(filmLoadListener)
        listenersRegistered = false
    }

    private fun setupTableFilter() {
        val sorter = TableRowSorter(tableModel)
        sorter.stringConverter = object : TableStringConverter() {
            override fun toString(model: TableModel, row: Int, column: Int): String =
                model.getValueAt(row, column).toString().lowercase()
        }
        jTableBlacklist.rowSorter = sorter
        btnFilterTable.addActionListener {
            val text = tfFilter.text
            if (text.isEmpty()) {
                sorter.rowFilter = null
                GuiFunktionen.showErrorIndication(tfFilter, false)
            } else {
                try {
                    sorter.rowFilter = RowFilter.regexFilter(text.lowercase())
                    GuiFunktionen.showErrorIndication(tfFilter, false)
                } catch (exception: PatternSyntaxException) {
                    GuiFunktionen.showErrorIndication(tfFilter, true)
                    logger.error("Bad regex pattern", exception)
                }
            }
        }
    }

    private fun resetRuleEntryFields() {
        jTextFieldTitel.text = ""
        jTextFieldThemaTitel.text = ""
        jComboBoxThema.selectedItem = ""
        jComboBoxSender.selectedItem = ""
    }

    @Handler
    private fun handleBlacklistChangedEvent(@Suppress("UNUSED_PARAMETER") event: BlacklistChangedEvent) {
        SwingUtilities.invokeLater(::initPanelState)
    }

    @Handler
    private fun handleBlacklistStartSettingChangedEvent(event: BlacklistStartSettingChangedEvent) {
        if (event.sourceName != name) {
            SwingUtilities.invokeLater {
                jCheckBoxStart.isSelected = MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_START_ON).toBoolean()
            }
        }
    }

    @Handler
    private fun handleBlacklistAboSettingChangedEvent(event: BlacklistAboSettingChangedEvent) {
        if (event.sourceName != name) {
            SwingUtilities.invokeLater(::initPanelState)
        }
    }

    private fun initPanelState() {
        jCheckBoxAbo.isSelected = MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_AUCH_ABO).toBoolean()
        jCheckBoxStart.isSelected = MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_START_ON).toBoolean()

        jCheckBoxBlacklistEingeschaltet.isSelected =
            ApplicationConfiguration.getConfiguration().getBoolean(ApplicationConfiguration.BLACKLIST_IS_ON, false)

        jCheckBoxZukunftNichtAnzeigen.isSelected =
            MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_ZUKUNFT_NICHT_ANZEIGEN).toBoolean()

        jCheckBoxGeo.isSelected = ApplicationConfiguration.getInstance().blacklistDoNotShowGeoblockedFilms

        try {
            jSliderMinuten.value = MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_FILMLAENGE).toInt()
        } catch (_: Exception) {
            jSliderMinuten.value = 0
            MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_FILMLAENGE, "0")
        }

        tableModel.fireTableDataChanged()
    }

    private fun initBehavior() {
        jTableBlacklist.addMouseListener(BlacklistTableMouseHandler())
        jTableBlacklist.selectionModel.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                fillControlsWithRuleData()
            }
        }

        jRadioButtonWhitelist.isSelected = MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST).toBoolean()
        jRadioButtonWhitelist.addActionListener {
            MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST, jRadioButtonWhitelist.isSelected.toString())
            notifyBlacklistChanged()
        }
        jRadioButtonBlacklist.addActionListener {
            MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_IST_WHITELIST, jRadioButtonWhitelist.isSelected.toString())
            notifyBlacklistChanged()
        }
        jCheckBoxZukunftNichtAnzeigen.addActionListener {
            MVConfig.add(
                MVConfig.Configs.SYSTEM_BLACKLIST_ZUKUNFT_NICHT_ANZEIGEN,
                jCheckBoxZukunftNichtAnzeigen.isSelected.toString(),
            )
            notifyBlacklistChanged()
        }
        jCheckBoxAbo.addActionListener {
            MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_AUCH_ABO, jCheckBoxAbo.isSelected.toString())
            MessageBus.messageBus.publishAsync(BlacklistAboSettingChangedEvent(name))
        }
        jCheckBoxStart.addActionListener {
            MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_START_ON, jCheckBoxStart.isSelected.toString())
            MessageBus.messageBus.publishAsync(BlacklistStartSettingChangedEvent(name))
        }
        jCheckBoxBlacklistEingeschaltet.addActionListener {
            ApplicationConfiguration.getConfiguration()
                .setProperty(ApplicationConfiguration.BLACKLIST_IS_ON, jCheckBoxBlacklistEingeschaltet.isSelected)
            notifyBlacklistChanged()
        }
        jButtonHinzufuegen.addActionListener { onAddBlacklistRule() }
        jButtonAendern.addActionListener { onChangeBlacklistRule() }
        jButtonHilfe.addActionListener {
            DialogHilfe(parentComponent, true, GetFile.getHilfeSuchen(Konstanten.PFAD_HILFETEXT_BLACKLIST)).isVisible = true
        }
        jButtonTabelleLoeschen.addActionListener {
            val result = JOptionPane.showConfirmDialog(
                parentComponent,
                "<html>Möchten Sie wirklich <b>alle Regeln</b> dauerhaft löschen?</html>",
                "Blacklist Regeln",
                JOptionPane.YES_NO_OPTION,
            )
            if (result == JOptionPane.OK_OPTION) {
                tableModel.removeAll()
            }
        }
        jComboBoxSender.addActionListener { comboThemaLaden() }

        val documentListener = object : DocumentListener {
            override fun insertUpdate(event: DocumentEvent) = validatePatternInput()
            override fun removeUpdate(event: DocumentEvent) = validatePatternInput()
            override fun changedUpdate(event: DocumentEvent) = validatePatternInput()

            private fun validatePatternInput() {
                validatePatternInput(jTextFieldThemaTitel)
                validatePatternInput(jTextFieldTitel)
            }

            private fun validatePatternInput(textField: JTextField) {
                val text = textField.text
                if (Filter.isPattern(text)) {
                    textField.foreground = MVColor.getRegExPatternColor()
                    GuiFunktionen.showErrorIndication(textField, Filter.makePatternNoCache(text) == null)
                } else {
                    GuiFunktionen.showErrorIndication(textField, false)
                    textField.foreground = UIManager.getColor("TextField.foreground")
                }
            }
        }
        jTextFieldTitel.document.addDocumentListener(documentListener)
        jTextFieldThemaTitel.document.addDocumentListener(documentListener)

        try {
            jSliderMinuten.value = MVConfig.get(MVConfig.Configs.SYSTEM_BLACKLIST_FILMLAENGE).toInt()
        } catch (_: Exception) {
            jSliderMinuten.value = 0
            MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_FILMLAENGE, "0")
        }
        updateMinimumLengthText()
        jSliderMinuten.addChangeListener {
            updateMinimumLengthText()
            if (!jSliderMinuten.valueIsAdjusting) {
                MVConfig.add(MVConfig.Configs.SYSTEM_BLACKLIST_FILMLAENGE, jSliderMinuten.value.toString())
                notifyBlacklistChanged()
            }
        }

        jComboBoxSender.model = SenderListComboBoxModel()
        comboThemaLaden()

        var handler = TextCopyPasteHandler(jTextFieldThemaTitel)
        jTextFieldThemaTitel.componentPopupMenu = handler.getPopupMenu()

        handler = TextCopyPasteHandler(jTextFieldTitel)
        jTextFieldTitel.componentPopupMenu = handler.getPopupMenu()
    }

    private fun updateMinimumLengthText() {
        jTextFieldMinuten.text = if (jSliderMinuten.value == 0) {
            "alles"
        } else {
            jSliderMinuten.value.toString()
        }
    }

    private fun onChangeBlacklistRule() {
        val sender = requireNotNull(jComboBoxSender.selectedItem).toString()
        val topic = requireNotNull(jComboBoxThema.selectedItem).toString()
        val title = jTextFieldTitel.text.trim()
        val topicTitle = jTextFieldThemaTitel.text.trim()
        if (sender.isNotEmpty() || topic.isNotEmpty() || title.isNotEmpty() || topicTitle.isNotEmpty()) {
            val selectedTableRow = jTableBlacklist.selectedRow
            if (selectedTableRow != -1) {
                val modelIndex = jTableBlacklist.convertRowIndexToModel(selectedTableRow)
                tableModel.updateRule(modelIndex, BlacklistRule(sender, topic, title, topicTitle))
            }
        }
    }

    private fun notifyBlacklistChanged() {
        daten.listeBlacklist.filterListe()
        MessageBus.messageBus.publishAsync(BlacklistChangedEvent())
    }

    private fun comboThemaLaden() {
        val filterSender = requireNotNull(jComboBoxSender.selectedItem).toString()

        val topics = daten.listeFilme.getThemen(filterSender)
        val model = DefaultComboBoxModel<String>()
        model.addElement("")
        for (topic in topics) {
            model.addElement(topic)
        }
        jComboBoxThema.model = model
    }

    private fun fillControlsWithRuleData() {
        val selectedTableRow = jTableBlacklist.selectedRow
        if (selectedTableRow != -1) {
            val modelIndex = jTableBlacklist.convertRowIndexToModel(selectedTableRow)
            val rule = tableModel.getRule(modelIndex)
            jComboBoxSender.selectedItem = rule.sender
            jComboBoxThema.selectedItem = rule.thema
            jTextFieldTitel.text = rule.titel
            jTextFieldThemaTitel.text = rule.thema_titel
        }
    }

    private fun onAddBlacklistRule() {
        val sender = requireNotNull(jComboBoxSender.selectedItem).toString()
        val topic = requireNotNull(jComboBoxThema.selectedItem).toString()
        val title = jTextFieldTitel.text.trim()
        val topicTitle = jTextFieldThemaTitel.text.trim()

        if (sender.isNotEmpty() || topic.isNotEmpty() || title.isNotEmpty() || topicTitle.isNotEmpty()) {
            val rule = BlacklistRule(sender, topic, title, topicTitle)
            if (!tableModel.contains(rule)) {
                tableModel.addRule(rule)
                resetRuleEntryFields()
            } else {
                val message = """
                    Es existiert bereits eine gleichlautende Regel.
                    Es dürfen keine Duplikate in der Liste vorkommen.
                """.trimIndent()
                JOptionPane.showMessageDialog(this, message, Konstanten.PROGRAMMNAME, JOptionPane.ERROR_MESSAGE)
            }
        }
    }

    private inner class BlacklistTableMouseHandler : MouseAdapter() {
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

        private fun onRemoveBlacklistRules() {
            val selectedIndices = jTableBlacklist.selectionModel.selectedIndices
            if (selectedIndices.size == 1) {
                val modelIndex = jTableBlacklist.convertRowIndexToModel(selectedIndices[0])
                tableModel.removeRow(modelIndex)
            } else {
                val rules = selectedIndices.map { selectedRow ->
                    val modelIndex = jTableBlacklist.convertRowIndexToModel(selectedRow)
                    tableModel.getRule(modelIndex)
                }
                tableModel.removeRules(rules)
            }
        }

        private fun showMenu(event: MouseEvent) {
            val row = jTableBlacklist.rowAtPoint(event.point)
            if (row == -1) {
                return
            }
            if (!jTableBlacklist.isRowSelected(row)) {
                jTableBlacklist.selectionModel.setSelectionInterval(row, row)
            }

            val menu = JPopupMenu()
            val menuText = if (jTableBlacklist.selectedRowCount > 1) "Zeilen löschen" else "Zeile löschen"
            val item = JMenuItem(menuText)
            item.addActionListener { onRemoveBlacklistRules() }
            menu.add(item)
            menu.show(event.component, event.x, event.y)
        }
    }

    private companion object {
        private val logger = LogManager.getLogger()
    }
}
