/*
 * Copyright (c) 2025 derreisende77.
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

package mediathek.gui.tabs.tab_livestreams.icons

import java.net.URI
import java.net.URL

object SvgIconCache {
    private val senderIconMap = mapOf(
        "arte" to SvgSenderIconLabel::class.java.getResource("/icons/sender/arte.svg")!!.toString(),
        "3sat" to SvgSenderIconLabel::class.java.getResource("/icons/sender/3sat.svg")!!.toString(),
        "ard-alpha" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ard-alpha.svg")!!.toString(),
        "br nord" to SvgSenderIconLabel::class.java.getResource("/icons/sender/br.svg")!!.toString(),
        "br süd" to SvgSenderIconLabel::class.java.getResource("/icons/sender/br.svg")!!.toString(),
        "das erste" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ard.svg")!!.toString(),
        "hr" to SvgSenderIconLabel::class.java.getResource("/icons/sender/hr.svg")!!.toString(),
        "kika" to SvgSenderIconLabel::class.java.getResource("/icons/sender/kika.svg")!!.toString(),
        "mdr sachsen" to SvgSenderIconLabel::class.java.getResource("/icons/sender/mdr.svg")!!.toString(),
        "mdr sachsen-anhalt" to SvgSenderIconLabel::class.java.getResource("/icons/sender/mdr.svg")!!.toString(),
        "mdr thüringen" to SvgSenderIconLabel::class.java.getResource("/icons/sender/mdr.svg")!!.toString(),
        "ndr hamburg" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ndr.svg")!!.toString(),
        "ndr mecklenburg-vorpommern" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ndr.svg")!!
            .toString(),
        "ndr schleswig-holstein" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ndr.svg")!!
            .toString(),
        "nrd niedersachsen" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ndr.svg")!!.toString(),
        "one" to SvgSenderIconLabel::class.java.getResource("/icons/sender/one.svg")!!.toString(),
        "parlamentsfernsehen kanal 1" to SvgSenderIconLabel::class.java.getResource("/icons/sender/Deutscher_Bundestag.svg")!!
            .toString(),
        "parlamentsfernsehen kanal 2" to SvgSenderIconLabel::class.java.getResource("/icons/sender/Deutscher_Bundestag.svg")!!
            .toString(),
        "phoenix" to SvgSenderIconLabel::class.java.getResource("/icons/sender/phoenix.svg")!!.toString(),
        "radio bremen" to SvgSenderIconLabel::class.java.getResource("/icons/sender/radio-bremen.svg")!!.toString(),
        "rbb fernsehen berlin" to SvgSenderIconLabel::class.java.getResource("/icons/sender/rbb.svg")!!.toString(),
        "rbb fernsehen brandenburg" to SvgSenderIconLabel::class.java.getResource("/icons/sender/rbb.svg")!!.toString(),
        "sr" to SvgSenderIconLabel::class.java.getResource("/icons/sender/sr.svg")!!.toString(),
        "swr baden-württemberg" to SvgSenderIconLabel::class.java.getResource("/icons/sender/swr.svg")!!.toString(),
        "swr rheinland-pfalz" to SvgSenderIconLabel::class.java.getResource("/icons/sender/swr.svg")!!.toString(),
        "tagesschau24" to SvgSenderIconLabel::class.java.getResource("/icons/sender/tagesschau24.svg")!!
            .toString(),
        "wdr" to SvgSenderIconLabel::class.java.getResource("/icons/sender/wdr.svg")!!.toString(),
        "zdf" to SvgSenderIconLabel::class.java.getResource("/icons/sender/zdf.svg")!!.toString(),
        "zdfinfo" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ZDFinfo.svg")!!.toString(),
        "zdfneo" to SvgSenderIconLabel::class.java.getResource("/icons/sender/ZDFneo.svg")!!.toString()
    )
    private val cache = mutableMapOf<String, URL>()

    private const val FALLBACK_URL = "https://upload.wikimedia.org/wikipedia/commons/3/34/IPod_placeholder.svg"

    fun getIconUrl(senderKey: String): URL {
        return cache.getOrPut(senderKey) {
            val iconUrl = senderIconMap[senderKey] ?: FALLBACK_URL
            URI(iconUrl).toURL()
        }
    }
}