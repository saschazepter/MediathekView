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
        "zdfneo" to "https://upload.wikimedia.org/wikipedia/commons/8/8c/ZDFneo2017_Logo.svg",
        "parlamentsfernsehen kanal 1" to "https://upload.wikimedia.org/wikipedia/commons/b/b5/Deutscher_Bundestag_logo.svg",
        "parlamentsfernsehen kanal 2" to "https://upload.wikimedia.org/wikipedia/commons/b/b5/Deutscher_Bundestag_logo.svg",
        "zdfinfo" to "https://upload.wikimedia.org/wikipedia/commons/3/34/ZDFinfo_2011.svg",
        "3sat" to "https://upload.wikimedia.org/wikipedia/commons/8/81/3sat_2019.svg",
        "ard-alpha" to "https://upload.wikimedia.org/wikipedia/commons/4/4b/ARD_alpha.svg",
        "tagesschau24" to "https://upload.wikimedia.org/wikipedia/commons/2/24/Tagesschau24-2012.svg",
        "arte" to "https://upload.wikimedia.org/wikipedia/commons/4/43/Arte_Logo_2017.svg",
        "one" to "https://upload.wikimedia.org/wikipedia/commons/3/3d/One_2022.svg",
        "phoenix" to "https://upload.wikimedia.org/wikipedia/commons/4/43/Phoenix-logo-2018.svg",
        "br nord" to "https://upload.wikimedia.org/wikipedia/commons/5/51/Logo_Bayerischer_Rundfunk_2024.svg",
        "br süd" to "https://upload.wikimedia.org/wikipedia/commons/5/51/Logo_Bayerischer_Rundfunk_2024.svg",
        "ndr hamburg" to "https://upload.wikimedia.org/wikipedia/commons/3/33/NDR_Logo.svg",
        "ndr mecklenburg-vorpommern" to "https://upload.wikimedia.org/wikipedia/commons/3/33/NDR_Logo.svg",
        "ndr schleswig-holstein" to "https://upload.wikimedia.org/wikipedia/commons/3/33/NDR_Logo.svg",
        "nrd niedersachsen" to "https://upload.wikimedia.org/wikipedia/commons/3/33/NDR_Logo.svg",
        "mdr sachsen" to "https://upload.wikimedia.org/wikipedia/commons/6/61/MDR_Logo_2017.svg",
        "mdr sachsen-anhalt" to "https://upload.wikimedia.org/wikipedia/commons/6/61/MDR_Logo_2017.svg",
        "mdr thüringen" to "https://upload.wikimedia.org/wikipedia/commons/6/61/MDR_Logo_2017.svg",
        "rbb fernsehen berlin" to "https://upload.wikimedia.org/wikipedia/commons/7/79/Rbb_Logo_2017.08.svg",
        "rbb fernsehen brandenburg" to "https://upload.wikimedia.org/wikipedia/commons/7/79/Rbb_Logo_2017.08.svg",
        "swr baden-württemberg" to "https://upload.wikimedia.org/wikipedia/commons/2/26/SWR_Logo_2023.svg",
        "swr rheinland-pfalz" to "https://upload.wikimedia.org/wikipedia/commons/2/26/SWR_Logo_2023.svg",
        "zdf" to "https://upload.wikimedia.org/wikipedia/commons/c/c1/ZDF_logo.svg",
        "wdr" to "https://upload.wikimedia.org/wikipedia/commons/9/9b/WDR_Dachmarke.svg",
        "das erste" to "https://upload.wikimedia.org/wikipedia/commons/1/19/ARD_Logo_2019.svg",
        "hr" to "https://upload.wikimedia.org/wikipedia/commons/e/ea/HR-Fernsehen_Logo_2023.svg",
        "kika" to "https://upload.wikimedia.org/wikipedia/commons/f/f5/Kika_2012.svg",
        "sr" to "https://upload.wikimedia.org/wikipedia/commons/9/9c/SR_Fernsehen_Logo_2023.svg",
        "radio bremen" to "https://upload.wikimedia.org/wikipedia/commons/3/39/Logo_Radio_Bremen.svg"
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