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

import com.jidesoft.swing.Searchable;
import com.jidesoft.swing.SearchableProvider;
import com.jidesoft.swing.event.SearchableEvent;

import javax.swing.*;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class ComboBoxSearchable2 extends Searchable implements ListDataListener, PropertyChangeListener, PopupMenuListener {

    public ComboBoxSearchable2(final JComboBox<?> comboBox) {
        super(comboBox);

        // to avoid conflict with default type-match feature of JComboBox.
        comboBox.setKeySelectionManager((key, model) -> -1);
        comboBox.getModel().addListDataListener(this);
        comboBox.addPropertyChangeListener("model", this);
        comboBox.addPopupMenuListener(this);

        if (comboBox.isEditable()) {
            final var textField = (JTextField) comboBox.getEditor().getEditorComponent();
            textField.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    if (e.getKeyChar() != KeyEvent.CHAR_UNDEFINED && e.getKeyCode() != KeyEvent.VK_ENTER
                            && e.getKeyCode() != KeyEvent.VK_ESCAPE) {
                        String text = textField.getText();
                        textChanged(text);
                    }
                }
            });
            setSearchableProvider(new MySearchableProvider(textField));
        }
    }

    @Override
    public void uninstallListeners() {
        super.uninstallListeners();
        if (_component instanceof JComboBox<?> cb) {
            cb.getModel().removeListDataListener(this);
            cb.removePopupMenuListener(this);
        }
        _component.removePropertyChangeListener("model", this);
    }

    @Override
    protected void setSelectedIndex(int index, boolean incremental) {
        var comboBox = (JComboBox<?>) _component;
        if (comboBox.getSelectedIndex() != index) {
            try {
                comboBox.setSelectedIndex(index);
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    protected int getSelectedIndex() {
        return ((JComboBox<?>) _component).getSelectedIndex();
    }

    @Override
    protected Object getElementAt(int index) {
        var comboBoxModel = ((JComboBox<?>) _component).getModel();
        return comboBoxModel.getElementAt(index);
    }

    @Override
    protected int getElementCount() {
        var comboBoxModel = ((JComboBox<?>) _component).getModel();
        return comboBoxModel.getSize();
    }

    /**
     * Converts the element in JCombobox to string. The returned value will be the <code>toString()</code> of whatever
     * element that returned from <code>list.getModel().getElementAt(i)</code>.
     *
     * @param object the object to be converted
     * @return the string representing the element in the JComboBox.
     */
    @Override
    protected String convertElementToString(Object object) {
        if (object != null) {
            return object.toString();
        } else {
            return "";
        }
    }

    public void contentsChanged(ListDataEvent e) {
        if (!isProcessModelChangeEvent()) {
            return;
        }
        if (e.getIndex0() == -1 && e.getIndex1() == -1) {
            //ignore
        } else {
            hidePopup();
            fireSearchableEvent(new SearchableEvent(this, SearchableEvent.SEARCHABLE_MODEL_CHANGE));
        }
    }

    public void intervalAdded(ListDataEvent e) {
        if (!isProcessModelChangeEvent()) {
            return;
        }
        hidePopup();
        fireSearchableEvent(new SearchableEvent(this, SearchableEvent.SEARCHABLE_MODEL_CHANGE));
    }

    public void intervalRemoved(ListDataEvent e) {
        if (!isProcessModelChangeEvent()) {
            return;
        }
        hidePopup();
        fireSearchableEvent(new SearchableEvent(this, SearchableEvent.SEARCHABLE_MODEL_CHANGE));
    }

    public void propertyChange(PropertyChangeEvent evt) {
        if ("model".equals(evt.getPropertyName())) {
            hidePopup();

            if (evt.getOldValue() instanceof ComboBoxModel<?> model) {
                model.removeListDataListener(this);
            }

            if (evt.getNewValue() instanceof ComboBoxModel<?> model) {
                model.addListDataListener(this);
            }
            fireSearchableEvent(new SearchableEvent(this, SearchableEvent.SEARCHABLE_MODEL_CHANGE));
        }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
        if (isHideSearchPopupOnEvent()) {
            hidePopup();
        }
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
    }

    private record MySearchableProvider(JTextField textField) implements SearchableProvider {

        @Override
        public String getSearchingText() {
            return textField.getText();
        }

        @Override
        public boolean isPassive() {
            return true;
        }

        @Override
        public void processKeyEvent(KeyEvent e) {
        }
    }
}