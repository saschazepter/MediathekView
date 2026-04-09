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

import mediathek.gui.messages.DownloadRateLimitChangedEvent
import mediathek.gui.messages.ParallelDownloadNumberChangedEvent
import mediathek.tool.ApplicationConfiguration
import mediathek.tool.MessageBus.messageBus
import net.engio.mbassy.listener.Handler
import net.miginfocom.layout.AC
import net.miginfocom.layout.CC
import net.miginfocom.layout.LC
import net.miginfocom.swing.MigLayout
import javax.swing.*
import javax.swing.border.TitledBorder

/**
 * @author Christian Franzke
 */
class DownloadsConfigPanel : JPanel() {
    private fun scheduleDownloadRateLimitChangedEvent() {
        if (downloadRateLimitChangeTimer.isRunning) {
            downloadRateLimitChangeTimer.restart()
        } else {
            downloadRateLimitChangeTimer.start()
        }
    }

    private fun setupDownloadRateLimitCheckBox() {
        val config = ApplicationConfiguration.getConfiguration()
        val active = config.getBoolean(
            ApplicationConfiguration.DownloadRateLimiter.ACTIVE, false)
        cbMaxBandwidth.isSelected = active
            cbMaxBandwidth.addActionListener {
            config.setProperty(ApplicationConfiguration.DownloadRateLimiter.ACTIVE, cbMaxBandwidth.isSelected)
            downloadRateLimitChangeTimer.stop()
            fireDownloadRateLimitChangedEvent()
        }
    }

    private fun fireDownloadRateLimitChangedEvent() {
        val downloadLimit = (spinnerMaxBandwidth.value as Number).toInt()
        ApplicationConfiguration.getConfiguration()
            .setProperty(ApplicationConfiguration.DownloadRateLimiter.LIMIT, downloadLimit)
        val evt = DownloadRateLimitChangedEvent()
        evt.newLimit = downloadLimit
        evt.active = cbMaxBandwidth.isSelected
        messageBus.publishAsync(evt)
    }

    private fun setupDownloadRateLimitSpinner() {
        spinnerMaxBandwidth.putClientProperty("JComponent.roundRect", true)
        spinnerMaxBandwidth.model = SpinnerNumberModel(0, 0, 1048576, 1)
        spinnerMaxBandwidth.toolTipText =
            "<html>Bandbreitenbegrenzung eines Downloads in XX Kilobytes pro Sekunde.\n<b><br><u>WICHTIG:</u><br>ENTWEDER<br>den Wert \u00fcber die Pfeiltasten \u00e4ndern<br>ODER<br>Zahlen eingeben UND ENTER-Taste dr\u00fccken!</b>\n</html>" //NON-NLS

        //restore spinner setting from config
        val oldDownloadLimit =
            ApplicationConfiguration.getConfiguration().getLong(ApplicationConfiguration.DownloadRateLimiter.LIMIT, 0)
        spinnerMaxBandwidth.value = oldDownloadLimit.toInt()
        spinnerMaxBandwidth.addChangeListener { scheduleDownloadRateLimitChangedEvent() }
    }

    private fun setupNumDownloadsSpinner() {
        val config = ApplicationConfiguration.getConfiguration()
        spinnerNumDownloads.putClientProperty("JComponent.roundRect", true)
        spinnerNumDownloads.model = SpinnerNumberModel(1, 1, 9, 1)
        spinnerNumDownloads.value = config.getInt(ApplicationConfiguration.DOWNLOAD_MAX_SIMULTANEOUS_NUM, 1)
        spinnerNumDownloads.addChangeListener {
            val maxNumDownloads = (spinnerNumDownloads.model.value as Number).toInt()
            config.setProperty(ApplicationConfiguration.DOWNLOAD_MAX_SIMULTANEOUS_NUM, maxNumDownloads)
            messageBus.publishAsync(ParallelDownloadNumberChangedEvent())
        }
    }

    private fun setupBrDirectDownloadCheckBox() {
        val config = ApplicationConfiguration.getConfiguration()
        cbUseBrDirectDownload.isSelected =
            config.getBoolean(ApplicationConfiguration.DOWNLOAD_USE_CDN_AWARE_DIRECT_DOWNLOAD, true)
        cbUseBrDirectDownload.addActionListener {
            config.setProperty(
                ApplicationConfiguration.DOWNLOAD_USE_CDN_AWARE_DIRECT_DOWNLOAD,
                cbUseBrDirectDownload.isSelected
            )
        }
    }

    @Suppress("UNUSED_PARAMETER")
    @Handler
    private fun handleParallelDownloadNumberChange(e: ParallelDownloadNumberChangedEvent) {
        SwingUtilities.invokeLater {
            val maxNumDownloads = ApplicationConfiguration.getConfiguration()
                .getInt(ApplicationConfiguration.DOWNLOAD_MAX_SIMULTANEOUS_NUM, 1)
            spinnerNumDownloads.value = maxNumDownloads
        }
    }

    private fun initComponents() {
        layout = MigLayout(
            LC().insets("0").hideMode(3).fillX(),
            AC().grow().fill(),
            AC()
                .gap("0")
                .gap("rel")
        )

        val downloadsPanel = JPanel().apply {
            border = TitledBorder("Downloads")
            layout = MigLayout(
                LC().insets("0").hideMode(3).align("center", "center"),
                // columns
                AC()
                    .align("center").gap()
                    .align("label").gap()
                    .align("left").gap()
                    .align("center"),
                // rows
                AC()
                    .gap()
            )

            add(JLabel("gleichzeitig:"), CC().cell(1, 0))
            add(spinnerNumDownloads, CC().cell(2, 0).width("80:100")) //NON-NLS
            add(cbMaxBandwidth, CC().cell(0, 1))

            add(JLabel("max. Bandbreite:"), CC().cell(1, 1))
            add(spinnerMaxBandwidth, CC().cell(2, 1).width("80:100")) //NON-NLS

            add(JLabel("KiB/s"), CC().cell(3, 1))
        }

        cbUseBrDirectDownload.border = BorderFactory.createEmptyBorder(0, 4, 0, 0)

        add(downloadsPanel, CC().cell(0, 0).growX())
        add(cbUseBrDirectDownload, CC().cell(0, 1).alignX("left"))
    }

    private val spinnerNumDownloads: JSpinner = JSpinner()
    private val cbMaxBandwidth: JCheckBox = JCheckBox()
    private val cbUseBrDirectDownload: JCheckBox = JCheckBox("CDN-aware Downloader verwenden")
    private val spinnerMaxBandwidth: JSpinner = JSpinner()
    private val downloadRateLimitChangeTimer = Timer(300) { fireDownloadRateLimitChangedEvent() }.apply {
        isRepeats = false
    }

    init {
        initComponents()
        cbMaxBandwidth.toolTipText = "Bandbreitenbegrenzung aktiviert?"
        cbUseBrDirectDownload.toolTipText =
            "<html>Aktivieren Sie dies falls direkte Downloads häufig fehlerhaft sind.<br/>" +
                    "Dies kann jedoch erheblich langsamer als ein normaler Download sein.<br/><br/>" +
                    "Das Umschalten wird nur <b>vor</b> Downloadstart berücksichtigt!" +
                    "</html>"
        setupNumDownloadsSpinner()
        setupDownloadRateLimitCheckBox()
        setupDownloadRateLimitSpinner()
        setupBrDirectDownloadCheckBox()
        messageBus.subscribe(this)
    }
}
