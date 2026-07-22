package ca.odell.glazedlists.impl;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class GlazedListsImplJavaInteropTest {

    @Test
    void utilityMethodsRemainJavaStaticAndGeneric() throws NoSuchMethodException {
        final Method replaceAll = GlazedListsImpl.class.getDeclaredMethod(
                "replaceAll",
                EventList.class,
                Collection.class,
                boolean.class,
                Comparator.class);
        final Method equalsComparator = GlazedListsImpl.class.getDeclaredMethod("equalsComparator");
        final Method identityFunction = GlazedListsImpl.class.getDeclaredMethod("identityFunction");
        final Comparator<String> equality = GlazedListsImpl.equalsComparator();
        final Function<String, String> identity = GlazedListsImpl.identityFunction();
        final BasicEventList<String> target = new BasicEventList<>();

        GlazedListsImpl.replaceAll(target, java.util.List.of("value"), false, null);

        assertTrue(Modifier.isStatic(replaceAll.getModifiers()));
        assertTrue(Modifier.isStatic(equalsComparator.getModifiers()));
        assertTrue(Modifier.isStatic(identityFunction.getModifiers()));
        assertEquals(1, replaceAll.getTypeParameters().length);
        assertEquals(1, equalsComparator.getTypeParameters().length);
        assertEquals(1, identityFunction.getTypeParameters().length);
        assertEquals(0, equality.compare("same", String.join("", "sa", "me")));
        assertEquals("value", identity.apply("value"));
        assertEquals(java.util.List.of("value"), target);
    }

    @Test
    void utilityClassRemainsFinalAndRejectsReflectiveConstruction() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(GlazedListsImpl.class.getModifiers()));

        final Constructor<GlazedListsImpl> constructor = GlazedListsImpl.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);

        final InvocationTargetException failure = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, failure.getCause());
    }
}
