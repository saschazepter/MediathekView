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

package mediathek.audiothek.repository

import mediathek.tool.ApplicationConfiguration
import org.apache.logging.log4j.LogManager

class OnlineSearchProxyConfigProvider {
    private val logger = LogManager.getLogger(OnlineSearchProxyConfigProvider::class.java)

    fun hasBaseUrl(): Boolean = readBaseUrl() != null

    fun readBaseUrl(): String? {
        val baseUrl = systemProperty(PROXY_URL_PROPERTY)
            ?: environment(PROXY_URL_PROPERTY)
            ?: applicationSetting()

        if (baseUrl == null) {
            logger.debug("Kein Proxy für die Audiothek-Onlinesuche konfiguriert, externe Suche bleibt deaktiviert")
        }

        return baseUrl
    }

    private fun systemProperty(name: String): String? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    private fun environment(name: String): String? {
        return System.getenv(name)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    private fun applicationSetting(): String? {
        return ApplicationConfiguration.getConfiguration()
            .getString(ApplicationConfiguration.APPLICATION_AUDIOTHEK_ONLINE_SEARCH_PROXY_URL, "")
            ?.trim()
            ?.takeIf(String::isNotEmpty)
    }

    companion object {
        private const val PROXY_URL_PROPERTY = "AUDIOTHEK_ONLINE_SEARCH_PROXY_URL"
    }
}
