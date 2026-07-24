package ca.odell.glazedlists.impl.adt.barcode2;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class ElementJavaInteropTest {
    @Test
    void constantsRemainJavaCompileTimeFields() throws ReflectiveOperationException {
        assertEquals(0, Element.SORTED);
        assertEquals(1, Element.UNSORTED);
        assertEquals(2, Element.PENDING);

        assertConstantField("SORTED", 0);
        assertConstantField("UNSORTED", 1);
        assertConstantField("PENDING", 2);
    }

    @Test
    void javaImplementationCanUseEveryElementMethod() {
        MutableElement<String> first = new MutableElement<>((byte) 3, "first");
        MutableElement<String> second = new MutableElement<>((byte) 5, "second");
        first.next = second;
        second.previous = first;

        assertEquals("first", first.get());
        setNull(first);
        assertNull(first.get());
        assertEquals((byte) 3, first.getColor());

        first.setSorted(Element.PENDING);
        assertEquals(Element.PENDING, first.getSorted());
        assertSame(second, first.next());
        assertSame(first, second.previous());
        assertNull(first.previous());
        assertNull(second.next());
    }

    private static <V> void setNull(Element<V> element) {
        element.set(null);
    }

    private static void assertConstantField(String name, int value) throws ReflectiveOperationException {
        Field field = Element.class.getField(name);
        assertTrue(Modifier.isPublic(field.getModifiers()));
        assertTrue(Modifier.isStatic(field.getModifiers()));
        assertTrue(Modifier.isFinal(field.getModifiers()));
        assertEquals(value, field.getInt(null));
    }

    private static final class MutableElement<V> implements Element<V> {
        private final byte color;
        private V value;
        private int sorted = Element.SORTED;
        private Element<V> next;
        private Element<V> previous;

        private MutableElement(byte color, V value) {
            this.color = color;
            this.value = value;
        }

        @Override
        public V get() {
            return value;
        }

        @Override
        public void set(V value) {
            this.value = value;
        }

        @Override
        public byte getColor() {
            return color;
        }

        @Override
        public void setSorted(int sorted) {
            this.sorted = sorted;
        }

        @Override
        public int getSorted() {
            return sorted;
        }

        @Override
        public Element<V> next() {
            return next;
        }

        @Override
        public Element<V> previous() {
            return previous;
        }
    }
}
