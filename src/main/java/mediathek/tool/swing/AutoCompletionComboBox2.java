/*
 * Copyright (c) 2025 derreisende77.
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

package mediathek.tool.swing;

import com.jidesoft.swing.ComboBoxSearchable;

import javax.swing.*;

/**
 * A searchable combobox.
 */
public class AutoCompletionComboBox2 extends JComboBox<String> {
    protected ComboBoxSearchable _searchable;
    boolean _noActionOnKeyNavigation;
    private boolean _preventActionEvent;

    public AutoCompletionComboBox2() {
        initComponents();
    }

    protected void initComponents() {
        setEditable(false);
        setMaximumRowCount(8);
        _searchable = createSearchable();

        _searchable.setSearchLabel("Suchen nach:");
        _searchable.setFromStart(true);
    }

    public void setNoActionOnKeyNavigation(boolean _noActionOnKeyNavigation) {
        this._noActionOnKeyNavigation = _noActionOnKeyNavigation;
    }

    protected ComboBoxSearchable createSearchable() {
        return new NoFireOnKeyComboBoxSearchable(this);
    }

    @Override
    protected void fireActionEvent() {
        if (!_preventActionEvent) {
            super.fireActionEvent();
        }
    }

    public class NoFireOnKeyComboBoxSearchable extends ComboBoxSearchable {
        public NoFireOnKeyComboBoxSearchable(final JComboBox comboBox) {
            super(comboBox);
        }

        @Override
        protected void setSelectedIndex(int index, boolean incremental) {
            Object propTableCellEditor = _component.getClientProperty("JComboBox.isTableCellEditor");
            Object propNoActionOnKeyNavigation = UIManager.get("ComboBox.noActionOnKeyNavigation");
            if ((propTableCellEditor instanceof Boolean && (Boolean) propTableCellEditor) ||
                    (propNoActionOnKeyNavigation instanceof Boolean && (Boolean) propNoActionOnKeyNavigation) ||
                    _noActionOnKeyNavigation) {
                _preventActionEvent = true;
            }
            try {
                super.setSelectedIndex(index, incremental);
            } finally {
                _preventActionEvent = false;
            }
        }
    }
}