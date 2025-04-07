package mediathek.javafx.filterpanel;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import mediathek.mainwindow.MediathekGui;
import mediathek.tool.FilterConfiguration;
import mediathek.tool.FilterDTO;
import org.jetbrains.annotations.NotNull;

/**
 * This class sets up the GuiFilme filter dialog.
 * property for filtering in GuiFilme.
 */
public class FilterActionPanel {
    private final FilterConfiguration filterConfig;
    private final ObservableList<FilterDTO> availableFilters;
    private final OldSwingJavaFxFilterDialog filterDialog;


    public FilterActionPanel(@NotNull FilterConfiguration filterConfig) {
        this.filterConfig = filterConfig;

        CommonViewSettingsPane viewSettingsPane = new CommonViewSettingsPane();

        //SwingUtilities.invokeLater(() -> filterDialog = new OldSwingJavaFxFilterDialog(MediathekGui.ui(), viewSettingsPane, filterToggleBtn));
        filterDialog = new OldSwingJavaFxFilterDialog(MediathekGui.ui(), viewSettingsPane);

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
        });
    }
}
