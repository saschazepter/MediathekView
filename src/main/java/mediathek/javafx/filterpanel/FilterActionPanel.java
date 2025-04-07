package mediathek.javafx.filterpanel;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.TransactionList;
import ca.odell.glazedlists.javafx.EventObservableList;
import impl.org.controlsfx.autocompletion.SuggestionProvider;
import javafx.application.Platform;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import mediathek.config.Daten;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.EventListWithEmptyFirstEntry;
import mediathek.tool.FilterConfiguration;
import mediathek.tool.FilterDTO;
import mediathek.tool.GermanStringSorter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.controlsfx.control.textfield.TextFields;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
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

    private ReadOnlyObjectProperty<String> themaProperty;


    /**
     * Stores the list of thema strings used for autocompletion.
     */
    private SuggestionProvider<String> themaSuggestionProvider;

    private CommonViewSettingsPane viewSettingsPane;

    public FilterActionPanel(@NotNull FilterConfiguration filterConfig) {
        this.filterConfig = filterConfig;

        setupViewSettingsPane();

        //SwingUtilities.invokeLater(() -> filterDialog = new OldSwingJavaFxFilterDialog(MediathekGui.ui(), viewSettingsPane, filterToggleBtn));
        filterDialog = new OldSwingJavaFxFilterDialog(MediathekGui.ui(), viewSettingsPane);

        restoreConfigSettings();

        setupConfigListeners();
        availableFilters = FXCollections.observableArrayList(filterConfig.getAvailableFilters());
        setupFilterSelection();
    }

    public OldSwingJavaFxFilterDialog getFilterDialog() {
        return filterDialog;
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

        themaProperty = viewSettingsPane.themaComboBox.valueProperty();
        setupThemaComboBox();
        /*viewSettingsPane.senderCheckList.getCheckModel().getCheckedItems().addListener((ListChangeListener<String>) c -> {
            if (!checkedChannels.isNull().get()) {
                checkedChannels.setAll(viewSettingsPane.senderCheckList.getCheckModel().getCheckedItems());
            }
            updateThemaComboBox();
        });*/
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
        viewSettingsPane.themaComboBox.setValue(filterConfig.getThema());
    }

    private void setupConfigListeners() {
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

        //var selectedSenders = viewSettingsPane.senderCheckList.getCheckModel().getCheckedItems();
        var selectedSenders = new ArrayList<String>();
        selectedSenders.add("ZDF");
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
