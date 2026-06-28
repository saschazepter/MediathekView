package mediathek.controller

import mediathek.daten.ListeAbo
import mediathek.daten.ListeDownloads
import mediathek.daten.ListePset
import mediathek.daten.blacklist.ListeBlacklist

interface ConfigDataStore {
    val listePset: ListePset
    val listeDownloads: ListeDownloads
    val listeBlacklist: ListeBlacklist
    val listeAbo: ListeAbo
}
