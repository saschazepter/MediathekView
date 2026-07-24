package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEvent;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class RangeListJavaInteropTest {
    @Test
    void preservesConstructionExtensionAndMethodSurface() throws Exception {
        final Constructor<?> constructor = RangeList.class.getDeclaredConstructor(EventList.class);
        assertTrue(Modifier.isPublic(constructor.getModifiers()));

        assertPublicAndOverridable("setHeadRange", int.class, int.class);
        assertPublicAndOverridable("setMiddleRange", int.class, int.class);
        assertPublicAndOverridable("setTailRange", int.class, int.class);
        assertPublicAndOverridable("getStartIndex");
        assertPublicAndOverridable("getEndIndex");

        assertFinal("listChanged", ListEvent.class);
        assertFinal("adjustRange");
        assertFinal("size");
        assertFinal("getSourceIndex", int.class);
        assertFinal("isWritable");
    }

    @Test
    void javaSubclassCanOverrideBoundsAndInvokeProtectedAdjustment() {
        final BasicEventList<String> source = new BasicEventList<>();
        source.addAll(java.util.List.of("A", "B", "C", "D"));
        final JavaRangeList transformed = new JavaRangeList(source);

        assertSame(source, transformed.sourceField());
        transformed.useBounds(1, 3);
        assertEquals(java.util.List.of("B", "C"), transformed);

        transformed.useBounds(0, 2);
        assertEquals(java.util.List.of("A", "B"), transformed);
    }

    private static void assertPublicAndOverridable(String name, Class<?>... parameterTypes) throws Exception {
        final Method method = RangeList.class.getDeclaredMethod(name, parameterTypes);
        assertTrue(Modifier.isPublic(method.getModifiers()));
        assertFalse(Modifier.isFinal(method.getModifiers()));
    }

    private static void assertFinal(String name, Class<?>... parameterTypes) throws Exception {
        final Method method = RangeList.class.getDeclaredMethod(name, parameterTypes);
        assertTrue(Modifier.isFinal(method.getModifiers()));
    }

    private static final class JavaRangeList extends RangeList<String> {
        private int start;
        private int end;

        private JavaRangeList(EventList<String> source) {
            super(source);
            end = source.size();
        }

        void useBounds(int start, int end) {
            this.start = start;
            this.end = end;
            adjustRange();
        }

        EventList<String> sourceField() {
            return source;
        }

        @Override
        public int getStartIndex() {
            return start;
        }

        @Override
        public int getEndIndex() {
            return end;
        }
    }
}
