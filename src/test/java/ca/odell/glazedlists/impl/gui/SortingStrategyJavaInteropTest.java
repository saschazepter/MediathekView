package ca.odell.glazedlists.impl.gui;

import org.junit.jupiter.api.Test;
import org.jspecify.annotations.NonNull;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SortingStrategyJavaInteropTest {
    @Test
    void remainsAJavaFunctionalInterfaceWithAGenericAbstractMethodAndDefaultMethod() throws Exception {
        assertTrue(SortingStrategy.class.isAnnotationPresent(FunctionalInterface.class));
        final Method supportMethod = SortingStrategy.class.getMethod("supportsMultipleColumnSorting");
        assertTrue(supportMethod.isDefault());

        final AtomicInteger clickedColumn = new AtomicInteger(-1);
        final SortingStrategy strategy = new SortingStrategy() {
            @Override
            public <E> void columnClicked(@NonNull SortingState<E> sortingState, int column, int clicks, boolean shift, boolean control) {
                clickedColumn.set(column);
            }
        };

        assertTrue(strategy.supportsMultipleColumnSorting());
        strategy.columnClicked(new SortingState<String>(), 3, 1, false, false);
        assertEquals(3, clickedColumn.get());
    }
}
