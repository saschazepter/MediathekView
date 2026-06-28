package mediathek.gui.duplicates

import mediathek.config.Daten

class CommonStatsEvaluationTask(
    private val daten: Daten,
) : Runnable {
    override fun run() {
        val statisticsMap = daten.listeFilme.parallelStream()
            .filter { film -> !film.isLivestream }
            .countFilmsBySender()

        replaceFilmStatistics(daten.commonStatistics, statisticsMap)
    }
}
