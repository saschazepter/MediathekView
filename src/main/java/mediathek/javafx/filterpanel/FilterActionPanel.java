package mediathek.javafx.filterpanel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransactionList;
import ca.odell.glazedlists.javafx.EventObservableList;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ListProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import mediathek.config.Daten;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.EventListWithEmptyFirstEntry;
import mediathek.tool.FilterConfiguration;
import mediathek.tool.FilterDTO;
import mediathek.tool.GermanStringSorter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.RangeSlider;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * This class sets up the GuiFilme filter dialog.
 * property for filtering in GuiFilme.
 */
public class FilterActionPanel {
    private static final Logger logger = LogManager.getLogger();
    private final FilterConfiguration filterConfig;
    private final ObservableList<FilterDTO> availableFilters;
    /**
     * The "base" thema list
     */
    private final EventList<String> sourceThemaList = new BasicEventList<>();
    /**
     * The JavaFX list based on {@link #sourceThemaList}.
     */
    private final EventObservableList<String> observableThemaList = new EventObservableList<>(new EventListWithEmptyFirstEntry(sourceThemaList));
    private final OldSwingJavaFxFilterDialog filterDialog;
    private RangeSlider filmLengthSlider;
    private BooleanProperty dontShowAudioVersions;
    private BooleanProperty dontShowSignLanguage;
    private BooleanProperty dontShowTrailers;
    private BooleanProperty dontShowAbos;
    private BooleanProperty dontShowDuplicates;

    private ListProperty<String> checkedChannels = new SimpleListProperty<>(FXCollections.observableArrayList());
    private ReadOnlyObjectProperty<String> themaProperty;


    /**
     * Stores the list of thema strings used for autocompletion.
     */
    private SuggestionProvider<String> themaSuggestionProvider;

    private CommonViewSettingsPane viewSettingsPane;

    public FilterActionPanel(@NotNull JToggleButton filterToggleBtn, @NotNull FilterConfiguration filterConfig) {
        this.filterConfig = filterConfig;

        setupViewSettingsPane();

        //SwingUtilities.invokeLater(() -> filterDialog = new OldSwingJavaFxFilterDialog(MediathekGui.ui(), viewSettingsPane, filterToggleBtn));
        filterDialog = new OldSwingJavaFxFilterDialog(MediathekGui.ui(), viewSettingsPane);

        restoreConfigSettings();
        ObservableList<String> senderList = FXCollections.observableArrayList(filterConfig.getCheckedChannels());
        checkedChannels = new SimpleListProperty<>(senderList);
        setupConfigListeners();
        availableFilters = FXCollections.observableArrayList(filterConfig.getAvailableFilters());
        setupFilterSelection();
    }

    public OldSwingJavaFxFilterDialog getFilterDialog() {
        return filterDialog;
    }

    public void addFilmLengthSliderListeners(@NotNull ChangeListener<Boolean> listener) {
        filmLengthSlider.lowValueChangingProperty().addListener(listener);
        filmLengthSlider.highValueChangingProperty().addListener(listener);
    }

    public BooleanProperty dontShowAudioVersionsProperty() {
        return dontShowAudioVersions;
    }

    public BooleanProperty dontShowSignLanguageProperty() {
        return dontShowSignLanguage;
    }

    public BooleanProperty dontShowTrailersProperty() {
        return dontShowTrailers;
    }

    public BooleanProperty dontShowAbosProperty() {
        return dontShowAbos;
    }

    public BooleanProperty dontShowDuplicatesProperty() {
        return dontShowDuplicates;
    }

    private void setupFilterSelection() {
        FilterConfiguration.addAvailableFiltersObserver(() -> Platform.runLater(() -> {
            availableFilters.clear();
            availableFilters.addAll(filterConfig.getAvailableFilters());
        }));
        FilterConfiguration.addCurrentFiltersObserver(filter -> {
            //viewSettingsPane.selectFilter(filter);
            restoreConfigSettings();
        });
    }

    private void setupViewSettingsPane() {
        viewSettingsPane = new CommonViewSettingsPane();

        dontShowAbos = viewSettingsPane.cbDontShowAbos.selectedProperty();
        dontShowSignLanguage = viewSettingsPane.cbDontShowGebaerdensprache.selectedProperty();
        dontShowTrailers = viewSettingsPane.cbDontShowTrailers.selectedProperty();
        dontShowAudioVersions = viewSettingsPane.cbDontShowAudioVersions.selectedProperty();
        dontShowDuplicates = viewSettingsPane.cbDontShowDuplicates.selectedProperty();
        themaProperty = viewSettingsPane.themaComboBox.valueProperty();
        setupThemaComboBox();
        viewSettingsPane.senderCheckList.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> {
            if (!checkedChannels.isNull().get()) {
                checkedChannels.setAll(viewSettingsPane.senderCheckList.getCheckModel().getCheckedItems());
            }
            updateThemaComboBox();
        });

