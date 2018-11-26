package mediathek.gui;

import javafx.collections.ObservableList;
import mSearch.daten.DatenFilm;
import mSearch.daten.ListeFilme;
import mediathek.config.Daten;
import mediathek.javafx.filterpanel.FilmActionPanel;
import mediathek.javafx.filterpanel.FilmLengthSlider;
import mediathek.tool.Filter;
import mediathek.tool.TModel;
import mediathek.tool.TModelFilm;
import mediathek.tool.table.MVTable;

import java.util.concurrent.TimeUnit;

public class GuiFilmeModelHelper {
    private final FilmActionPanel fap;
    private final Daten daten;
    private final MVTable tabelle;
    private final TModel tModel;
    private final ListeFilme listeFilme;
    private boolean searchThroughDescriptions;
    private boolean nurNeue;
    private boolean nurUt;
    private boolean showOnlyHd;
    private boolean kGesehen;
    private boolean keineAbos;
    private boolean showOnlyLivestreams;
    private boolean dontShowTrailers;
    private boolean dontShowGebaerdensprache;
    private boolean dontShowAudioVersions;
    private long maxLength;
    private String[] arrIrgendwo;
    private long minLengthInSeconds;
    private long maxLengthInSeconds;

    public GuiFilmeModelHelper(FilmActionPanel fap, Daten daten, MVTable tabelle) {
        this.fap = fap;
        this.daten = daten;
        this.tabelle = tabelle;

        tModel = new TModelFilm(new Object[][]{}, DatenFilm.COLUMN_NAMES);
        listeFilme = daten.getListeFilmeNachBlackList();

    }

    private String getFilterThema() {
        String filterThema = fap.themaBox.getSelectionModel().getSelectedItem();
        if (filterThema == null) {
            filterThema = "";
        }

        return filterThema;
    }

    private String[] evaluateThemaTitel() {
        String[] arrThemaTitel;

        final String filterThemaTitel = fap.roSearchStringProperty.getValueSafe();
        if (Filter.isPattern(filterThemaTitel)) {
            arrThemaTitel = new String[]{filterThemaTitel};
        } else {
            arrThemaTitel = filterThemaTitel.split(",");
            for (int i = 0; i < arrThemaTitel.length; ++i) {
                arrThemaTitel[i] = arrThemaTitel[i].trim().toLowerCase();
            }
        }

        return arrThemaTitel;
    }

    private boolean noFiltersAreSet() {
        boolean ret = false;

        if (fap.senderList.getCheckModel().isEmpty()
                && getFilterThema().isEmpty()
                && fap.roSearchStringProperty.getValueSafe().isEmpty()
                && ((int) fap.filmLengthSlider.getLowValue() == 0)
                && ((int) fap.filmLengthSlider.getHighValue() == FilmLengthSlider.UNLIMITED_VALUE)
                && !fap.dontShowAbos.getValue()
                && !fap.showUnseenOnly.getValue()
                && !fap.showOnlyHd.getValue()
                && !fap.showSubtitlesOnly.getValue()
                && !fap.showLivestreamsOnly.getValue()
                && !fap.showNewOnly.getValue()
                && !fap.dontShowTrailers.getValue()
                && !fap.dontShowSignLanguage.getValue()
                && !fap.dontShowAudioVersions.getValue())
            ret = true;

        return ret;
    }

    private void updateFilterVars() {
        nurNeue = fap.showNewOnly.getValue();
        nurUt = fap.showSubtitlesOnly.getValue();
        showOnlyHd = fap.showOnlyHd.getValue();
        kGesehen = fap.showUnseenOnly.getValue();
        keineAbos = fap.dontShowAbos.getValue();
        showOnlyLivestreams = fap.showLivestreamsOnly.getValue();
        dontShowTrailers = fap.dontShowTrailers.getValue();
        dontShowGebaerdensprache = fap.dontShowSignLanguage.getValue();
        dontShowAudioVersions = fap.dontShowAudioVersions.getValue();
        searchThroughDescriptions = fap.searchThroughDescription.getValue();

        arrIrgendwo = evaluateThemaTitel();
    }

    private void calculateFilmLengthSliderValues() {
        final long minLength = (long) fap.filmLengthSlider.getLowValue();
        maxLength = (long) fap.filmLengthSlider.getHighValue();
        minLengthInSeconds = TimeUnit.SECONDS.convert(minLength, TimeUnit.MINUTES);
        maxLengthInSeconds = TimeUnit.SECONDS.convert(maxLength, TimeUnit.MINUTES);
    }

