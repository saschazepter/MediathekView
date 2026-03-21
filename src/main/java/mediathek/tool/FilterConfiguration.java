/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package mediathek.tool;

import mediathek.gui.tabs.tab_film.filter.FilmLengthSlider;
import mediathek.gui.tabs.tab_film.filter.ZeitraumSpinner;
import org.apache.commons.configuration2.Configuration;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FilterConfiguration {
    protected static final String FILTER_PANEL_CURRENT_FILTER = "filter.current.filter";
    protected static final String FILTER_PANEL_AVAILABLE_FILTERS = "filter.available.filters.filter_";
    private static final Pattern JSON_STRING_PATTERN = Pattern.compile("\"((?:\\\\.|[^\"])*)\"");
    private static final Logger LOG = LoggerFactory.getLogger(FilterConfiguration.class);
    private final Configuration configuration;
    private final CopyOnWriteArraySet<Runnable> availableFiltersChangedCallbacks = new CopyOnWriteArraySet<>();
    private final CopyOnWriteArraySet<Consumer<FilterDTO>> currentFilterChangedCallbacks = new CopyOnWriteArraySet<>();
    private final Map<UUID, FilterDTO> availableFiltersCache = new LinkedHashMap<>();
    private boolean availableFiltersCacheInitialized;
    private UUID currentFilterIdCache;
    private boolean currentFilterCacheInitialized;

    public FilterConfiguration() {
        this(ApplicationConfiguration.getConfiguration());
    }

    protected FilterConfiguration(Configuration configuration) {
        super();
        this.configuration = configuration;
        migrateOldFilterConfigurations();
    }

    public void addAvailableFiltersObserver(Runnable availableFiltersChangedCallback) {
        availableFiltersChangedCallbacks.add(availableFiltersChangedCallback);
    }

    public void addCurrentFiltersObserver(Consumer<FilterDTO> currentFilterChangedCallback) {
        currentFilterChangedCallbacks.add(currentFilterChangedCallback);
    }

    private void migrateOldFilterConfigurations() {
        FilterDTO newFilter = new FilterDTO(UUID.randomUUID(), "Alter Filter");
        if (migrateAll(() -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_ABOS.getOldKey(), newFilter, Boolean.class, this::setDontShowAbos),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_GEOBLOCKED.getOldKey(), newFilter, Boolean.class, this::setDontShowGeoblocked),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_AUDIO_VERSIONS.getOldKey(), newFilter, Boolean.class, this::setDontShowAudioVersions),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_SIGN_LANGUAGE.getOldKey(), newFilter, Boolean.class, this::setDontShowSignLanguage),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_TRAILERS.getOldKey(), newFilter, Boolean.class, this::setDontShowTrailers),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_FILM_LENGTH_MAX.getOldKey(), newFilter, Double.class, this::setFilmLengthMax),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_FILM_LENGTH_MIN.getOldKey(), newFilter, Double.class, this::setFilmLengthMin),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_SHOW_HD_ONLY.getOldKey(), newFilter, Boolean.class, this::setShowHighQualityOnly),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_SHOW_LIVESTREAMS_ONLY.getOldKey(), newFilter, Boolean.class, this::setShowLivestreamsOnly),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_SHOW_NEW_ONLY.getOldKey(), newFilter, Boolean.class, this::setShowNewOnly),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_SHOW_BOOK_MARKED_ONLY.getOldKey(), newFilter, Boolean.class, this::setShowBookMarkedOnly),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_SHOW_SUBTITLES_ONLY.getOldKey(), newFilter, Boolean.class, this::setShowSubtitlesOnly),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_SHOW_UNSEEN_ONLY.getOldKey(), newFilter, Boolean.class, this::setShowUnseenOnly),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_ZEITRAUM.getOldKey(), newFilter, String.class, this::setZeitraum),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_CHECKED_CHANNELS.getOldKey(), newFilter, String.class, json -> setCheckedChannels(parseJsonToSet(json))),

                () -> migrateOldFilterConfiguration(FilterConfigurationKeys.FILTER_PANEL_THEMA.getOldKey(), newFilter, String.class, this::setThema))) {
            addNewFilter(newFilter);
            LOG.info("Filter migration abgeschlossen.");
        }
    }

    @SafeVarargs
    private boolean migrateAll(Supplier<Boolean>... migrationSteps) {
        return !Arrays.stream(migrationSteps).map(Supplier::get).filter(Boolean::booleanValue).toList().isEmpty();
    }

    private <T> boolean migrateOldFilterConfiguration(String oldFilterConfigKey, FilterDTO newFilter, Class<T> classOfValueType, Consumer<T> newFilterSetter) {
        if (configuration.containsKey(oldFilterConfigKey)) {
            LOG.info("Alte Filter Konfiguration {} mit dem Wert {} gefunden. Migriere es zu einer neuen Filter Konfiguration mit der Filter ID {}.", oldFilterConfigKey, configuration.getString(oldFilterConfigKey), newFilter.id());
            setCurrentFilter(newFilter);
            T oldValue = configuration.get(classOfValueType, oldFilterConfigKey);
            if (oldValue == null) {
                LOG.info("Filter Konfiguration {} ist null, ignoriere Konfiguration für Migration.", oldFilterConfigKey);
            }
            else {
                newFilterSetter.accept(oldValue);
                configuration.clearProperty(oldFilterConfigKey);
                return true;
            }
        }
        return false;
    }

    public boolean noFiltersAreSet() {
        /*
         * If conditions are met, no filmlength filter is set.
         * return true if filtering is not needed, false if needed.
         */
        final BooleanSupplier filmLengthFilterIsNotSet = () -> {
            var filmLengthMin = (int) getFilmLengthMin();
            var filmLengthMax = (int) getFilmLengthMax();
            return filmLengthMin == 0 && filmLengthMax == FilmLengthSlider.UNLIMITED_VALUE;
        };

        return getCheckedChannels().isEmpty()
                && getThema().isEmpty()
                && filmLengthFilterIsNotSet.getAsBoolean()
                && !isDontShowAbos()
                && !isShowUnseenOnly()
                && !isShowHighQualityOnly()
                && !isShowSubtitlesOnly()
                && !isShowLivestreamsOnly()
                && !isShowNewOnly()
                && !isShowBookMarkedOnly()
                && !isDontShowTrailers()
                && !isDontShowSignLanguage()
                && !isDontShowGeoblocked()
                && !isDontShowAudioVersions()
                && !isDontShowDuplicates()
                && getZeitraum().equalsIgnoreCase(ZeitraumSpinner.INFINITE_TEXT);
    }

    public boolean isShowHighQualityOnly() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_SHOW_HD_ONLY, false);
    }

    public FilterConfiguration setShowHighQualityOnly(boolean showHdOnly) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_SHOW_HD_ONLY, showHdOnly);
        return this;
    }

    private String currentFilterConfigName(FilterConfigurationKeys filterConfigurationKey) {
        return toFilterConfigName(filterConfigurationKey, requireCurrentFilterId());
    }

    private String toFilterConfigName(FilterConfigurationKeys filterConfigurationKey, UUID filterId) {
        return String.format(filterConfigurationKey.getKey(), filterId);
    }

    private String toAvailableFilterKey(UUID filterId) {
        return FILTER_PANEL_AVAILABLE_FILTERS + filterId;
    }

    public boolean isShowSubtitlesOnly() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_SHOW_SUBTITLES_ONLY, false);
    }

    public FilterConfiguration setShowSubtitlesOnly(boolean showSubtitlesOnly) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_SHOW_SUBTITLES_ONLY, showSubtitlesOnly);
        return this;
    }

    public boolean isShowNewOnly() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_SHOW_NEW_ONLY, false);
    }

    public FilterConfiguration setShowNewOnly(boolean showNewOnly) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_SHOW_NEW_ONLY, showNewOnly);
        return this;
    }

    public boolean isShowBookMarkedOnly() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_SHOW_BOOK_MARKED_ONLY, false);
    }

    public FilterConfiguration setShowBookMarkedOnly(boolean showBookMarkedOnly) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_SHOW_BOOK_MARKED_ONLY, showBookMarkedOnly);
        return this;
    }

    public boolean isShowUnseenOnly() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_SHOW_UNSEEN_ONLY, false);
    }

    public FilterConfiguration setShowUnseenOnly(boolean showUnseenOnly) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_SHOW_UNSEEN_ONLY, showUnseenOnly);
        return this;
    }

    public boolean isDontShowDuplicates() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_DUPLICATES, false);
    }

    public FilterConfiguration setDontShowDuplicates(boolean dontShowDuplicates) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_DUPLICATES, dontShowDuplicates);
        return this;
    }

    public boolean isShowLivestreamsOnly() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_SHOW_LIVESTREAMS_ONLY, false);
    }

    public FilterConfiguration setShowLivestreamsOnly(boolean showLivestreamsOnly) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_SHOW_LIVESTREAMS_ONLY, showLivestreamsOnly);
        return this;
    }

    public boolean isDontShowAbos() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_ABOS, false);
    }

    public FilterConfiguration setDontShowAbos(boolean dontShowAbos) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_ABOS, dontShowAbos);
        return this;
    }

    public boolean isDontShowTrailers() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_TRAILERS, false);
    }

    public FilterConfiguration setDontShowTrailers(boolean dontShowTrailers) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_TRAILERS, dontShowTrailers);
        return this;
    }

    public boolean isDontShowSignLanguage() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_SIGN_LANGUAGE, false);
    }

    public FilterConfiguration setDontShowSignLanguage(boolean dontShowSignLanguage) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_SIGN_LANGUAGE, dontShowSignLanguage);
        return this;
    }

    public boolean isDontShowGeoblocked() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_GEOBLOCKED, false);
    }

    public FilterConfiguration setDontShowGeoblocked(boolean dontShowGeoblocked) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_GEOBLOCKED, dontShowGeoblocked);
        return this;
    }

    public boolean isDontShowAudioVersions() {
        return getCurrentFilterBoolean(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_AUDIO_VERSIONS, false);
    }

    public FilterConfiguration setDontShowAudioVersions(boolean dontShowAudioVersions) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_DONT_SHOW_AUDIO_VERSIONS, dontShowAudioVersions);
        return this;
    }


    public double getFilmLengthMin() {
        return getCurrentFilterDouble(FilterConfigurationKeys.FILTER_PANEL_FILM_LENGTH_MIN, 0.0d);
    }

    public FilterConfiguration setFilmLengthMin(double filmLengthMin) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_FILM_LENGTH_MIN, filmLengthMin);
        return this;
    }

    public double getFilmLengthMax() {
        return getCurrentFilterDouble(FilterConfigurationKeys.FILTER_PANEL_FILM_LENGTH_MAX, FilmLengthSlider.UNLIMITED_VALUE);
    }

    public FilterConfiguration setFilmLengthMax(double filmLengthMax) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_FILM_LENGTH_MAX, filmLengthMax);
        return this;
    }

    public String getZeitraum() {
        return getCurrentFilterString(FilterConfigurationKeys.FILTER_PANEL_ZEITRAUM, ZeitraumSpinner.INFINITE_TEXT);
    }

    public FilterConfiguration setZeitraum(@NotNull String zeitraum) {
        setCurrentFilterProperty(FilterConfigurationKeys.FILTER_PANEL_ZEITRAUM, zeitraum);
        return this;
    }

    public Set<String> getCheckedChannels() {
        String key = currentFilterConfigName(FilterConfigurationKeys.FILTER_PANEL_CHECKED_CHANNELS);
        Object value = configuration.getProperty(key);

        switch (value) {
            case Collection<?> collection -> {
                Set<String> result = new LinkedHashSet<>();
                collection.forEach(item -> {
                    if (item != null) {
                        result.add(item.toString());
                    }
                });
                return result;
            }
            case null, default -> {
            }
        }

        String raw = configuration.getString(key, "");
        if (raw == null || raw.isBlank()) {
            return new HashSet<>();
        }

        Set<String> legacyParsed = parseLegacyJsonArray(raw);
        if (!legacyParsed.isEmpty() || "[]".equals(raw.trim())) {
            return legacyParsed;
        }

        return new HashSet<>(Collections.singletonList(raw));
    }

    public FilterConfiguration setCheckedChannels(@NotNull Collection<String> newList) {
        var distinctValues = new LinkedHashSet<>(newList);
        String json = JsonStringUtils.toJsonStringArray(distinctValues);
        configuration.setProperty(currentFilterConfigName(FilterConfigurationKeys.FILTER_PANEL_CHECKED_CHANNELS), json);
        return this;
    }


    public String getThema() {
        return getCurrentFilterString(FilterConfigurationKeys.FILTER_PANEL_THEMA, "");
    }

    public FilterConfiguration setThema(String thema) {
        String key = currentFilterConfigName(FilterConfigurationKeys.FILTER_PANEL_THEMA);

        if (thema == null || thema.trim().isEmpty()) {
            configuration.clearProperty(key);
        }
        else {
            configuration.setProperty(key, thema);
        }
        return this;
    }


    private Set<String> parseJsonToSet(String json) {
        return parseLegacyJsonArray(json);
    }

    private Set<String> parseLegacyJsonArray(String json) {
        try {
            String trimmed = json == null ? "" : json.trim();
            if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
                return new HashSet<>();
            }

            Set<String> result = new HashSet<>();
            Matcher matcher = JSON_STRING_PATTERN.matcher(trimmed);
            while (matcher.find()) {
                result.add(JsonStringUtils.unescapeJsonString(matcher.group(1)));
            }
            return result;
        }
        catch (Exception e) {
            LOG.error("Fehler beim Konvertieren der alten Senderliste aus JSON", e);
            return new HashSet<>();
        }
    }

    public FilterConfiguration clearCurrentFilter() {
        UUID currentFilterId = requireCurrentFilterId();
        Arrays.stream(FilterConfigurationKeys.values())
                .map(key -> toFilterConfigName(key, currentFilterId))
                .forEach(configuration::clearProperty);
        return this;
    }

    public UUID getCurrentFilterID() {
        return getCurrentFilter().id();
    }

    public FilterDTO getCurrentFilter() {
        UUID currentFilterId = requireCurrentFilterId();

        if (availableFiltersCache.isEmpty()) {
            return new FilterDTO(currentFilterId, configuration.getString(toAvailableFilterKey(currentFilterId), ""));
        }

        FilterDTO currentFilter = availableFiltersCache.get(currentFilterId);
        if (currentFilter == null) {
            FilterDTO filter = getFirstAvailableFilter().orElseGet(() -> {
                FilterDTO newFilter = new FilterDTO(UUID.randomUUID(), "Filter 1");
                addNewFilter(newFilter);
                return newFilter;
            });
            setCurrentFilter(filter);
            return filter;
        }
        return currentFilter;
    }

    public FilterConfiguration setCurrentFilter(FilterDTO currentFilter) {
        return setCurrentFilter(currentFilter.id());
    }

    public FilterConfiguration setCurrentFilter(UUID currentFilterID) {
        ensureAvailableFiltersCacheInitialized();
        configuration.setProperty(FILTER_PANEL_CURRENT_FILTER, currentFilterID);
        currentFilterIdCache = currentFilterID;
        currentFilterCacheInitialized = true;
        notifyCurrentFilterChanged(resolveCurrentFilterForNotification(currentFilterID));
        return this;
    }

    public List<UUID> getAvailableFilterIds() {
        return getAvailableFilters().stream().map(FilterDTO::id).toList();
    }

    public List<String> getAvailableFilterNames() {
        return getAvailableFilters().stream().map(FilterDTO::name).toList();
    }

    public int getAvailableFilterCount() {
        ensureAvailableFiltersCacheInitialized();
        return availableFiltersCache.size();
    }

    public List<FilterDTO> getAvailableFilters() {
        ensureAvailableFiltersCacheInitialized();
        return List.copyOf(availableFiltersCache.values());
    }

    public String getFilterName(UUID id) {
        ensureAvailableFiltersCacheInitialized();
        FilterDTO filter = availableFiltersCache.get(id);
        return filter != null ? filter.name() : "";
    }

    public FilterConfiguration addNewFilter(FilterDTO filterDTO) {
        ensureAvailableFiltersCacheInitialized();
        configuration.addProperty(toAvailableFilterKey(filterDTO.id()), filterDTO.name());
        availableFiltersCache.put(filterDTO.id(), filterDTO);
        notifyAvailableFiltersChanged();
        return this;
    }

    public FilterConfiguration addNewFilter(UUID filterId, String filterName) {
        return addNewFilter(new FilterDTO(filterId, filterName));
    }

    public FilterConfiguration deleteFilter(FilterDTO filterToDelete) {
        return deleteFilter(filterToDelete.id());
    }

    public FilterConfiguration deleteFilter(UUID idToDelete) {
        ensureAvailableFiltersCacheInitialized();
        ensureCurrentFilterCacheInitialized();

        boolean filterToDeleteIsCurrentFilter = idToDelete.equals(currentFilterIdCache);
        if (filterToDeleteIsCurrentFilter) {
            configuration.clearProperty(FILTER_PANEL_CURRENT_FILTER);
            currentFilterIdCache = null;
            currentFilterCacheInitialized = true;
        }
        clearFilterProperties(idToDelete);
        availableFiltersCache.remove(idToDelete);
        notifyAvailableFiltersChanged();
        if (filterToDeleteIsCurrentFilter) {
            notifyCurrentFilterChanged(getCurrentFilter());
        }
        return this;
    }

    private void clearFilterProperties(UUID filterId) {
        configuration.clearProperty(toAvailableFilterKey(filterId));
        Arrays.stream(FilterConfigurationKeys.values())
                .map(key -> toFilterConfigName(key, filterId))
                .forEach(configuration::clearProperty);
    }

    public FilterConfiguration renameCurrentFilter(String newName) {
        UUID currentFilterId = getCurrentFilterID();
        configuration.setProperty(toAvailableFilterKey(currentFilterId), newName);
        ensureAvailableFiltersCacheInitialized();
        availableFiltersCache.put(currentFilterId, new FilterDTO(currentFilterId, newName));
        notifyAvailableFiltersChanged();
        notifyCurrentFilterChanged(getCurrentFilter());
        return this;
    }

    public Optional<FilterDTO> findFilterForName(String name) {
        ensureAvailableFiltersCacheInitialized();
        return availableFiltersCache.values().stream().filter(filter -> filter.name().equals(name)).findFirst();
    }

    private void ensureAvailableFiltersCacheInitialized() {
        if (availableFiltersCacheInitialized) {
            return;
        }

        availableFiltersCache.clear();
        configuration.getKeys().forEachRemaining(key -> {
            if (key.startsWith(FILTER_PANEL_AVAILABLE_FILTERS)) {
                UUID filterId = UUID.fromString(key.substring(FILTER_PANEL_AVAILABLE_FILTERS.length()));
                availableFiltersCache.put(filterId, new FilterDTO(filterId, String.valueOf(configuration.getProperty(key))));
            }
        });
        availableFiltersCacheInitialized = true;
    }

    private void ensureCurrentFilterCacheInitialized() {
        if (currentFilterCacheInitialized) {
            return;
        }

        currentFilterIdCache = configuration.get(UUID.class, FILTER_PANEL_CURRENT_FILTER, null);
        currentFilterCacheInitialized = true;
    }

    private Optional<FilterDTO> getFirstAvailableFilter() {
        ensureAvailableFiltersCacheInitialized();
        return availableFiltersCache.values().stream().findFirst();
    }

    private void notifyAvailableFiltersChanged() {
        availableFiltersChangedCallbacks.forEach(Runnable::run);
    }

    private void notifyCurrentFilterChanged(FilterDTO filter) {
        currentFilterChangedCallbacks.forEach(consumer -> consumer.accept(filter));
    }

    private FilterDTO resolveCurrentFilterForNotification(UUID currentFilterID) {
        FilterDTO currentFilter = availableFiltersCache.get(currentFilterID);
        if (currentFilter != null) {
            return currentFilter;
        }
        return new FilterDTO(currentFilterID, configuration.getString(toAvailableFilterKey(currentFilterID), ""));
    }

    private boolean getCurrentFilterBoolean(FilterConfigurationKeys key, boolean defaultValue) {
        return configuration.getBoolean(currentFilterConfigName(key), defaultValue);
    }

    private double getCurrentFilterDouble(FilterConfigurationKeys key, double defaultValue) {
        return configuration.getDouble(currentFilterConfigName(key), defaultValue);
    }

    private String getCurrentFilterString(FilterConfigurationKeys key, String defaultValue) {
        return configuration.getString(currentFilterConfigName(key), defaultValue);
    }

    private void setCurrentFilterProperty(FilterConfigurationKeys key, Object value) {
        configuration.setProperty(currentFilterConfigName(key), value);
    }

    private UUID requireCurrentFilterId() {
        ensureAvailableFiltersCacheInitialized();
        ensureCurrentFilterCacheInitialized();

        if (currentFilterIdCache == null) {
            FilterDTO filter = getFirstAvailableFilter().orElseGet(() -> {
                FilterDTO newFilter = new FilterDTO(UUID.randomUUID(), "Filter 1");
                addNewFilter(newFilter);
                return newFilter;
            });
            setCurrentFilter(filter);
        }

        return currentFilterIdCache;
    }

    protected enum FilterConfigurationKeys {
        FILTER_PANEL_SHOW_HD_ONLY("filter.filter_%s.show.hd_only"),
        FILTER_PANEL_SHOW_SUBTITLES_ONLY("filter.filter_%s.show.subtitles_only"),
        FILTER_PANEL_SHOW_BOOK_MARKED_ONLY("filter.filter_%s.show.book_marked_only"),
        FILTER_PANEL_SHOW_NEW_ONLY("filter.filter_%s.show.new_only"),
        FILTER_PANEL_SHOW_UNSEEN_ONLY("filter.filter_%s.show.unseen_only"),
        FILTER_PANEL_SHOW_LIVESTREAMS_ONLY("filter.filter_%s.show.livestreams_only"),
        FILTER_PANEL_DONT_SHOW_ABOS("filter.filter_%s.dont_show.abos"),
        FILTER_PANEL_DONT_SHOW_GEOBLOCKED("filter.filter_%s.dont_show.geoblocked"),
        FILTER_PANEL_DONT_SHOW_TRAILERS("filter.filter_%s.dont_show.trailers"),
        FILTER_PANEL_DONT_SHOW_SIGN_LANGUAGE("filter.filter_%s.dont_show.sign_language"),
        FILTER_PANEL_DONT_SHOW_AUDIO_VERSIONS("filter.filter_%s.dont_show.audio_versions"),
        FILTER_PANEL_FILM_LENGTH_MIN("filter.filter_%s.film_length.min"),
        FILTER_PANEL_FILM_LENGTH_MAX("filter.filter_%s.film_length.max"),
        FILTER_PANEL_ZEITRAUM("filter.filter_%s.zeitraum"),
        FILTER_PANEL_DONT_SHOW_DUPLICATES("filter.filter_%s.dont_show_duplicates"),
        FILTER_PANEL_CHECKED_CHANNELS("filter.filter_%s.checked_channels"),
        FILTER_PANEL_THEMA("filter.filter_%s.thema");
        private final String key;

        FilterConfigurationKeys(final String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }

        public String getOldKey() {
            return key.replace(".filter_%s", "");
        }
    }
}
