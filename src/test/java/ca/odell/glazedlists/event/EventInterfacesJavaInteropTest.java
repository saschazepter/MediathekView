package ca.odell.glazedlists.event;

import ca.odell.glazedlists.BasicEventList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EventInterfacesJavaInteropTest {
    @Test
    void listEventListenerRemainsAJavaSamWithConsumerBridges() {
        try (final BasicEventList<String> source = new BasicEventList<>()) {
            final AtomicReference<ListEvent<String>> emitted = new AtomicReference<>();
            source.addListEventListener(emitted::set);
            source.add("value");

            final AtomicReference<ListEvent<String>> received = new AtomicReference<>();
            final ListEventListener<String> listener = received::set;
            listener.listChanged(emitted.get());
            assertSame(emitted.get(), received.get());

            accept(listener, emitted.get());
            assertSame(emitted.get(), received.get());
        }
    }

    @Test
    void listEventPublisherRemainsImplementableFromJava() {
        final List<String> calls = new ArrayList<>();
        final ListEventPublisher publisher = new ListEventPublisher() {
            @Override
            public void setRelatedSubject(Object listener, Object relatedSubject) {
                calls.add("setSubject:" + listener + ":" + relatedSubject);
            }

            @Override
            public void clearRelatedSubject(Object listener) {
                calls.add("clearSubject:" + listener);
            }

            @Override
            public void setRelatedListener(Object subject, Object relatedListener) {
                calls.add("setListener:" + subject + ":" + relatedListener);
            }

            @Override
            public void clearRelatedListener(Object subject, Object relatedListener) {
                calls.add("clearListener:" + subject + ":" + relatedListener);
            }
        };

        publisher.setRelatedSubject("listener", null);
        publisher.clearRelatedSubject("listener");
        publisher.setRelatedListener("subject", "related");
        publisher.clearRelatedListener("subject", "related");

        assertEquals(List.of(
                "setSubject:listener:null",
                "clearSubject:listener",
                "setListener:subject:related",
                "clearListener:subject:related"), calls);
    }

    @Test
    void interfaceMetadataAndDescriptorsRemainAvailable() throws ReflectiveOperationException {
        assertTrue(ListEventListener.class.isAnnotationPresent(FunctionalInterface.class));

        final Method listenerMethod = ListEventListener.class.getMethod("listChanged", ListEvent.class);
        final Method typedDefault = ListEventListener.class.getMethod("accept", ListEvent.class);
        final Method erasedBridge = ListEventListener.class.getMethod("accept", Object.class);
        assertFalse(listenerMethod.isDefault());
        assertTrue(typedDefault.isDefault());
        assertTrue(erasedBridge.isDefault());
        assertTrue(erasedBridge.isBridge());

        assertAbstractPublisherMethod("setRelatedSubject", Object.class, Object.class);
        assertAbstractPublisherMethod("clearRelatedSubject", Object.class);
        assertAbstractPublisherMethod("setRelatedListener", Object.class, Object.class);
        assertAbstractPublisherMethod("clearRelatedListener", Object.class, Object.class);
    }

    private static void accept(Consumer<ListEvent<String>> consumer, ListEvent<String> event) {
        consumer.accept(event);
    }

    private static void assertAbstractPublisherMethod(String name, Class<?>... parameterTypes)
            throws ReflectiveOperationException {
        final Method method = ListEventPublisher.class.getMethod(name, parameterTypes);
        assertTrue(Modifier.isAbstract(method.getModifiers()));
    }
}
