package mediathek.gui.duplicates

import mediathek.config.Daten

class CommonStatsEvaluationTask(
    private val daten: Daten,
) : Runnable {
    override fun run() {
        val statisticsMap = daten.filmCatalog.allFilms.parallelStream()
            .filter { film -> !film.isLivestream }
            .countFilmsBySender()

        replaceFilmStatistics(daten.filmCatalog.commonStatistics, statisticsMap)
    }
}
