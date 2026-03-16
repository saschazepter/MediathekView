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

package mediathek.swingaudiothek.repository

import org.apache.logging.log4j.LogManager
import java.security.MessageDigest
import java.util.*

class PodcastIndexCredentialsProvider {
    private val logger = LogManager.getLogger(PodcastIndexCredentialsProvider::class.java)

    fun read(): PodcastIndexCredentials? {
        val key = readValue(PRIMARY_KEY_PROPERTY)
        val secret = readValue(PRIMARY_SECRET_PROPERTY)

        if (key.isNullOrBlank() || secret.isNullOrBlank()) {
            logger.debug("Podcastindex-Zugangsdaten fehlen, externe Podcast-Suche bleibt deaktiviert")
            return null
        }

        return PodcastIndexCredentials(key = key, secret = secret)
    }

    private fun readValue(name: String): String? {
        return systemProperty(name)
            ?: environment(name)
    }

    private fun systemProperty(name: String): String? {
        return System.getProperty(name)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { decodeIfObfuscated(it, name) }
    }

    private fun environment(name: String): String? {
        return System.getenv(name)
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?.let { decodeIfObfuscated(it, name) }
    }

    private fun decodeIfObfuscated(value: String, propertyName: String): String {
        if (!value.startsWith(OBFUSCATED_PREFIX)) {
            return value
        }

        val encodedPayload = value.removePrefix(OBFUSCATED_PREFIX)
        return runCatching {
            val encryptedBytes = Base64.getUrlDecoder().decode(encodedPayload)
            val keyBytes = deriveKey(propertyName)
            encryptedBytes
                .mapIndexed { index, byte -> (byte.toInt() xor keyBytes[index % keyBytes.size].toInt()).toByte() }
                .toByteArray()
                .toString(Charsets.UTF_8)
        }.getOrElse {
            logger.warn("Podcastindex-Zugangsdaten in Property {} konnten nicht dekodiert werden", propertyName, it)
            ""
        }
    }

    private fun deriveKey(propertyName: String): ByteArray {
        return MessageDigest.getInstance("SHA-256")
            .digest((OBFUSCATION_SALT + propertyName).toByteArray(Charsets.UTF_8))
    }

    companion object {
        private const val PRIMARY_KEY_PROPERTY = "PODCASTINDEX_API_KEY"
        private const val PRIMARY_SECRET_PROPERTY = "PODCASTINDEX_API_SECRET"
        private const val OBFUSCATED_PREFIX = "obf:"
        private const val OBFUSCATION_SALT = "MediathekView.PodcastIndex.v1:"
    }
}

data class PodcastIndexCredentials(
    val key: String,
    val secret: String
)
