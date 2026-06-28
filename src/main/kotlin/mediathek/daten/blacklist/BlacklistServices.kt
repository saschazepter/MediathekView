package mediathek.daten.blacklist

import mediathek.config.Daten

class BlacklistServices(daten: Daten) {
    val rules: ListeBlacklist = ListeBlacklist(daten)

    fun applyToFilmList() {
        rules.filterListe()
    }
}
