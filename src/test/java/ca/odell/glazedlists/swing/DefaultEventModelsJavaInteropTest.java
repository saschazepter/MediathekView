package ca.odell.glazedlists.swing;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.event.ListEvent;
import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

import javax.swing.event.ListDataEvent;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@NullMarked
class DefaultEventModelsJavaInteropTest {
    @Test
    void constructorsAndSubclassHooksRemainAvailableToJava() {
        BasicEventList<String> source = new BasicEventList<>();
        JavaComboBoxModel<String> model = new JavaComboBoxModel<>(source);
        Object selection = new Object();

        model.setSelectedItem(selection);

        assertSame(selection, model.getSelectedItem());
        assertSame(source, model.sourceField());
        assertNotNull(model.reusableEventField());
        model.dispose();
    }

    private static final class JavaComboBoxModel<E> extends DefaultEventComboBoxModel<E> {
        private JavaComboBoxModel(EventList<E> source) {
            super(source, false);
        }

        @Override
        public void setSelectedItem(@Nullable Object selected) {
            super.setSelectedItem(selected);
        }

        @Override
        public void listChanged(ListEvent<E> listChanges) {
            super.listChanged(listChanges);
        }

        @Override
        protected void fireListDataEvent(ListDataEvent listDataEvent) {
            super.fireListDataEvent(listDataEvent);
        }

        private @Nullable EventList<E> sourceField() {
            return source;
        }

        private MutableListDataEvent reusableEventField() {
            return listDataEvent;
        }
    }
}
