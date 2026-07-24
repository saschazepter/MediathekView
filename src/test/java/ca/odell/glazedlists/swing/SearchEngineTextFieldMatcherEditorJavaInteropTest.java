package ca.odell.glazedlists.swing;

import ca.odell.glazedlists.TextFilterator;
import org.junit.jupiter.api.Test;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SearchEngineTextFieldMatcherEditorJavaInteropTest {
    @Test
    void javaSubclassRetainsNullableFilteratorAndOverridableDispose() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTextField textField = new JTextField();
            Subclass<String> editor = new Subclass<>(textField, null);

            assertFalse(editor.disposed);
            editor.dispose();
            assertTrue(editor.disposed);
        });
    }

    private static final class Subclass<E> extends SearchEngineTextFieldMatcherEditor<E> {
        private boolean disposed;

        private Subclass(JTextField textField, TextFilterator<? super E> textFilterator) {
            super(textField, textFilterator);
        }

        @Override
        public void dispose() {
            disposed = true;
            super.dispose();
        }
    }
}
