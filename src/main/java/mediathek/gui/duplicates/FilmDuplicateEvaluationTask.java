package mediathek.gui.duplicates;

import ca.odell.glazedlists.TransactionList;
import mediathek.config.Daten;
import mediathek.daten.DatenFilm;
import mediathek.daten.ListeFilme;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class FilmDuplicateEvaluationTask implements Runnable {
    private static final Logger logger = LogManager.getLogger();
    private final ListeFilme listeFilme;

    public FilmDuplicateEvaluationTask() {
        this.listeFilme = Daten.getInstance().getListeFilme();
    }

    private void printDuplicateStatistics() {
        var statisticsEventList = Daten.getInstance().getDuplicateStatistics();
        Map<String, Long> statisticsMap = listeFilme.parallelStream()
                .filter(DatenFilm::isDuplicate)
                .collect(Collectors.groupingBy(DatenFilm::getSender, Collectors.counting()));
        long duplicateCount = statisticsMap.values().stream().mapToLong(Long::longValue).sum();
        TransactionList<FilmStatistics> tList = new TransactionList<>(statisticsEventList);
        tList.getReadWriteLock().writeLock().lock();
        try {
            tList.beginEvent(true);
            tList.clear();
            for (var sender : statisticsMap.keySet()) {
                tList.add(new FilmStatistics(sender, statisticsMap.get(sender)));
            }
            tList.commitEvent();
        }
        finally {
            tList.getReadWriteLock().writeLock().unlock();
        }
        logger.trace("Number of duplicates: {}", duplicateCount);
    }

    private void checkDuplicates() {
        logger.trace("Start Duplicate URL search");
        final Map<String, Map<String, Set<String>>> urlCache = new HashMap<>();
        listeFilme.stream()
                .filter(f -> !f.isLivestream())
                .sorted(new BigSenderPenaltyComparator())
                .forEach(film -> {
                    final String normalUrl = film.getUrlNormalQuality();
                    final String highQualityUrl = film.isHighQuality() ? film.getHighQualityUrl() : "";
                    final String lowQualityUrl = film.hasLowQuality() ? film.getLowQualityUrl() : "";

                    Map<String, Set<String>> byHighQualityUrl = urlCache.computeIfAbsent(normalUrl, _ -> new HashMap<>());
                    Set<String> seenLowQualityUrls = byHighQualityUrl.computeIfAbsent(highQualityUrl, _ -> new HashSet<>());

                    film.setDuplicate(!seenLowQualityUrls.add(lowQualityUrl));
                });
        urlCache.clear();
    }

    @Override
    public void run() {
        checkDuplicates();
        printDuplicateStatistics();
    }
}
