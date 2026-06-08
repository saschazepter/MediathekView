package mediathek.gui.dialogEinstellungen;

import mediathek.daten.blacklist.BlacklistRule;
import mediathek.daten.blacklist.ListeBlacklist;
import org.jspecify.annotations.NonNull;

import javax.swing.table.AbstractTableModel;
import java.util.List;

public class BlacklistRuleTableModel extends AbstractTableModel {
    private static final int BLACKLIST_SENDER = 0;
    private static final int BLACKLIST_THEMA = 1;
    private static final int BLACKLIST_TITEL = 2;
    private static final int BLACKLIST_THEMA_TITEL = 3;
    private final ListeBlacklist blacklist;

    public BlacklistRuleTableModel(@NonNull ListeBlacklist blacklist) {
        this.blacklist = blacklist;
    }

    @Override
    public int getRowCount() {
        return blacklist.size();
    }

    @Override
    public int getColumnCount() {
        return 4;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        var rule = blacklist.get(rowIndex);
        return switch (columnIndex) {
            case BLACKLIST_SENDER -> rule.getSender();
            case BLACKLIST_THEMA -> rule.getThema();
            case BLACKLIST_TITEL -> rule.getTitel();
            case BLACKLIST_THEMA_TITEL -> rule.getThema_titel();
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
            default -> throw new IllegalStateException("Unexpected value: " + column);
        };
    }

    /**
     * Remove a BlacklistRule from model
     *
     * @param modelIndex index from blacklist to delete
     */
    public void removeRow(int modelIndex) {
        blacklist.remove(modelIndex);
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
        fireTableDataChanged();
    }

    /**
     * Remove all blacklist rules.
     */
    public void removeAll() {
        blacklist.clear();
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
        fireTableRowsUpdated(modelIndex, modelIndex);
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
}
