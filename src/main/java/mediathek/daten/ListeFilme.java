package mediathek.daten;

import mediathek.config.Konstanten;
import mediathek.tool.GermanStringSorter;
import org.jetbrains.annotations.NotNull;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class ListeFilme extends ArrayList<DatenFilm> {
    private static final String PCS_METADATA = "metaData";
    protected final PropertyChangeSupport pcs = new PropertyChangeSupport(this);
    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private volatile FilmListMetaData metaData = new FilmListMetaData();

    public FilmListMetaData getMetaData() {
        rwLock.readLock().lock();
        try {
            return metaData;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void setMetaData(FilmListMetaData meta) {
        FilmListMetaData oldValue;
        rwLock.writeLock().lock();
        try {
            oldValue = metaData;
            metaData = meta;
        } finally {
            rwLock.writeLock().unlock();
        }
        this.pcs.firePropertyChange(PCS_METADATA, oldValue, metaData);
    }

    public void addMetaDataChangeListener(PropertyChangeListener listener) {
        this.pcs.addPropertyChangeListener(PCS_METADATA, listener);
    }

    /**
     * case-insensitive .distinct() implementation.
     * @param keyExtractor the function to be applied to the key
     * @return true if it has been seen already
     * @param <T> template param
     */
    private static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Set<Object> seen = ConcurrentHashMap.newKeySet();
        return t -> seen.add(keyExtractor.apply(t));
    }

    /**
     * Search all themas within list based on sender.
     * If sender is empty, return full list of themas.
     *
     * @param sender sender name as String
     * @return IMMUTABLE List of themas as String.
     */
    public List<String> getThemen(String sender) {
        Stream<DatenFilm> mystream = snapshot().parallelStream();
        //if sender is empty return all themas...
        if (!sender.isEmpty())
            mystream = mystream.filter(f -> f.getSender().equals(sender));

        return mystream.map(DatenFilm::getThema)
                .filter(distinctByKey(String::toLowerCase))
                .sorted(GermanStringSorter.getInstance()).toList();
    }

    /**
     * Search all themas within list based on sender.
     * If sender is empty, return full list of themas.
     * <p>
     * This version does not sort nor return a unique list.
     *
     * @param sender sender name as String
     * @return IMMUTABLE List of themas as String.
     */
    public List<String> getThemenUnprocessed(String sender) {
        Stream<DatenFilm> mystream = snapshot().parallelStream();
        //if sender is empty return all themas...
        if (!sender.isEmpty())
            mystream = mystream.filter(f -> f.getSender().equalsIgnoreCase(sender));

        return mystream.map(DatenFilm::getThema).toList();
    }

    public void updateFromFilmList(@NotNull ListeFilme newFilmsList) {
        // In die vorhandene Liste soll eine andere Filmliste einsortiert werden
        // es werden nur Filme, die noch nicht vorhanden sind, einsortiert
        var incomingFilms = newFilmsList.snapshot();
        var hashNewFilms = new HashSet<String>(incomingFilms.size() + 1, 1);
        incomingFilms.forEach(newFilm -> hashNewFilms.add(newFilm.getSha256()));

        rwLock.writeLock().lock();
        try {
            this.removeIf(currentFilm -> hashNewFilms.contains(currentFilm.getSha256()));
            hashNewFilms.clear();

            incomingFilms.forEach(film -> {
                film.init();
                add(film);
            });
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    /**
     * Find movie with given url and sendername
     * @param url    String wiht URL
     * @param sender String with sender name
     * @return DatenFilm object if found or null
     */
    public DatenFilm getFilmByUrlAndSender(@NotNull String url, @NotNull String sender) {
        return snapshot().parallelStream()
                .filter(f -> f.getSender().equalsIgnoreCase(sender))
                .filter(f -> f.getUrlNormalQuality().equalsIgnoreCase(url))
                .findAny()
                .orElse(null);
    }

    /**
     * Find movie with given url
     * @param url    String wiht URL
     * @return DatenFilm object if found or null
     */
    public DatenFilm getFilmByUrl(@NotNull String url) {
        return snapshot().parallelStream()
                .filter(f -> f.getUrlNormalQuality().equalsIgnoreCase(url))
                .findAny()
                .orElse(null);
    }

    public DatenFilm getFilmByUrl_klein_hoch_hd(String url) {
        // Problem wegen gleicher URLs
        // wird versucht, einen Film mit einer kleinen/Hoher/HD-URL zu finden
        DatenFilm ret = null;
        for (DatenFilm f : snapshot()) {
            if (f.getUrlNormalQuality().equals(url)) {
                ret = f;
                break;
            } else if (f.getUrlFuerAufloesung(FilmResolution.Enum.HIGH_QUALITY).equals(url)) {
                ret = f;
                break;
            } else if (f.getUrlFuerAufloesung(FilmResolution.Enum.LOW).equals(url)) {
                ret = f;
                break;
            }
        }

        return ret;
    }

    /**
     * List needs update when it is either empty or too old.
     * @return true if we need an update.
     */
    public boolean needsUpdate() {
        return isEmptyThreadSafe() || getMetaData().isOlderThan(Konstanten.ALTER_FILMLISTE_SEKUNDEN_FUER_AUTOUPDATE);
    }

    public long countNewFilms() {
        return snapshot().stream().filter(DatenFilm::isNew).count();
    }

    public List<DatenFilm> snapshot() {
        rwLock.readLock().lock();
        try {
            return List.copyOf(this);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public void clearThreadSafe() {
        rwLock.writeLock().lock();
        try {
            super.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void addFilm(@NotNull DatenFilm film) {
        rwLock.writeLock().lock();
        try {
            super.add(film);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void sortFilms(Comparator<? super DatenFilm> comparator) {
        rwLock.writeLock().lock();
        try {
            super.sort(comparator);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public void removeFilms(@NotNull Collection<? extends DatenFilm> films) {
        rwLock.writeLock().lock();
        try {
            super.removeAll(films);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    public int sizeThreadSafe() {
        rwLock.readLock().lock();
        try {
            return super.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    public boolean isEmptyThreadSafe() {
        rwLock.readLock().lock();
        try {
            return super.isEmpty();
        } finally {
            rwLock.readLock().unlock();
        }
    }
}
