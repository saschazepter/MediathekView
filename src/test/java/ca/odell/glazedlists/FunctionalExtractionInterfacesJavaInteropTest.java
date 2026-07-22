package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FunctionalExtractionInterfacesJavaInteropTest {
    @Test
    void javaLambdasRemainAvailable() {
        final List<Integer> extracted = new ArrayList<>();
        final Filterator<Integer, String> filterator =
                (target, element) -> target.add(element.length());
        filterator.getFilterValues(extracted, "value");

        final AtomicReference<Object> changedElement = new AtomicReference<>();
        final ObservableElementChangeHandler<String> changeHandler = changedElement::set;
        changeHandler.elementChanged(null);
        assertNull(changedElement.get());
        changeHandler.elementChanged(42);

        assertEquals(List.of(5), extracted);
        assertEquals(42, changedElement.get());
    }

    @Test
    void jdkConsumerBridgesRemainAvailable() {
        final List<String> strings = new ArrayList<>();
        final TextFilterator<String> textFilterator =
                (target, element) -> target.add("text:" + element);
        textFilterator.getFilterStrings(strings, "one");
        accept(textFilterator, strings);

        final TextFilterable textFilterable = target -> target.add("self");
        textFilterable.getFilterStrings(strings);
        accept(textFilterable, strings);

        assertEquals(List.of("text:one", "text:two", "self", "self"), strings);
    }

    @Test
    void functionalInterfaceAndDefaultMethodMetadataRemainAvailable() throws ReflectiveOperationException {
        assertFunctionalInterface(Filterator.class);
        assertFunctionalInterface(TextFilterator.class);
        assertFunctionalInterface(TextFilterable.class);
        assertFunctionalInterface(ObservableElementChangeHandler.class);

        final Method textFilteratorMethod =
                TextFilterator.class.getMethod("getFilterStrings", List.class, Object.class);
        final Method textFilteratorDefault =
                TextFilterator.class.getMethod("accept", List.class, Object.class);
        final Method textFilteratorBridge =
                TextFilterator.class.getMethod("accept", Object.class, Object.class);
        assertFalse(textFilteratorMethod.isDefault());
        assertTrue(textFilteratorDefault.isDefault());
        assertTrue(textFilteratorBridge.isDefault());
        assertTrue(textFilteratorBridge.isBridge());

        final Method textFilterableMethod =
                TextFilterable.class.getMethod("getFilterStrings", List.class);
        final Method textFilterableDefault =
                TextFilterable.class.getMethod("accept", List.class);
        final Method textFilterableBridge =
                TextFilterable.class.getMethod("accept", Object.class);
        assertFalse(textFilterableMethod.isDefault());
        assertTrue(textFilterableDefault.isDefault());
        assertTrue(textFilterableBridge.isDefault());
        assertTrue(textFilterableBridge.isBridge());
    }

    private static void assertFunctionalInterface(Class<?> type) {
        assertTrue(type.isAnnotationPresent(FunctionalInterface.class));
    }

    private static void accept(BiConsumer<List<String>, String> consumer, List<String> values) {
        consumer.accept(values, "two");
    }

    private static void accept(Consumer<List<String>> consumer, List<String> values) {
        consumer.accept(values);
    }
}
