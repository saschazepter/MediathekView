package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEvent;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class TransformedListJavaInteropTest {
    @Test
    void protectedExtensionSurfaceAndJvmDescriptorsRemainStable() throws Exception {
        assertTrue(Modifier.isPublic(TransformedList.class.getModifiers()));
        assertTrue(Modifier.isAbstract(TransformedList.class.getModifiers()));

        final Field source = TransformedList.class.getDeclaredField("source");
        assertTrue(Modifier.isProtected(source.getModifiers()));
        assertFalse(Modifier.isFinal(source.getModifiers()));
        assertEquals(EventList.class, source.getType());

        final Constructor<?> constructor = TransformedList.class.getDeclaredConstructor(EventList.class);
        assertTrue(Modifier.isProtected(constructor.getModifiers()));

        final Method sourceIndex = TransformedList.class.getDeclaredMethod("getSourceIndex", int.class);
        assertTrue(Modifier.isProtected(sourceIndex.getModifiers()));
        assertFalse(Modifier.isFinal(sourceIndex.getModifiers()));
        assertProtectedAndAbstract("isWritable");
        final Method listChanged = TransformedList.class.getDeclaredMethod("listChanged", ListEvent.class);
        assertTrue(Modifier.isPublic(listChanged.getModifiers()));
        assertTrue(Modifier.isAbstract(listChanged.getModifiers()));
        assertPublicAndOverridable("add", int.class, Object.class);
        assertPublicAndOverridable("get", int.class);
        assertPublicAndOverridable("remove", int.class);
        assertPublicAndOverridable("set", int.class, Object.class);
        assertPublicAndOverridable("size");
        assertPublicAndOverridable("dispose");
    }

    @Test
    void javaSubclassesCanReadAndWriteTheSourceFieldAndOverrideCollectionMethods() {
        final BasicEventList<String> source = new BasicEventList<>();
        source.add("value");
        final JavaTransformedList transformed = new JavaTransformedList(source);

        assertSame(source, transformed.sourceField());
        assertEquals(1, transformed.size());
        //noinspection SequencedCollectionMethodCanBeUsed -- exercise the preserved remove(int) override
        assertEquals("value", transformed.remove(0));

        final BasicEventList<String> replacement = new BasicEventList<>();
        transformed.replaceSourceField(replacement);
        assertSame(replacement, transformed.sourceField());
    }


    private static void assertProtectedAndAbstract(String name, Class<?>... parameterTypes) throws Exception {
        final Method method = TransformedList.class.getDeclaredMethod(name, parameterTypes);
        assertTrue(Modifier.isProtected(method.getModifiers()));
        assertTrue(Modifier.isAbstract(method.getModifiers()));
    }


    private static void assertPublicAndOverridable(String name, Class<?>... parameterTypes) throws Exception {
        final Method method = TransformedList.class.getDeclaredMethod(name, parameterTypes);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertFalse(Modifier.isFinal(method.getModifiers()));
    }

    private static final class JavaTransformedList extends TransformedList<String, String> {
        private JavaTransformedList(EventList<String> source) {
            super(source);
        }

        private EventList<String> sourceField() {
            return source;
        }

        private void replaceSourceField(EventList<String> replacement) {
            source = replacement;
        }

        @Override
        protected boolean isWritable() {
            return true;
        }

        @Override
        public void listChanged(@NonNull ListEvent<String> listChanges) {
        }

        @Override
        public int size() {
            return super.size();
        }

        @Override
        public String remove(int index) {
            return super.remove(index);
        }
    }
}
