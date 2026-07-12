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

package mediathek.tool.cellrenderer

import ca.odell.glazedlists.swing.AdvancedTableModel
import mediathek.config.MVColor
import mediathek.config.application.ApplicationConfiguration
import mediathek.controller.starter.DownloadServices
import mediathek.controller.starter.StartStatus
import mediathek.daten.DatenDownload
import mediathek.daten.DatenFilm
import mediathek.gui.tabs.tab_film.table.FilmTableAppearance
import mediathek.swing.IconUtils
import mediathek.tool.models.FilmColumn
import org.apache.logging.log4j.LogManager
import org.kordamp.ikonli.fontawesome6.FontAwesomeSolid
import org.kordamp.ikonli.swing.FontIcon
import java.awt.Color
import java.awt.Component
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.swing.JTable

class CellRendererFilme(
    private val downloads: DownloadServices,
    private val appearance: FilmTableAppearance,
) : CellRendererBaseWithStart() {
    private val stopIcons = rendererIconPair(
        normal = IconUtils.of(FontAwesomeSolid.STOP),
        selected = FontIcon.of(FontAwesomeSolid.STOP, IconUtils.DEFAULT_SIZE, Color.WHITE),
    )
    private val downloadIcons = rendererIconPair(
        normal = IconUtils.of(FontAwesomeSolid.DOWNLOAD),
        selected = FontIcon.of(FontAwesomeSolid.DOWNLOAD, IconUtils.DEFAULT_SIZE, Color.WHITE),
    )
    private val playIcons = rendererIconPair(
        normal = IconUtils.of(FontAwesomeSolid.PLAY),
        selected = FontIcon.of(FontAwesomeSolid.PLAY, IconUtils.DEFAULT_SIZE, Color.WHITE),
    )
    private val bookmarkIcons = rendererIconPair(
        normal = IconUtils.of(FontAwesomeSolid.BOOKMARK),
        selected = FontIcon.of(FontAwesomeSolid.BOOKMARK, IconUtils.DEFAULT_SIZE, Color.WHITE),
    )
    private val selectedBookmarkIconHighlighted =
        FontIcon.of(FontAwesomeSolid.BOOKMARK, IconUtils.DEFAULT_SIZE, Color.ORANGE)

    override fun getTableCellRendererComponent(
        table: JTable,
        value: Any?,
        isSelected: Boolean,
        hasFocus: Boolean,
        row: Int,
        column: Int
    ): Component {
        try {
            resetComponent()
            super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column)

            val rowModelIndex = table.convertRowIndexToModel(row)
            val filmColumn = FilmColumn.fromIndex(table.convertColumnIndexToModel(column))

            @Suppress("UNCHECKED_CAST")
            val filmModel = table.model as AdvancedTableModel<DatenFilm>
            val datenFilm = filmModel.getElementAt(rowModelIndex)

            if (appearance.lineBreak) {
                horizontalAlignment = LEFT
                verticalAlignment = TOP

                when (filmColumn) {
                    FilmColumn.TOPIC,
                    FilmColumn.TITLE,
                    FilmColumn.URL,
                        -> {
                        val wrappedText = createWrappedTextArea(valueText(value), useLabelFont = true)
                        if (!isSelected) {
                            applyUnselectedRowColors(wrappedText, table, row, datenFilm)
                        }
                        if (filmColumn == FilmColumn.TITLE) {
                            wrappedText.toolTipText = datenFilm.title.takeIf {
                                wrappedText.preferredSize.width > table.getCellRect(row, column, false).width
                            }
                        }
                        return wrappedText
                    }

                    else -> Unit
                }
            } else {
                applyHorizontalAlignment(filmColumn)
            }

            when (filmColumn) {
                FilmColumn.DURATION -> text = datenFilm.filmLengthAsString
                FilmColumn.PLAY -> {
                    val datenDownload = downloads.findButtonDownloadByFilmUrl(datenFilm.urlNormalQuality)
                    handleButtonStartColumn(datenDownload, isSelected)
                }

                FilmColumn.SAVE -> handleButtonDownloadColumn(isSelected)
                FilmColumn.BOOKMARK -> handleButtonBookmarkColumn(
                    datenFilm.isBookmarked,
                    isSelected,
                    datenFilm.isLivestream
                )

                FilmColumn.SENDER -> {
                    if (appearance.showSenderIcons) {
                        val targetDim = getSenderCellDimension(table, row, column)
                        setSenderIcon(valueText(value), targetDim, isSelected)
                    }
                }

                FilmColumn.TITLE -> {
                    text = datenFilm.title
                    setIndicatorIcons(table, datenFilm, isSelected)
                }

                FilmColumn.SIZE -> text = datenFilm.fileSizeAsString
                FilmColumn.GEO -> drawGeolocationIcons(datenFilm, isSelected)
                FilmColumn.TIME -> drawTime(datenFilm)
                else -> Unit
            }

            if (!isSelected) {
                applyUnselectedRowColors(this, table, row, datenFilm)
            }
            if (filmColumn == FilmColumn.TITLE) {
                toolTipText =
                    datenFilm.title.takeIf { preferredSize.width > table.getCellRect(row, column, false).width }
            }
        } catch (ex: Exception) {
            logger.error("Fehler", ex)
        }

        return this
    }

    private fun applyUnselectedRowColors(component: Component, table: JTable, viewRow: Int, film: DatenFilm) {
        component.foreground = if (film.isNew) MVColor.NEW_COLOR.color else table.foreground
        val backgrounds = ArrayList<Color>(4)
        val alternate = if (viewRow % 2 != 0) javax.swing.UIManager.getColor("Table.alternateRowColor") else null
        backgrounds.add(alternate ?: table.background)
        if (film.isSeenInHistory) backgrounds.add(MVColor.FILM_HISTORY.color)
        if (film.isBookmarked) backgrounds.add(MVColor.FILM_BOOKMARKED.color)
        if (film.isDuplicate) backgrounds.add(MVColor.FILM_DUPLICATE.color)
        component.background = if (backgrounds.size == 1) backgrounds[0] else blend(backgrounds)
    }

    private fun blend(colors: Collection<Color>): Color = Color(
        colors.sumOf(Color::getRed) / colors.size,
        colors.sumOf(Color::getGreen) / colors.size,
        colors.sumOf(Color::getBlue) / colors.size,
        colors.sumOf(Color::getAlpha) / colors.size,
    )

    private fun drawTime(film: DatenFilm) {
        var zeit = film.sendeZeit
        if (zeit.isBlank()) {
            text = ""
            return
        }

        zeit = zeit.trim()
        try {
            val time = LocalTime.parse(zeit, PARSER)
            val longFormat = ApplicationConfiguration.getInstance().filmTimeUseLongFormat
            text = (if (longFormat) LONG else SHORT).format(time)
        } catch (_: DateTimeParseException) {
            text = zeit
        }
    }

    private fun applyHorizontalAlignment(filmColumn: FilmColumn) {
        when (filmColumn) {
            FilmColumn.NUMBER,
            FilmColumn.DATE,
            FilmColumn.TIME,
            FilmColumn.DURATION,
            FilmColumn.PLAY,
            FilmColumn.SAVE,
            FilmColumn.BOOKMARK,
                -> horizontalAlignment = CENTER

            FilmColumn.SIZE -> horizontalAlignment = RIGHT
            else -> Unit
        }
    }

    private fun handleButtonStartColumn(datenDownload: DatenDownload?, isSelected: Boolean) {
        if (datenDownload?.runtime?.runState?.status == StartStatus.RUNNING) {
            setSelectedIconAndToolTip(isSelected, stopIcons, "Film stoppen")
        }

        if (icon == null) {
            setSelectedIconAndToolTip(isSelected, playIcons, "Film abspielen")
        }
    }

    private fun handleButtonDownloadColumn(isSelected: Boolean) {
        setSelectedIconAndToolTip(isSelected, downloadIcons, "Film aufzeichnen")
    }

    private fun handleButtonBookmarkColumn(isBookMarked: Boolean, isSelected: Boolean, isLivestream: Boolean) {
        if (isLivestream) {
            icon = null
            toolTipText = ""
            return
        }

        toolTipText = if (isBookMarked) "Film aus Merkliste entfernen" else "Film merken"
        icon = when {
            isBookMarked -> selectedBookmarkIconHighlighted
            else -> bookmarkIcons.icon(isSelected)
        }
    }

    private companion object {
        private val logger = LogManager.getLogger(CellRendererFilme::class.java)
        private val PARSER: DateTimeFormatter = DateTimeFormatter.ofPattern("H:mm[:ss]")
        private val SHORT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        private val LONG: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss")
    }
}
