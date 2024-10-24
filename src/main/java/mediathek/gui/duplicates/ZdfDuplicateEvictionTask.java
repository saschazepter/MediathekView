package mediathek.gui.duplicates;

import com.google.common.base.Stopwatch;
import mediathek.config.Daten;
import mediathek.daten.DatenFilm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
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
        var dreisat_list = listeFilme
                .parallelStream()
                .filter(item -> item.getSender().equalsIgnoreCase("3sat"))
                .toList();
        ArrayList<DatenFilm> tbd_list = new ArrayList<>();

        zdf_list.forEach(zdf_film -> {
            var list = dreisat_list
                    .parallelStream()
                    .filter(dreisat_film -> dreisat_film.getUrlNormalQuality().equals(zdf_film.getUrlNormalQuality())
                            && dreisat_film.getHighQualityUrl().equals(zdf_film.getHighQualityUrl()))
                    .toList();

            if (list.size() == 1) {
                tbd_list.add(zdf_film); // remove the zdf_film
                evicted_films.getAndIncrement();
            }
        });
        listeFilme.removeAll(tbd_list);
        watch.stop();
        logger.trace("Evicted films: {}", evicted_films);
        logger.trace("ZDF eviction took: {}", watch);
        tbd_list.clear();
    }
}
