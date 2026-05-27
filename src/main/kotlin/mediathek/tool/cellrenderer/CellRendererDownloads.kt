package mediathek.tool.cellrenderer

import com.formdev.flatlaf.extras.FlatSVGIcon
import mediathek.config.MVColor
import mediathek.controller.starter.Start
import mediathek.daten.DatenDownload
import mediathek.swing.IconUtils
import mediathek.tool.SVGIconUtilities
import mediathek.tool.table.MVTable
import org.apache.logging.log4j.LogManager
import org.kordamp.ikonli.fontawesome6.FontAwesomeRegular
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Component
import javax.swing.*
import javax.swing.border.Border

class CellRendererDownloads : CellRendererBaseWithStart() {
    private val filmStartSelected: FontIcon = FontIcon.of(FontAwesomeSolid.PLAY, IconUtils.DEFAULT_SIZE, Color.WHITE)
    private val filmStart: FontIcon = IconUtils.of(FontAwesomeSolid.PLAY)
    private val emptyBorder: Border = BorderFactory.createEmptyBorder(3, 2, 3, 2)
    private val largeBorder: Border = BorderFactory.createEmptyBorder(9, 2, 9, 2)
    private val progressBar = JProgressBar(0, 1000)
    private val panel = JPanel(BorderLayout()).apply {
        add(progressBar)
    }
    private val downloadStopSelected: FontIcon = FontIcon.of(FontAwesomeSolid.STOP, IconUtils.DEFAULT_SIZE, Color.WHITE)
    private val downloadStop: FontIcon = IconUtils.of(FontAwesomeSolid.STOP)
    private val downloadStartSelected: FlatSVGIcon = SVGIconUtilities.createSVGIcon("icons/fontawesome/caret-down.svg").apply {
        colorFilter = FlatSVGIcon.ColorFilter { Color.WHITE }
    }
    private val downloadStart: Icon = SVGIconUtilities.createSVGIcon("icons/fontawesome/caret-down.svg")
    private val downloadClearSelected: FontIcon =
        FontIcon.of(FontAwesomeSolid.ERASER, IconUtils.DEFAULT_SIZE, Color.WHITE)
    private val downloadClear: FontIcon = IconUtils.of(FontAwesomeSolid.ERASER)
    private val downloadDeleteSelected: FontIcon =
        FontIcon.of(FontAwesomeRegular.TRASH_ALT, IconUtils.DEFAULT_SIZE, Color.WHITE)
    private val downloadDelete: FontIcon = IconUtils.of(FontAwesomeRegular.TRASH_ALT)

    private fun applyHorizontalAlignment(colIndex: Int) {
        when (colIndex) {
            DatenDownload.DOWNLOAD_PROGRESS,
            DatenDownload.DOWNLOAD_FILM_NR,
            DatenDownload.DOWNLOAD_NR,
            DatenDownload.DOWNLOAD_DATUM,
            DatenDownload.DOWNLOAD_ZEIT,
            DatenDownload.DOWNLOAD_DAUER,
            DatenDownload.DOWNLOAD_BANDBREITE,
            DatenDownload.DOWNLOAD_RESTZEIT,
                -> horizontalAlignment = SwingConstants.CENTER

            DatenDownload.DOWNLOAD_GROESSE -> horizontalAlignment = SwingConstants.RIGHT
        }
    }

