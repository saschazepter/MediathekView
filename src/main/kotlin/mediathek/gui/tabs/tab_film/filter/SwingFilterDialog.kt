/*
 * Copyright (c) 2025-2026 derreisende77.
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

package mediathek.gui.tabs.tab_film.filter

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.FilterList
import ca.odell.glazedlists.swing.GlazedListsSwing
import com.jidesoft.swing.CheckBoxList
import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import mediathek.config.Daten
import mediathek.config.Konstanten
import mediathek.controller.SenderFilmlistLoadApprover
import mediathek.filmeSuchen.ListenerFilmeLaden
import mediathek.filmeSuchen.ListenerFilmeLadenEvent
import mediathek.gui.messages.BaseEvent
import mediathek.gui.messages.FilterZeitraumEvent
import mediathek.gui.messages.ReloadTableDataEvent
import mediathek.gui.messages.TableModelChangeEvent
import mediathek.gui.tabs.tab_film.filter_selection.FilterSelectionComboBoxModel
import mediathek.mainwindow.MediathekGui
import mediathek.swing.IconUtils
import mediathek.tool.*
import mediathek.tool.MessageBus.messageBus
import net.engio.mbassy.listener.Handler
import org.apache.commons.configuration2.Configuration
import org.apache.commons.configuration2.sync.LockMode
import org.apache.logging.log4j.LogManager
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import java.awt.Component
import java.awt.Dimension
import java.awt.Window
import java.awt.event.ActionEvent
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.awt.event.KeyEvent
import java.util.*
import javax.swing.*
import javax.swing.event.ListDataEvent
import javax.swing.event.ListDataListener

class SwingFilterDialog internal constructor(
    owner: Window,
    private val filterSelectionComboBoxModel: FilterSelectionComboBoxModel,
    private val filterToggleButton: JToggleButton,
    private val filterConfig: FilterConfiguration,
    private val dependencies: Dependencies
) : SwingFilterDialogView(owner, filterSelectionComboBoxModel) {

    constructor(
        owner: Window,
        filterSelectionComboBoxModel: FilterSelectionComboBoxModel,
        filterToggleButton: JToggleButton,
        filterConfig: FilterConfiguration
    ) : this(owner, filterSelectionComboBoxModel, filterToggleButton, filterConfig, DefaultDependencies)

    interface Dependencies {
        fun subscribe(subscriber: Any)
        fun unsubscribe(subscriber: Any)
        fun publish(event: BaseEvent)
        fun addFilmeLadenListener(listener: ListenerFilmeLaden)
        fun removeFilmeLadenListener(listener: ListenerFilmeLaden)
        fun senderList(): EventList<String>
        fun getThemen(senders: Collection<String>): List<String>
    }

    private object DefaultDependencies : Dependencies {
        private val daten: Daten
            get() = Daten.getInstance()

        override fun subscribe(subscriber: Any) {
            messageBus.subscribe(subscriber)
        }

        override fun unsubscribe(subscriber: Any) {
            messageBus.unsubscribe(subscriber)
        }

        override fun publish(event: BaseEvent) {
            messageBus.publish(event)
        }

        override fun addFilmeLadenListener(listener: ListenerFilmeLaden) {
            daten.filmeLaden.addAdListener(listener)
        }

        override fun removeFilmeLadenListener(listener: ListenerFilmeLaden) {
            daten.filmeLaden.removeAdListener(listener)
        }

        override fun senderList(): EventList<String> {
            return FilterList(daten.allSendersList, SenderFilmlistLoadApprover::isApproved)
        }

        override fun getThemen(senders: Collection<String>): List<String> {
            return daten.listeFilmeNachBlackList.getThemen(senders)
        }
    }

    companion object {
        private const val CHECKBOX_RELOAD_DEBOUNCE_MS = 450L
        private const val ZEITRAUM_RELOAD_DEBOUNCE_MS = 450L
        private const val CONFIG_SENDERLIST_VERTICAL_WRAP = "senderlist.vertical_wrap"
        private const val STR_NEW_FILTER = "Neuen Filter anlegen"
        private const val STR_DELETE_CURRENT_FILTER = "Aktuellen Filter löschen"
        private const val STR_RESET_THEMA = "Thema zurücksetzen"
        private const val STR_RENAME_FILTER = "Filter umbenennen"
        private val logger = LogManager.getLogger()
    }

    private val config: Configuration = ApplicationConfiguration.getConfiguration()
    private val uiScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    private val sourceThemaList: EventList<String> = BasicEventList()
    private val renameFilterAction = RenameFilterAction()
    private val deleteCurrentFilterAction = DeleteCurrentFilterAction()
    private val addNewFilterAction = AddNewFilterAction()
    private val resetCurrentFilterAction = ResetCurrentFilterAction()
    private val checkBoxBindings = createCheckBoxBindings()
    private val senderCheckBoxList = SenderCheckBoxList()
    private val filmLengthRangeSlider = filmLengthSlider as FilmLengthSlider
    private val filmeLadenListener = FilmeLadenListener()
    private val filterSelectionDataListener = FilterSelectionDataListener()
    private var checkboxReloadJob: Job? = null
    private var zeitraumReloadJob: Job? = null
    private val managedActions by lazy {
        listOf<Action>(
            renameFilterAction,
            addNewFilterAction,
            resetCurrentFilterAction
        )
    }
    private val managedComponents by lazy {
        listOf<JComponent>(
            cboxFilterSelection,
            btnSplit,
            btnResetThema,
            label3,
            senderCheckBoxList,
            label4,
            jcbThema,
            label5,
            label7,
            lblMinFilmLengthValue,
            lblMaxFilmLengthValue,
            filmLengthRangeSlider,
            spZeitraum,
            label1,
            label2
        ) + checkBoxBindings.map(CheckBoxBinding::checkBox)
    }
    private val restoreOperations by lazy {
        listOf(
            ::restoreCheckBoxSettings,
            ::restoreThemaSelection,
            senderCheckBoxList::restoreFilterConfig,
            { filmLengthRangeSlider.restoreFilterConfig(filterConfig) },
            { spZeitraum.restoreFilterConfig(filterConfig) }
        )
    }

    init {
        scpSenderList.setViewportView(senderCheckBoxList)

        configureComponents()
        setupInteraction()
        restoreState()
        registerExternalListeners()
    }

    override fun dispose() {
        checkboxReloadJob?.cancel()
        zeitraumReloadJob?.cancel()
        uiScope.cancel()
        filterSelectionComboBoxModel.removeListDataListener(filterSelectionDataListener)
        dependencies.removeFilmeLadenListener(filmeLadenListener)
        dependencies.unsubscribe(this)
        super.dispose()
    }

    private fun configureComponents() {
        setupRoundControls()
        btnSplit.icon = SVGIconUtilities.createSVGIcon("icons/fontawesome/ellipsis-vertical.svg")
        populateSplitButton()
        ToggleVisibilityKeyHandler(this).installHandler(filterToggleButton.action)
        setupButtons()
    }

    private fun setupInteraction() {
        setupCheckBoxes()
        setupThemaComboBox()
        setupFilmLengthSlider()
        setupZeitraumSpinner()
    }

    private fun restoreState() {
        restoreConfigSettings()
        restoreWindowSizeFromConfig()
        restoreDialogVisibility()
    }

    private fun registerExternalListeners() {
        filterSelectionComboBoxModel.addListDataListener(filterSelectionDataListener)
        addComponentListener(FilterDialogComponentListener())
        dependencies.subscribe(this)
        dependencies.addFilmeLadenListener(filmeLadenListener)
    }

    private fun setupRoundControls() {
        cboxFilterSelection.putClientProperty("JComponent.roundRect", true)
        cboxFilterSelection.maximumSize = Dimension(500, 100)
        jcbThema.putClientProperty("JComponent.roundRect", true)
        spZeitraum.putClientProperty("JComponent.roundRect", true)
    }

    private fun populateSplitButton() {
        btnSplit.add(renameFilterAction)
        btnSplit.add(addNewFilterAction)
        btnSplit.add(deleteCurrentFilterAction)
        btnSplit.addSeparator()
        btnSplit.add(resetCurrentFilterAction)
    }

    private fun setupButtons() {
        checkDeleteCurrentFilterButtonState()
        btnResetThema.action = ResetThemaAction()
    }

    private fun setupCheckBoxes() {
        checkBoxBindings.forEach { binding ->
            binding.checkBox.addActionListener {
                binding.selectedStateWriter(binding.checkBox.isSelected)
                requestDebouncedCheckboxReload()
            }
        }
    }

    private fun requestDebouncedCheckboxReload() {
        checkboxReloadJob = restartDebouncedJob(checkboxReloadJob, CHECKBOX_RELOAD_DEBOUNCE_MS) {
            publishReloadTableDataEvent()
        }
    }

    private fun publishReloadTableDataEvent() {
        dependencies.publish(ReloadTableDataEvent())
    }

    private fun createCheckBoxBindings(): List<CheckBoxBinding> {
        return listOf(
            CheckBoxBinding(cbShowNewOnly, filterConfig::isShowNewOnly, filterConfig::setShowNewOnly),
            CheckBoxBinding(cbShowBookMarkedOnly, filterConfig::isShowBookMarkedOnly, filterConfig::setShowBookMarkedOnly),
            CheckBoxBinding(cbShowOnlyHq, filterConfig::isShowHighQualityOnly, filterConfig::setShowHighQualityOnly),
            CheckBoxBinding(cbShowSubtitlesOnly, filterConfig::isShowSubtitlesOnly, filterConfig::setShowSubtitlesOnly),
            CheckBoxBinding(cbShowOnlyLivestreams, filterConfig::isShowLivestreamsOnly, filterConfig::setShowLivestreamsOnly),
            CheckBoxBinding(cbShowUnseenOnly, filterConfig::isShowUnseenOnly, filterConfig::setShowUnseenOnly),
            CheckBoxBinding(cbDontShowAbos, filterConfig::isDontShowAbos, filterConfig::setDontShowAbos),
            CheckBoxBinding(cbDontShowSignLanguage, filterConfig::isDontShowSignLanguage, filterConfig::setDontShowSignLanguage),
            CheckBoxBinding(cbDontShowGeoblocked, filterConfig::isDontShowGeoblocked, filterConfig::setDontShowGeoblocked),
            CheckBoxBinding(cbDontShowTrailers, filterConfig::isDontShowTrailers, filterConfig::setDontShowTrailers),
            CheckBoxBinding(cbDontShowAudioVersions, filterConfig::isDontShowAudioVersions, filterConfig::setDontShowAudioVersions),
            CheckBoxBinding(cbDontShowDuplicates, filterConfig::isDontShowDuplicates, filterConfig::setDontShowDuplicates)
        )
    }

    private fun restoreCheckBoxSettings() {
        checkBoxBindings.forEach { binding ->
            binding.checkBox.isSelected = binding.selectedStateReader()
        }
    }

    private fun setupFilmLengthSlider() {
        val slider = filmLengthRangeSlider

        lblMinFilmLengthValue.text = slider.lowValue.toString()
        lblMaxFilmLengthValue.text = slider.highValueText

        slider.addChangeListener {
            lblMinFilmLengthValue.text = slider.lowValue.toString()
            lblMaxFilmLengthValue.text = slider.highValueText

            if (!slider.valueIsAdjusting) {
                filterConfig.setFilmLengthMin(slider.lowValue.toDouble())
                filterConfig.setFilmLengthMax(slider.highValue.toDouble())
                publishReloadTableDataEvent()
            }
        }
    }

    private fun updateThemaComboBox() {
        val currentThema = jcbThema.selectedItem as? String
        val tempThemaList = dependencies.getThemen(filterConfig.checkedChannels.toList())

        sourceThemaList.withWriteLock {
            sourceThemaList.clear()
            sourceThemaList.addAll(tempThemaList)

            if (!currentThema.isNullOrEmpty() && !sourceThemaList.contains(currentThema)) {
                sourceThemaList.add(currentThema)
            }
        }

        jcbThema.selectedItem = currentThema
    }

    private fun createPopupMenu(): JPopupMenu {
        return JPopupMenu().apply {
            add(ResetThemaButtonAction())
        }
    }

    private fun setupThemaComboBox() {
        jcbThema.componentPopupMenu = createPopupMenu()

        val model = GlazedListsSwing.eventComboBoxModel(EventListWithEmptyFirstEntry(sourceThemaList))
        jcbThema.model = model

        val thema = filterConfig.thema
        sourceThemaList.withWriteLock {
            if (!sourceThemaList.contains(thema)) {
                sourceThemaList.add(thema)
            }
        }

        jcbThema.selectedItem = thema
        jcbThema.addActionListener {
            (jcbThema.selectedItem as? String)?.let(filterConfig::setThema)
            publishReloadTableDataEvent()
        }
    }

    private fun setupZeitraumSpinner() {
        runCatching {
            spZeitraum.restoreFilterConfig(filterConfig)
            spZeitraum.installFilterConfigurationChangeListener(filterConfig)
            spZeitraum.addChangeListener { requestDebouncedZeitraumReload() }
        }.onFailure {
            logger.error("Failed to setup zeitraum spinner", it)
        }
    }

    private fun requestDebouncedZeitraumReload() {
        zeitraumReloadJob = restartDebouncedJob(zeitraumReloadJob, ZEITRAUM_RELOAD_DEBOUNCE_MS) {
            resetSenderSelection()
            dependencies.publish(FilterZeitraumEvent())
        }
    }

    private fun restartDebouncedJob(job: Job?, delayMs: Long, action: suspend () -> Unit): Job {
        job?.cancel()
        return uiScope.launch {
            delay(delayMs)
            action()
        }
    }

    private fun checkDeleteCurrentFilterButtonState() {
        deleteCurrentFilterAction.isEnabled = filterConfig.availableFilterCount > 1
    }

    private fun restoreConfigSettings() {
        restoreOperations.forEach { it.invoke() }
    }

    private fun restoreThemaSelection() {
        jcbThema.selectedItem = filterConfig.thema
    }

    private fun applyEnabledState(enabled: Boolean) {
        managedActions.forEach { it.isEnabled = enabled }
        managedComponents.forEach { it.isEnabled = enabled }
    }

    override fun setEnabled(enabled: Boolean) {
        super.setEnabled(enabled)
        applyEnabledState(enabled)
    }

    fun resetSenderSelection() {
        senderCheckBoxList.selectNone()
    }

    @Handler
    private fun handleTableModelChangeEvent(event: TableModelChangeEvent) {
        uiScope.launch {
            val enabled = !event.active
            isEnabled = enabled

            if (event.active) {
                deleteCurrentFilterAction.isEnabled = false
            } else {
                checkDeleteCurrentFilterButtonState()
            }
        }
    }

    private fun restoreDialogVisibility() {
        isVisible = config.getBoolean(ApplicationConfiguration.FilterDialog.VISIBLE, false)
    }

    private fun restoreWindowSizeFromConfig() {
        try {
            config.withLock(LockMode.READ) {
                setBounds(
                    getInt(ApplicationConfiguration.FilterDialog.X),
                    getInt(ApplicationConfiguration.FilterDialog.Y),
                    getInt(ApplicationConfiguration.FilterDialog.WIDTH),
                    getInt(ApplicationConfiguration.FilterDialog.HEIGHT)
                )
            }
        } catch (_: NoSuchElementException) {
            // do not restore anything
        }
    }

    class ToggleVisibilityKeyHandler(dlg: JDialog) {
        companion object {
            private const val TOGGLE_FILTER_VISIBILITY = "toggle_dialog_visibility"
        }

        private val rootPane: JRootPane = dlg.rootPane

        fun installHandler(action: Action?) {
            rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
                .put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), TOGGLE_FILTER_VISIBILITY)
            rootPane.actionMap.put(TOGGLE_FILTER_VISIBILITY, action)
        }
    }

    private inner class ResetCurrentFilterAction : AbstractAction() {
        init {
            putValue(SMALL_ICON, IconUtils.of(FontAwesomeSolid.RECYCLE))
            putValue(SHORT_DESCRIPTION, "Aktuellen Filter zurücksetzen")
            putValue(NAME, "Aktuellen Filter zurücksetzen...")
        }

        override fun actionPerformed(e: ActionEvent?) {
            val result = JOptionPane.showConfirmDialog(
                MediathekGui.ui(),
                "Sind Sie sicher, dass Sie den Filter zurücksetzen möchten?",
                "Filter zurücksetzen",
                JOptionPane.YES_NO_OPTION
            )
            if (result == JOptionPane.YES_OPTION) {
                filterConfig.clearCurrentFilter()
                restoreConfigSettings()
            }
        }
    }

    private inner class SenderCheckBoxList : CheckBoxList() {
        private val miVerticalWrap = JCheckBoxMenuItem("Senderliste vertikal umbrechen", false)

        init {
            visibleRowCount = -1
            setupSenderList()
            restoreVerticalWrapState()
        }

        private fun restoreVerticalWrapState() {
            if (config.getBoolean(CONFIG_SENDERLIST_VERTICAL_WRAP, true)) {
                miVerticalWrap.doClick()
            }
        }

        private fun setupSenderList() {
            model = GlazedListsSwing.eventListModel(dependencies.senderList())
            checkBoxListSelectionModel.addListSelectionListener { event ->
                if (!event.valueIsAdjusting) {
                    filterConfig.setCheckedChannels(selectedSenders)
                    updateThemaComboBox()
                    publishReloadTableDataEvent()
                }
            }

            setupContextMenu()
        }

        private fun setupContextMenu() {
            componentPopupMenu = JPopupMenu().apply {
                add(JMenuItem("Alle Senderfilter zurücksetzen").apply {
                    addActionListener { selectNone() }
                })
                addSeparator()
                add(miVerticalWrap.apply {
                    addActionListener {
                        val selected = isSelected
                        config.withLock(LockMode.WRITE) {
                            setProperty(CONFIG_SENDERLIST_VERTICAL_WRAP, selected)
                        }
                        layoutOrientation = if (selected) VERTICAL_WRAP else VERTICAL
                        repaint()
                    }
                })
            }
        }

        private val selectedSenders: List<String>
            get() {
                val newSelectedSenderList = mutableListOf<String>()
                val senderListModel = model
                val selectionModel = checkBoxListSelectionModel
                for (index in 0 until senderListModel.size) {
                    if (selectionModel.isSelectedIndex(index)) {
                        newSelectedSenderList += senderListModel.getElementAt(index).toString()
                    }
                }
                return newSelectedSenderList
            }

        fun restoreFilterConfig() {
            val checkedSenders = filterConfig.checkedChannels
            val selectionModel = checkBoxListSelectionModel
            val senderListModel = model

            selectionModel.valueIsAdjusting = true
            try {
                selectNone()
                for (index in 0 until senderListModel.size) {
                    val item = senderListModel.getElementAt(index) as String
                    if (checkedSenders.contains(item)) {
                        selectionModel.addSelectionInterval(index, index)
                    }
                }
            } finally {
                selectionModel.valueIsAdjusting = false
            }
        }

    }

    private inner class AddNewFilterAction : AbstractAction() {
        init {
            putValue(SMALL_ICON, SVGIconUtilities.createSVGIcon("icons/fontawesome/plus.svg"))
            putValue(SHORT_DESCRIPTION, STR_NEW_FILTER)
            putValue(NAME, "$STR_NEW_FILTER...")
        }

        override fun actionPerformed(e: ActionEvent?) {
            val newFilterName = JOptionPane.showInputDialog(
                MediathekGui.ui(),
                "Filtername:",
                STR_NEW_FILTER,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                "Filter ${filterConfig.availableFilters.size + 1}"
            ) as? String

            if (newFilterName != null) {
                filterConfig.findFilterForName(newFilterName).ifPresentOrElse(
                    {
                        JOptionPane.showMessageDialog(
                            MediathekGui.ui(),
                            "Ein Filter mit dem gewählten Namen existiert bereits!",
                            STR_NEW_FILTER,
                            JOptionPane.ERROR_MESSAGE
                        )
                    },
                    {
                        val newFilter = FilterDTO(UUID.randomUUID(), newFilterName)
                        filterConfig.addNewFilter(newFilter)
                        checkDeleteCurrentFilterButtonState()
                        filterSelectionComboBoxModel.selectedItem = newFilter
                    }
                )
            }
        }

    }

    private inner class DeleteCurrentFilterAction : AbstractAction() {
        init {
            putValue(SMALL_ICON, SVGIconUtilities.createSVGIcon("icons/fontawesome/trash-can.svg"))
            putValue(SHORT_DESCRIPTION, STR_DELETE_CURRENT_FILTER)
            putValue(NAME, "$STR_DELETE_CURRENT_FILTER...")
        }

        override fun actionPerformed(e: ActionEvent?) {
            val result = JOptionPane.showConfirmDialog(
                MediathekGui.ui(),
                "Möchten Sie wirklich den aktuellen Filter löschen?",
                Konstanten.PROGRAMMNAME,
                JOptionPane.YES_NO_OPTION
            )
            if (result == JOptionPane.YES_OPTION) {
                filterConfig.deleteFilter(filterConfig.currentFilter)
                checkDeleteCurrentFilterButtonState()
            }
        }

    }

    private open inner class ResetThemaAction : AbstractAction() {
        init {
            putValue(SMALL_ICON, SVGIconUtilities.createSVGIcon("icons/fontawesome/trash-can.svg"))
            putValue(SHORT_DESCRIPTION, STR_RESET_THEMA)
        }

        override fun actionPerformed(e: ActionEvent?) {
            filterConfig.thema = ""
            jcbThema.selectedIndex = 0
        }

    }

    private inner class ResetThemaButtonAction : ResetThemaAction() {
        init {
            putValue(NAME, STR_RESET_THEMA)
        }
    }

    private inner class RenameFilterAction : AbstractAction() {
        init {
            putValue(SMALL_ICON, SVGIconUtilities.createSVGIcon("icons/fontawesome/pen-to-square.svg"))
            putValue(SHORT_DESCRIPTION, STR_RENAME_FILTER)
            putValue(NAME, "$STR_RENAME_FILTER...")
        }

        override fun actionPerformed(e: ActionEvent?) {
            val currentFilterName = filterConfig.currentFilter.name()
            val input = JOptionPane.showInputDialog(
                MediathekGui.ui(),
                "Neuer Name des Filters:",
                "Filter umbenennen",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                currentFilterName
            ) as? String ?: return

            if (input.isEmpty()) {
                JOptionPane.showMessageDialog(
                    MediathekGui.ui(),
                    "Filtername darf nicht leer sein!",
                    Konstanten.PROGRAMMNAME,
                    JOptionPane.ERROR_MESSAGE
                )
                logger.warn("Rename filter text was empty...doing nothing")
                return
            }

            val trimmedName = input.trim()
            if (trimmedName == currentFilterName) {
                logger.warn("New and old filter name are identical...doing nothing")
                return
            }

            filterConfig.findFilterForName(trimmedName).ifPresentOrElse(
                {
                    JOptionPane.showMessageDialog(
                        MediathekGui.ui(),
                        "Filter $trimmedName existiert bereits.\nAktion wird abgebrochen",
                        Konstanten.PROGRAMMNAME,
                        JOptionPane.ERROR_MESSAGE
                    )
                },
                {
                    config.withLock(LockMode.WRITE) {
                        val thema = filterConfig.thema
                        filterConfig.thema = ""
                        filterConfig.renameCurrentFilter(trimmedName)
                        filterConfig.thema = thema
                    }
                    logger.trace("Renamed filter \"{}\" to \"{}\"", currentFilterName, trimmedName)
                }
            )
        }

    }

    inner class FilterDialogComponentListener : ComponentAdapter() {
        override fun componentResized(event: ComponentEvent) {
            storeWindowPosition(event)
        }

        override fun componentMoved(event: ComponentEvent) {
            storeWindowPosition(event)
        }

        override fun componentShown(event: ComponentEvent) {
            storeDialogVisibility()
            filterToggleButton.isSelected = true
        }

        override fun componentHidden(event: ComponentEvent) {
            storeWindowPosition(event)
            storeDialogVisibility()
            filterToggleButton.isSelected = false
        }

        private fun storeDialogVisibility() {
            config.setProperty(ApplicationConfiguration.FilterDialog.VISIBLE, isVisible)
        }

        private fun storeWindowPosition(event: ComponentEvent) {
            val component: Component = event.component
            val dims = component.size
            val loc = component.location
            config.withLock(LockMode.WRITE) {
                setProperty(ApplicationConfiguration.FilterDialog.WIDTH, dims.width)
                setProperty(ApplicationConfiguration.FilterDialog.HEIGHT, dims.height)
                setProperty(ApplicationConfiguration.FilterDialog.X, loc.x)
                setProperty(ApplicationConfiguration.FilterDialog.Y, loc.y)
            }
        }
    }

    private inner class FilmeLadenListener : ListenerFilmeLaden() {
        override fun start(event: ListenerFilmeLadenEvent) {
            isEnabled = false
        }

        override fun fertig(event: ListenerFilmeLadenEvent) {
            updateThemaComboBox()
            isEnabled = true
        }
    }

    private inner class FilterSelectionDataListener : ListDataListener {
        override fun intervalAdded(event: ListDataEvent) = restoreConfigSettings()
        override fun intervalRemoved(event: ListDataEvent) = restoreConfigSettings()
        override fun contentsChanged(event: ListDataEvent) = restoreConfigSettings()
    }

    private data class CheckBoxBinding(
        val checkBox: JCheckBox,
        val selectedStateReader: () -> Boolean,
        val selectedStateWriter: (Boolean) -> Unit
    )
}