        filmLengthSlider = viewSettingsPane.filmLengthSliderNode._filmLengthSlider;
    }

    private void setupThemaComboBox() {
        viewSettingsPane.themaComboBox.setItems(observableThemaList);
        themaSuggestionProvider = SuggestionProvider.create(sourceThemaList);
        TextFields.bindAutoCompletion(viewSettingsPane.themaComboBox.getEditor(), themaSuggestionProvider);
    }

    public CommonViewSettingsPane getViewSettingsPane() {
        return viewSettingsPane;
    }

    private void restoreConfigSettings() {
        dontShowAbos.set(filterConfig.isDontShowAbos());
        dontShowTrailers.set(filterConfig.isDontShowTrailers());
        dontShowSignLanguage.set(filterConfig.isDontShowSignLanguage());
        dontShowAudioVersions.set(filterConfig.isDontShowAudioVersions());
        dontShowDuplicates.set(filterConfig.isDontShowDuplicates());
        viewSettingsPane.themaComboBox.setValue(filterConfig.getThema());

        restoreFilmLengthSlider();

        restoreSenderList();
    }

    private void restoreFilmLengthSlider() {
        try {
            double loadedMin = filterConfig.getFilmLengthMin();
            filmLengthSlider.setHighValueChanging(true);
            filmLengthSlider.setHighValue(filterConfig.getFilmLengthMax());
            filmLengthSlider.setHighValueChanging(false);

            filmLengthSlider.setLowValueChanging(true);
            filmLengthSlider.setLowValue(loadedMin);
            filmLengthSlider.setLowValueChanging(false);
        } catch (Exception exception) {
            logger.debug("Beim wiederherstellen der Filter Einstellungen für die Filmlänge ist ein Fehler aufgetreten!", exception);
        }
    }

    private void restoreSenderList() {
        try {
            final var checkModel = viewSettingsPane.senderCheckList.getCheckModel();
            checkModel.clearChecks();
            final var senderItems = viewSettingsPane.senderCheckList.getItems();
            for (var sender : filterConfig.getCheckedChannels()) {
                if (senderItems.contains(sender)) {
                    checkModel.check(sender);
                }
            }
        } catch (Exception exception) {
            logger.debug("Beim Wiederherstellen der Filter Einstellungen für die ausgewählten Sender ist ein Fehler aufgetreten!", exception);
        }
    }

    private void setupConfigListeners() {
        dontShowAbos.addListener(((ov, oldVal, newValue) -> filterConfig.setDontShowAbos(newValue)));
        dontShowTrailers.addListener(((ov, oldVal, newValue) -> filterConfig.setDontShowTrailers(newValue)));
        dontShowSignLanguage.addListener(((ov, oldVal, newValue) -> filterConfig.setDontShowSignLanguage(newValue)));
        dontShowAudioVersions.addListener(((ov, oldVal, newValue) -> filterConfig.setDontShowAudioVersions(newValue)));
        dontShowDuplicates.addListener(((ov, oldVal, newValue) -> filterConfig.setDontShowDuplicates(newValue)));

        filmLengthSlider.lowValueProperty().addListener(((ov, oldVal, newValue) -> filterConfig.setFilmLengthMin(newValue.doubleValue())));
        filmLengthSlider.highValueProperty().addListener(((ov, oldVal, newValue) -> filterConfig.setFilmLengthMax(newValue.doubleValue())));

        checkedChannels.addListener((obs, oldList, newList) -> filterConfig.setCheckedChannels(new HashSet<>(newList)));
        themaProperty.addListener(((ov, oldVal, newValue) -> filterConfig.setThema(newValue)));
    }

    /**
     * Retrieve the list of all thema based on sender select checkbox list.
     *
     * @param selectedSenders the list of selected senders
     * @return list of all applicable themas.
     */
    private List<String> getThemaList(@NotNull List<String> selectedSenders) {
        List<String> finalList = new ArrayList<>();

        final var blackList = Daten.getInstance().getListeFilmeNachBlackList();
        if (selectedSenders.isEmpty()) {
            finalList.addAll(blackList.getThemen(""));
        } else {
            for (String sender : selectedSenders) {
                finalList.addAll(blackList.getThemen(sender));
            }
        }

        return finalList;
    }

    /**
     * Update the Thema list and the autocompletion provider after a sender checkbox list change.
     */
    public void updateThemaComboBox() {
        //update the thema list -> updates the combobox automagically
        //use transaction list to minimize updates...
        String aktuellesThema = viewSettingsPane.themaComboBox.getValue();
        var transactionThemaList = new TransactionList<>(sourceThemaList);
        transactionThemaList.beginEvent(true);
        transactionThemaList.clear();

        var selectedSenders = viewSettingsPane.senderCheckList.getCheckModel().getCheckedItems();
        var tempThemaList = getThemaList(selectedSenders).stream().distinct().sorted(GermanStringSorter.getInstance()).toList();
        transactionThemaList.addAll(tempThemaList);
        transactionThemaList.commitEvent();

        //update autocompletion provider here only as the other listeners fire too much
        themaSuggestionProvider.clearSuggestions();
        themaSuggestionProvider.addPossibleSuggestions(sourceThemaList);

        if (!sourceThemaList.contains(aktuellesThema) && aktuellesThema != null && !aktuellesThema.isEmpty()) {
            sourceThemaList.add(aktuellesThema);
        }
        viewSettingsPane.themaComboBox.setValue(aktuellesThema);
    }
}
