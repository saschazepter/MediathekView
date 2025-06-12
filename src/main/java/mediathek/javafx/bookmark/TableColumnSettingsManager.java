package mediathek.javafx.bookmark;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.swing.*;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Helper to persist and restore a JTable's column order, widths, and visibility
 * using Jackson POJOs, plus a header context menu for toggling column visibility.
 * Hidden columns remember their last position.
 * <p>
 * Usage:
 * TableColumnSettingsManager mgr = new TableColumnSettingsManager(table, settingsFile);
 * mgr.load();
 * mgr.installContextMenu();
 * dialog.addWindowListener(e -> mgr.save());
 */
public class TableColumnSettingsManager {
    private static final Logger LOG = LogManager.getLogger();
    private final JTable table;
    private final File settingsFile;
    private final List<TableColumn> allColumns = new ArrayList<>();
    private final ObjectMapper mapper = new ObjectMapper();
    private final List<ColumnSetting> lastSettings = new ArrayList<>();

    public TableColumnSettingsManager(JTable table, File settingsFile) {
        this.table = table;
        this.settingsFile = settingsFile;
        // Initialize from table's current model
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            TableColumn col = cm.getColumn(i);
            allColumns.add(col);
            ColumnSetting cs = new ColumnSetting(
                    col.getIdentifier().toString(),
                    i,
                    col.getWidth(),
                    true
            );
            lastSettings.add(cs);
        }
    }

    /**
     * Load and apply saved column settings (visibility, order, width).
     * Merges file settings into runtime defaults to preserve any in-memory changes.
     */
    public void load() {
        if (settingsFile.exists()) {
            try {
                // Read file into temporary list
                List<ColumnSetting> fileSettings = mapper.readValue(settingsFile, new TypeReference<>() {});
                // Merge fileSettings into lastSettings
                for (ColumnSetting fs : fileSettings) {
                    Optional<ColumnSetting> existing = lastSettings.stream()
                            .filter(ls -> ls.id.equals(fs.id))
                            .findFirst();
                    if (existing.isPresent()) {
                        ColumnSetting ls = existing.get();
                        ls.position = fs.position;
                        ls.width = fs.width;
                        ls.visible = fs.visible;
                    }
                    else {
                        // New column entry
                        lastSettings.add(new ColumnSetting(fs.id, fs.position, fs.width, fs.visible));
                    }
                }
                // Remove any lastSettings entries not present in allColumns
                var validIds = allColumns.stream()
                        .map(c -> c.getIdentifier().toString())
                        .toList();
                lastSettings.removeIf(ls -> !validIds.contains(ls.id));
            }
            catch (IOException ex) {
                LOG.error("Failed to load column settings.", ex);
            }
        }
        // Apply settings to table
        TableColumnModel cm = table.getColumnModel();
        while (cm.getColumnCount() > 0) {
            cm.removeColumn(cm.getColumn(0));
        }
        lastSettings.stream()
                .filter(s -> s.visible)
                .sorted(Comparator.comparingInt(a -> a.position))
                .forEach(s -> allColumns.stream()
                        .filter(col -> col.getIdentifier().toString().equals(s.id))
                        .findFirst()
                        .ifPresent(col -> {
                            cm.addColumn(col);
                            col.setPreferredWidth(s.width);
                        }));
    }

    /**
     * Save current column settings (visibility, order, width) to disk.
     * Hidden columns preserve their last known position.
     */
    public void save() {
        TableColumnModel cm = table.getColumnModel();
        // Update lastSettings with current state
        for (ColumnSetting ls : lastSettings) {
            Optional<TableColumn> colOpt = allColumns.stream()
                    .filter(c -> c.getIdentifier().toString().equals(ls.id))
                    .findFirst();
            if (colOpt.isEmpty())
                continue;
            TableColumn col = colOpt.get();
            boolean visible = isInModel(col);
            ls.visible = visible;
            if (visible) {
                ls.position = cm.getColumnIndex(ls.id);
                ls.width = col.getWidth();
            }
            // if hidden, position/width remain from last load or hide event
        }
        // Write JSON if needed
        try {
            Path parent = settingsFile.toPath().getParent();
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
            mapper.writerWithDefaultPrettyPrinter()
                    .writeValue(settingsFile, lastSettings);
        }
        catch (IOException ex) {
            LOG.error("Failed to save column settings.", ex);
        }
    }

    /**
     * Install a header context menu to toggle column visibility.
     */
    public void installContextMenu() {
        // Apply current settings
        load();
        JPopupMenu popup = new JPopupMenu();

        // Toggle visibility per column
        for (TableColumn col : allColumns) {
            String id = col.getIdentifier().toString();
            Optional<ColumnSetting> csOpt = lastSettings.stream()
                    .filter(s -> s.id.equals(id))
                    .findFirst();
            boolean visible = csOpt.map(s -> s.visible).orElse(true);
            JCheckBoxMenuItem item = new JCheckBoxMenuItem(id, visible);
            item.addActionListener(_ -> {
                TableColumnModel m = table.getColumnModel();
                csOpt.ifPresent(s -> s.visible = item.isSelected());
                if (item.isSelected()) {
                    if (!isInModel(col)) {
                        m.addColumn(col);
                        int lastIndex = m.getColumnCount() - 1;
                        int target = csOpt.map(s -> s.position).orElse(lastIndex);
                        m.moveColumn(lastIndex, Math.max(0, Math.min(target, lastIndex)));
                    }
                }
                else {
                    if (isInModel(col)) {
                        int idx = m.getColumnIndex(id);
                        csOpt.ifPresent(s -> s.position = idx);
                        m.removeColumn(col);
                    }
                }
                save();
            });
            popup.add(item);
        }
        table.getTableHeader().setComponentPopupMenu(popup);
    }

    // Helper to check if a column is currently visible
    private boolean isInModel(TableColumn col) {
        TableColumnModel cm = table.getColumnModel();
        for (int i = 0; i < cm.getColumnCount(); i++) {
            if (cm.getColumn(i) == col)
                return true;
        }
        return false;
    }

    /**
     * POJO for column settings
     */
    public static class ColumnSetting {
        public String id;
        public int position;
        public int width;
        public boolean visible;

        public ColumnSetting() {
        }

        public ColumnSetting(String id, int position, int width, boolean visible) {
            this.id = id;
            this.position = position;
            this.width = width;
            this.visible = visible;
        }
    }
}
