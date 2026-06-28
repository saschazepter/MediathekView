package mediathek.controller

import mediathek.daten.ListeAbo
import mediathek.daten.ListeDownloads
import mediathek.daten.ListePset
import mediathek.daten.blacklist.ListeBlacklist

data class XmlConfigData(
    val programSets: ListePset,
    val downloads: ListeDownloads,
    val blacklistRules: ListeBlacklist,
    val abos: ListeAbo,
)
