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

package mediathek.swing;

import com.jidesoft.swing.AutoCompletion;

import javax.swing.*;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.util.Objects;

/**
 * A strict searchable combo box that allows typing for quick search, but does not
 * treat key-navigation or in-progress auto-completion as a committed selection.
 *
 * The combo box remains editable so the user can search, while action events are
 * only forwarded for committed selections.
 */
public class StrictSearchComboBox extends JComboBox<String> {
    private AutoCompletion autoCompletion;
    private boolean committingSelection;
    private Object selectionBeforePopupOpened;

    public StrictSearchComboBox() {
        initialize();
    }

    private void initialize() {
        setEditable(true);

        autoCompletion = new AutoCompletion(this, new NoFireOnKeyComboBoxSearchable(this));
        autoCompletion.setStrict(true);
        autoCompletion.setStrictCompletion(true);
        setNoActionOnKeyNavigation(true);

        installCommitListeners();
    }

    public void setNoActionOnKeyNavigation(boolean value) {
        ((NoFireOnKeyComboBoxSearchable) autoCompletion.getSearchable()).setNoActionOnKeyNavigation(value);
    }

    private boolean isPreventingActionEvent() {
        return ((NoFireOnKeyComboBoxSearchable) autoCompletion.getSearchable()).isPreventActionEvent();
    }

    private void installCommitListeners() {
        var editorComponent = getEditor().getEditorComponent();
        if (editorComponent instanceof JTextField textField) {
            textField.addActionListener(_ -> fireCommittedActionEvent());
        }

        addPopupMenuListener(new PopupMenuListener() {
            @Override
            public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
                selectionBeforePopupOpened = getSelectedItem();
            }

            @Override
            public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
                if (!Objects.equals(selectionBeforePopupOpened, getSelectedItem())) {
                    fireCommittedActionEvent();
                }
            }

            @Override
            public void popupMenuCanceled(PopupMenuEvent e) {
                // no-op
            }
        });
    }

    private void fireCommittedActionEvent() {
        if (committingSelection) {
            return;
        }

        committingSelection = true;
        try {
            resetCaretPosition();
            super.fireActionEvent();
        } finally {
            committingSelection = false;
        }
    }

    private void resetCaretPosition() {
        var editorComponent = getEditor().getEditorComponent();
        if (!(editorComponent instanceof JTextField textField)) {
            return;
        }

        int textLength = textField.getText().length();
        if (textLength > 0) {
            textField.setCaretPosition(textLength);
        }
    }

    private void syncEditorWithSelection() {
        var editor = getEditor();
        if (editor == null) {
            return;
        }

        Object selectedItem = getSelectedItem();
        editor.setItem(selectedItem == null ? "" : selectedItem);
        resetCaretPosition();
    }

    @Override
    public void setSelectedItem(Object anObject) {
        super.setSelectedItem(anObject);
        syncEditorWithSelection();
    }

    @Override
    public void setSelectedIndex(int index) {
        super.setSelectedIndex(index);
        syncEditorWithSelection();
    }

    @Override
    protected void fireActionEvent() {
        if (isPreventingActionEvent() || !committingSelection) {
            return;
        }

        resetCaretPosition();
        super.fireActionEvent();
    }
}