    private void performTableFiltering() {
        updateFilterVars();
        calculateFilmLengthSliderValues();

        final String filterThema = getFilterThema();
        final boolean searchFieldEmpty = arrIrgendwo.length == 0;
        final ObservableList<String> selectedSenders = fap.senderList.getCheckModel().getCheckedItems();

        for (DatenFilm film : listeFilme) {
            if (!selectedSenders.isEmpty()) {
                if (!selectedSenders.contains(film.getSender()))
                    continue;
            }

            final long filmLength = film.getFilmLength();
            if (filmLength < minLengthInSeconds)
                continue;

            if (maxLength < FilmLengthSlider.UNLIMITED_VALUE) {
                if (filmLength > maxLengthInSeconds)
                    continue;

            }
            if (nurNeue) {
                if (!film.isNew()) {
                    continue;
                }
            }
            if (showOnlyLivestreams) {
                if (!film.getThema().equals(ListeFilme.THEMA_LIVE)) {
                    continue;
                }
            }
            if (showOnlyHd) {
                if (!film.isHD()) {
                    continue;
                }
            }
            if (nurUt) {
                if (!film.hasSubtitle()) {
                    continue;
                }
            }
            if (keineAbos) {
                if (!film.arr[DatenFilm.FILM_ABO_NAME].isEmpty()) {
                    continue;
                }
            }
            if (kGesehen) {
                if (daten.history.urlPruefen(film.getUrlHistory())) {
                    continue;
                }
            }

            if (dontShowTrailers) {
                if (film.isTrailerTeaser())
                    continue;
            }

            if (dontShowGebaerdensprache) {
                if (film.isSignLanguage())
                    continue;
            }

            if (dontShowAudioVersions) {
                if (film.isAudioVersion())
                    continue;
            }

            if (!filterThema.isEmpty()) {
                if (!film.getThema().equalsIgnoreCase(filterThema))
                    continue;
            }

            //minor speedup in case we don´t have search field entries...
            if (searchFieldEmpty)
                addFilmToTableModel(film);
            else {
                if (finalStageFiltering(film)) {
                    addFilmToTableModel(film);
                }
            }
        }
    }

    /**
     * Perform the last stage of filtering.
     * Rework!!!
     */
    private boolean finalStageFiltering(final DatenFilm film) {
        boolean result;

        if (searchThroughDescriptions && !film.getDescription().isEmpty())
            result = searchEntriesWithDescription(film);
        else
            result = searchEntries(film);

        return result;
    }

    private boolean searchEntries(DatenFilm film) {
        boolean result = false;
        if (Filter.pruefen(arrIrgendwo, film.getThema())
                || Filter.pruefen(arrIrgendwo, film.getTitle())) {
            result = true;
        }
        return result;
    }

    private boolean searchEntriesWithDescription(DatenFilm film) {
        boolean result = false;

        if (Filter.pruefen(arrIrgendwo, film.getDescription())
                || searchEntries(film)) {
            result = true;
        }

        return result;
    }

    private void fillTableModel() {
        // dann ein neues Model anlegen
        if (noFiltersAreSet()) {
            // dann ganze Liste laden
            addAllFilmsToTableModel();
        } else {
            performTableFiltering();
        }
        tabelle.setModel(tModel);
    }

    public void prepareTableModel() {
        if (!listeFilme.isEmpty())
            fillTableModel();

        //use empty model
        tabelle.setModel(tModel);
    }

    private void addAllFilmsToTableModel() {
        if (!listeFilme.isEmpty()) {
            for (DatenFilm film : listeFilme) {
                addFilmToTableModel(film);
            }
        }
    }

    private void addFilmToTableModel(DatenFilm film) {
        Object[] object = new Object[DatenFilm.MAX_ELEM];
        for (int m = 0; m < DatenFilm.MAX_ELEM; ++m) {
            switch (m) {
                case DatenFilm.FILM_NR:
                    object[m] = film.getFilmNr();
                    break;
                case DatenFilm.FILM_DATUM:
                    object[m] = film.datumFilm;
                    break;
                case DatenFilm.FILM_GROESSE:
                    object[m] = film.getFilmSize();
                    break;
                case DatenFilm.FILM_REF:
                    object[m] = film;
                    break;
                case DatenFilm.FILM_HD:
                    object[m] = film.isHD() ? "1" : "0";
                    break;
                case DatenFilm.FILM_UT:
                    object[m] = film.hasSubtitle() ? "1" : "0";
                    break;
                case DatenFilm.FILM_URL:
                    object[m] = film.getUrl();
                    break;
                default:
                    object[m] = film.arr[m];
                    break;
            }
        }

        tModel.addRow(object);
    }
}