    private fun setBackgroundColor(c: Component, s: Start?, isSelected: Boolean) {
        if (s != null) {
            val color = when (s.status) {
                Start.STATUS_INIT -> if (isSelected) MVColor.DOWNLOAD_WAIT_SEL.color else MVColor.DOWNLOAD_WAIT.color
                Start.STATUS_RUN -> if (isSelected) MVColor.DOWNLOAD_RUN_SEL.color else MVColor.DOWNLOAD_RUN.color
                Start.STATUS_FERTIG -> if (isSelected) MVColor.DOWNLOAD_FERTIG_SEL.color else MVColor.DOWNLOAD_FERTIG.color
                Start.STATUS_ERR -> if (isSelected) MVColor.DOWNLOAD_FEHLER_SEL.color else MVColor.DOWNLOAD_FEHLER.color
                else -> null
            }
            c.background = color
        }
    }

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int,
    ): Component {
        try {
            resetComponent()
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            val rowModelIndex = table.convertRowIndexToModel(row)
            val columnModelIndex = table.convertColumnIndexToModel(column)
            val datenDownload = table.model.getValueAt(rowModelIndex, DatenDownload.DOWNLOAD_REF) as DatenDownload
            val mvTable = table as MVTable

            if (mvTable.isLineBreak()) {
                horizontalAlignment = SwingConstants.LEFT
                verticalAlignment = SwingConstants.TOP

                when (columnModelIndex) {
                    DatenDownload.DOWNLOAD_TITEL,
                    DatenDownload.DOWNLOAD_THEMA,
                    DatenDownload.DOWNLOAD_URL,
                    DatenDownload.DOWNLOAD_PROGRAMM_AUFRUF,
                    DatenDownload.DOWNLOAD_PROGRAMM_AUFRUF_ARRAY,
                    DatenDownload.DOWNLOAD_FILM_URL,
                    DatenDownload.DOWNLOAD_URL_SUBTITLE,
                    DatenDownload.DOWNLOAD_ZIEL_DATEINAME,
                    DatenDownload.DOWNLOAD_ZIEL_PFAD,
                    DatenDownload.DOWNLOAD_ZIEL_PFAD_DATEINAME,
                    DatenDownload.DOWNLOAD_ABO,
                        -> return createTextArea(valueText(value), datenDownload, columnModelIndex, isSelected)
                }
            } else {
                applyHorizontalAlignment(columnModelIndex)
            }

            when (columnModelIndex) {
                DatenDownload.DOWNLOAD_PROGRESS -> {
                    progressBar.border = if (mvTable.showSenderIcons() && !mvTable.getUseSmallSenderIcons()) {
                        largeBorder
                    } else {
                        emptyBorder
                    }
                    val start = datenDownload.start
                    if (start != null) {
                        if (1 < start.percent && start.percent < Start.PROGRESS_FERTIG) {
                            setBackgroundColor(panel, start, isSelected)
                            setBackgroundColor(progressBar, start, isSelected)

                            progressBar.value = start.percent

                            val progressValue = start.percent / 10.0
                            progressBar.string = "$progressValue%"

                            return panel
                        } else {
                            text = Start.getTextProgress(datenDownload.isDownloadManager, start)
                        }
                    } else {
                        text = ""
                    }
                }

                DatenDownload.DOWNLOAD_FILM_NR -> {
                    if (table.model.getValueAt(rowModelIndex, DatenDownload.DOWNLOAD_FILM_NR) as Int == 0) {
                        text = ""
                    }
                }

                DatenDownload.DOWNLOAD_ART -> {
                    when (datenDownload.art.toInt()) {
                        DatenDownload.ART_DOWNLOAD.toInt() -> text = DatenDownload.ART_DOWNLOAD_TXT
                        DatenDownload.ART_PROGRAMM.toInt() -> text = DatenDownload.ART_PROGRAMM_TXT
                    }
                }

                DatenDownload.DOWNLOAD_QUELLE -> {
                    when (datenDownload.quelle.toInt()) {
                        DatenDownload.QUELLE_ALLE.toInt() -> text = DatenDownload.QUELLE_ALLE_TXT
                        DatenDownload.QUELLE_ABO.toInt() -> text = DatenDownload.QUELLE_ABO_TXT
                        DatenDownload.QUELLE_BUTTON.toInt() -> text = DatenDownload.QUELLE_BUTTON_TXT
                        DatenDownload.QUELLE_DOWNLOAD.toInt() -> text = DatenDownload.QUELLE_DOWNLOAD_TXT
                    }
                }

                DatenDownload.DOWNLOAD_BUTTON_START -> handleButtonStartColumn(datenDownload, isSelected)
                DatenDownload.DOWNLOAD_BUTTON_DEL -> handleButtonDeleteColumn(datenDownload, isSelected)
                DatenDownload.DOWNLOAD_ABO -> handleAboColumn(datenDownload)
                DatenDownload.DOWNLOAD_SENDER -> {
                    if (mvTable.showSenderIcons()) {
                        val targetDim = getSenderCellDimension(table, row, column)
                        setSenderIcon(valueText(value), targetDim, isSelected)
                    }
                }

                DatenDownload.DOWNLOAD_GEO -> drawGeolocationIcons(datenDownload.film, isSelected)
            }

            if (columnModelIndex == DatenDownload.DOWNLOAD_TITEL) {
                datenDownload.film?.let { film ->
                    setIndicatorIcons(table, film, isSelected)
                }
            }

            setBackgroundColor(this, datenDownload.start, isSelected)
        } catch (ex: Exception) {
            logger.error(ex)
        }
        return this
    }

    private fun createTextArea(
        value: String,
        datenDownload: DatenDownload,
        columnModelIndex: Int,
        isSelected: Boolean,
    ): JTextArea {
        val textArea = createWrappedTextArea(value)
        if (columnModelIndex == DatenDownload.DOWNLOAD_ABO) {
            handleAboColumn(textArea, datenDownload)
        }
        setBackgroundColor(textArea, datenDownload.start, isSelected)
        return textArea
    }

    private fun setIconsAndToolTips(
        datenDownload: DatenDownload,
        filmIcon: Icon,
        downloadStartIcon: Icon,
        downloadStopIcon: Icon,
    ) {
        val start = datenDownload.start
        if (start != null && !datenDownload.isDownloadManager) {
            when (start.status) {
                Start.STATUS_FERTIG -> {
                    icon = filmIcon
                    toolTipText = PLAY_DOWNLOADED_FILM
                }

                Start.STATUS_ERR -> {
                    icon = downloadStartIcon
                    toolTipText = DOWNLOAD_STARTEN
                }

                else -> {
                    icon = downloadStopIcon
                    toolTipText = DOWNLOAD_STOPPEN
                }
            }
        } else {
            icon = downloadStartIcon
            toolTipText = DOWNLOAD_STARTEN
        }
    }

    private fun handleButtonStartColumn(datenDownload: DatenDownload, isSelected: Boolean) {
        horizontalAlignment = SwingConstants.CENTER
        if (isSelected) {
            setIconsAndToolTips(datenDownload, filmStartSelected, downloadStartSelected, downloadStopSelected)
        } else {
            setIconsAndToolTips(datenDownload, filmStart, downloadStart, downloadStop)
        }
    }

    private fun handleAboColumn(a: JTextArea, datenDownload: DatenDownload) {
        if (datenDownload.arr[DatenDownload.DOWNLOAD_ABO].isNotEmpty()) {
            a.foreground = MVColor.DOWNLOAD_IST_ABO.color
        } else {
            a.foreground = MVColor.DOWNLOAD_IST_DIREKTER_DOWNLOAD.color
            a.text = "Download"
        }
    }

    private fun handleAboColumn(datenDownload: DatenDownload) {
        horizontalAlignment = SwingConstants.CENTER
        if (datenDownload.arr[DatenDownload.DOWNLOAD_ABO].isNotEmpty()) {
            foreground = MVColor.DOWNLOAD_IST_ABO.color
        } else {
            foreground = MVColor.DOWNLOAD_IST_DIREKTER_DOWNLOAD.color
            text = "Download"
        }
    }

    private fun handleButtonDeleteColumn(datenDownload: DatenDownload, isSelected: Boolean) {
        horizontalAlignment = SwingConstants.CENTER
        val start = datenDownload.start
        if (start != null) {
            if (start.status >= Start.STATUS_FERTIG) {
                setIcons(downloadClearSelected, downloadClear, DOWNLOAD_ENTFERNEN, isSelected)
            } else {
                setupDownloadLoeschen(isSelected)
            }
        } else {
            setupDownloadLoeschen(isSelected)
        }
    }

    private fun setIcons(tab: Icon, tabSw: Icon, text: String, isSelected: Boolean) {
        icon = selectedIcon(isSelected, tabSw, tab)
        toolTipText = text
    }

    private fun setupDownloadLoeschen(isSelected: Boolean) {
        setIcons(downloadDeleteSelected, downloadDelete, DOWNLOAD_LOESCHEN, isSelected)
    }

    private companion object {
        private const val DOWNLOAD_STARTEN = "Download starten"
        private const val DOWNLOAD_LOESCHEN = "Download aus Liste entfernen"
        private const val DOWNLOAD_STOPPEN = "Download stoppen"
        private const val DOWNLOAD_ENTFERNEN = "Download entfernen"
        private const val PLAY_DOWNLOADED_FILM = "gespeicherten Film abspielen"
        private val logger = LogManager.getLogger(CellRendererDownloads::class.java)
    }
}
