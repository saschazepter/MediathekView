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

package mediathek.swing

import kotlinx.coroutines.*
import kotlinx.coroutines.swing.Swing
import javax.swing.JLabel
import kotlin.time.Duration

class FilmAgeLabel(
    private val ageProvider: () -> Duration?,
    tooltip: String
) : JLabel() {
    private var uiScope: CoroutineScope? = null
    private var updateJob: Job? = null
    private var lastRenderedText: String? = null

    init {
        toolTipText = tooltip
        updateLabel()
    }

    override fun addNotify() {
        super.addNotify()
        startUpdating()
    }

    override fun removeNotify() {
        stopUpdating()
        super.removeNotify()
    }

    private fun startUpdating() {
        if (updateJob?.isActive == true) {
            return
        }

        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
        uiScope = scope
        updateJob = scope.launch {
            while (isActive) {
                updateLabel()
                delay(1_000)
            }
        }
    }

    private fun stopUpdating() {
        updateJob?.cancel()
        updateJob = null
        uiScope?.cancel()
        uiScope = null
    }

    private fun updateLabel() {
        val renderedText = ageProvider()
            ?.let(FilmListAge::from)
            ?.toDisplayText()
            .orEmpty()
        if (renderedText == lastRenderedText) {
            return
        }

        text = renderedText
        lastRenderedText = renderedText
    }

    private data class FilmListAge(val totalMinutes: Long) {
        val days: Long = totalMinutes / MINUTES_PER_DAY
        val hours: Long = (totalMinutes % MINUTES_PER_DAY) / MINUTES_PER_HOUR
        val minutes: Long = totalMinutes % MINUTES_PER_HOUR

        fun toDisplayText(): String {
            return when {
                days > 0 -> "Alter: ${days}d ${hours}h ${minutes}m"
                hours > 0 -> "Alter: ${hours}h ${minutes}m"
                else -> "Alter: ${minutes}m"
            }
        }

        companion object {
            private const val MINUTES_PER_HOUR = 60L
            private const val MINUTES_PER_DAY = 24L * MINUTES_PER_HOUR

            fun from(duration: Duration): FilmListAge {
                return FilmListAge(duration.inWholeMinutes)
            }
        }
    }
}
