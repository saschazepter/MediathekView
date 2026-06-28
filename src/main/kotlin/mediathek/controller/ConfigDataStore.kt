package mediathek.controller

import mediathek.daten.ListeAbo
import mediathek.daten.ListeDownloads
import mediathek.daten.ListePset
import mediathek.daten.blacklist.ListeBlacklist

interface ConfigDataStore {
    val configProgramSets: ListePset
    val configDownloads: ListeDownloads
    val configBlacklistRules: ListeBlacklist
    val configAbos: ListeAbo
}
