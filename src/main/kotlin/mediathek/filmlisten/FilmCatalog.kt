package mediathek.filmlisten

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.EventList
import mediathek.config.Daten
import mediathek.daten.ListeFilme
import mediathek.gui.duplicates.FilmStatistics

class FilmCatalog(daten: Daten) {
    val loader: FilmeLaden = FilmeLaden(daten)
    val allFilms: ListeFilme = ListeFilme()
    val duplicateStatistics: EventList<FilmStatistics> = BasicEventList()
    val commonStatistics: EventList<FilmStatistics> = BasicEventList()

    /**
     * The final list of films after all filtering is done.
     * Defaults to no Lucene index unless changed at startup.
     */
    var filteredFilms: ListeFilme = ListeFilme()
}
