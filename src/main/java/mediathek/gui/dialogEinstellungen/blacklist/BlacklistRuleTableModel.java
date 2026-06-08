package mediathek.gui.dialogEinstellungen.blacklist;

import mediathek.daten.blacklist.BlacklistRule;
import mediathek.daten.blacklist.CompiledBlacklistMatcher;
import mediathek.daten.blacklist.ListeBlacklist;
import mediathek.daten.DatenFilm;
import org.jspecify.annotations.NonNull;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class BlacklistRuleTableModel extends AbstractTableModel {
    private static final int BLACKLIST_SENDER = 0;
    private static final int BLACKLIST_THEMA = 1;
    private static final int BLACKLIST_TITEL = 2;
    private static final int BLACKLIST_THEMA_TITEL = 3;
    private static final int BLACKLIST_FILTERED = 4;
    private final ListeBlacklist blacklist;
    private final Supplier<List<DatenFilm>> filmsSupplier;
    private int[] filteredCounts = new int[0];

    public BlacklistRuleTableModel(
            @NonNull ListeBlacklist blacklist,
            @NonNull Supplier<List<DatenFilm>> filmsSupplier
    ) {
        this.blacklist = blacklist;
        this.filmsSupplier = filmsSupplier;
        updateFilteredCounts();
    }

    @Override
    public int getRowCount() {
        return blacklist.size();
    }

    @Override
    public int getColumnCount() {
        return 5;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var rule = blacklist.get(rowIndex);
        return switch (columnIndex) {
            case BLACKLIST_SENDER -> rule.getSender();
            case BLACKLIST_THEMA -> rule.getThema();
            case BLACKLIST_TITEL -> rule.getTitel();
            case BLACKLIST_THEMA_TITEL -> rule.getThema_titel();
            case BLACKLIST_FILTERED -> getFilteredCount(rowIndex);
            default -> throw new IllegalStateException("Unexpected value: " + columnIndex);
        };
    }

    @Override
    public String getColumnName(int column) {
        return switch (column) {
            case BLACKLIST_SENDER -> "Sender";
            case BLACKLIST_THEMA -> "Thema";
            case BLACKLIST_TITEL -> "Titel";
            case BLACKLIST_THEMA_TITEL -> "Thema-Titel";
            case BLACKLIST_FILTERED -> "gefiltert";
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        return switch (columnIndex) {
            case BLACKLIST_FILTERED -> Integer.class;
            default -> String.class;
        };
    }

    /**
     * Remove a BlacklistRule from model
     *
     * @param modelIndex index from blacklist to delete
     */
    public void removeRow(int modelIndex) {
        blacklist.remove(modelIndex);
        updateFilteredCounts();
        fireTableRowsDeleted(modelIndex, modelIndex);
    }

    /**
     * Remove a collection of rules.
     * Fire update after all rules have been removed.
     *
     * @param list of objects to be deleted
     */
    public void removeRules(@NonNull List<BlacklistRule> list) {
        blacklist.remove(list);
        updateFilteredCounts();
        fireTableDataChanged();
    }

    /**
     * Remove all blacklist rules.
     */
    public void removeAll() {
        blacklist.clear();
        updateFilteredCounts();
        fireTableDataChanged();
    }

    /**
     * Add a rule to the blacklist store.
     *
     * @param rule to be added.
     */
    public void addRule(@NonNull BlacklistRule rule) {
        int rowIndex = blacklist.size();
        blacklist.add(rule);
        updateFilteredCounts();
        fireTableRowsInserted(rowIndex, rowIndex);
    }

    public boolean contains(@NonNull BlacklistRule rule) {
        return blacklist.contains(rule);
    }

    public void updateRule(int modelIndex, @NonNull BlacklistRule updatedRule) {
        var rule = blacklist.get(modelIndex);
        rule.setSender(updatedRule.getSender());
        rule.setThema(updatedRule.getThema());
        rule.setTitel(updatedRule.getTitel());
        rule.setThema_titel(updatedRule.getThema_titel());

        blacklist.filterListAndNotifyListeners();
        updateFilteredCounts();
        fireTableRowsUpdated(modelIndex, modelIndex);
    }

    public void refreshFilteredCounts() {
        updateFilteredCounts();
        if (getRowCount() > 0) {
            fireTableRowsUpdated(0, getRowCount() - 1);
        }
    }

    /**
     * Get a blacklist rule based on model index.
     *
     * @param fromModelIndex the index.
     * @return the rule.
     */
    public BlacklistRule getRule(int fromModelIndex) {
        var rule = blacklist.get(fromModelIndex);
        return new BlacklistRule(
                rule.getSender(),
                rule.getThema(),
                rule.getTitel(),
                rule.getThema_titel()
        );
    }

    private int getFilteredCount(int rowIndex) {
        if (rowIndex < 0 || rowIndex >= filteredCounts.length) {
            return 0;
        }
        return filteredCounts[rowIndex];
    }

    private void updateFilteredCounts() {
        var rules = blacklistSnapshot();
        if (rules.isEmpty()) {
            filteredCounts = new int[0];
            return;
        }

        filteredCounts = new CompiledBlacklistMatcher(rules).countMatchesByRule(filmsSupplier.get());
    }

    private List<BlacklistRule> blacklistSnapshot() {
        synchronized (blacklist) {
            return new ArrayList<>(blacklist);
        }
    }
}
