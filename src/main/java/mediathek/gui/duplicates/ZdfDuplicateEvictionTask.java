package mediathek.gui.duplicates;

import com.google.common.base.Stopwatch;
import mediathek.config.Daten;
import mediathek.daten.DatenFilm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

public class ZdfDuplicateEvictionTask implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void run() {
        logger.trace("Start ZDF eviction task");
        AtomicLong evicted_films = new AtomicLong();

        Stopwatch watch = Stopwatch.createStarted();
        var listeFilme = Daten.getInstance().getListeFilme();
        var zdf_list = listeFilme.parallelStream()
                .filter(DatenFilm::isDuplicate)
                .filter(f -> f.getSender().equalsIgnoreCase("zdf"))
                .toList();
        var other_list = listeFilme
                .parallelStream()
                .toList();

        zdf_list.forEach(zdf_film -> {
            var list = other_list
                    .parallelStream()
                    .filter(other_film -> other_film.getUrlNormalQuality().equals(zdf_film.getUrlNormalQuality())
                            && other_film.getHighQualityUrl().equals(zdf_film.getHighQualityUrl())
                            && other_film.getTitle().equalsIgnoreCase(zdf_film.getTitle())
                    )
                    .sorted(new BigSenderPenaltyComparator())
                    .toList();

            if (list.size() == 2) {
                var tbd_film = list.getLast();
                if (tbd_film.getSender().equalsIgnoreCase("zdf")) {
                    listeFilme.remove(tbd_film);
                    evicted_films.getAndIncrement();
                }
                else {
                    logger.error("TBD film was NOT ZDF!");
                }
            }
        });

        watch.stop();
        logger.trace("Evicted films: {}", evicted_films);
        logger.trace("ZDF eviction took: {}", watch);
    }
}
