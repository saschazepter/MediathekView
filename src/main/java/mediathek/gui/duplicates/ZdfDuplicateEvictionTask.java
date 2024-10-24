package mediathek.gui.duplicates;

import com.google.common.base.Stopwatch;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hashing;
import mediathek.config.Daten;
import mediathek.daten.DatenFilm;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class ZdfDuplicateEvictionTask implements Runnable {
    private static final Logger logger = LogManager.getLogger();

    @Override
    public void run() {
        logger.trace("Start ZDF eviction task");
        AtomicLong evicted_films = new AtomicLong();

        Stopwatch watch = Stopwatch.createStarted();
        HashFunction hf = Hashing.murmur3_128();
        final var zdf_hash = hf.newHasher()
                .putString("zdf", StandardCharsets.UTF_8)
                .hash().padToLong();
        final var dreisat_hash = hf.newHasher()
                .putString("3sat", StandardCharsets.UTF_8)
                .hash().padToLong();


        var listeFilme = Daten.getInstance().getListeFilme();
        var zdf_list = listeFilme.parallelStream()
                .filter(DatenFilm::isDuplicate)
                .filter(f -> f.getSenderHash() == zdf_hash)
                .toList();
        var dreisat_list = Daten.getInstance().getListeFilme()
                .parallelStream()
                .filter(item -> !item.isLivestream())
                .filter(item -> item.getSenderHash() == dreisat_hash)
                .toList();
        ArrayList<DatenFilm> tbd_list = new ArrayList<>();


        zdf_list.forEach(zdf_film -> {
            var list = dreisat_list
                    .parallelStream()
                    .filter(dreisat_film -> dreisat_film.getUrlNormalQuality().equals(zdf_film.getUrlNormalQuality())
                            && dreisat_film.getHighQualityUrl().equals(zdf_film.getHighQualityUrl()))
                    .toList();

            if (list.size() == 1) {
                //otherwise it will become too difficult to figure out what to remove
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
