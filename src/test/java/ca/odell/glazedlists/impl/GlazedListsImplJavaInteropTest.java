package ca.odell.glazedlists.impl;

import ca.odell.glazedlists.BasicEventList;
import ca.odell.glazedlists.EventList;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Comparator;
import kotlin.jvm.functions.Function1;

import static org.junit.jupiter.api.Assertions.*;

class GlazedListsImplJavaInteropTest {

    @Test
    void utilityMethodsRemainGenericWithoutStaticBridges() throws NoSuchMethodException {
        final Method replaceAll = GlazedListsImpl.class.getDeclaredMethod(
                "replaceAll",
                EventList.class,
                Collection.class,
                boolean.class,
                Comparator.class);
        final Method equalsComparator = GlazedListsImpl.class.getDeclaredMethod("equalsComparator");
        final Method identityFunction = GlazedListsImpl.class.getDeclaredMethod("identityFunction");
        final Comparator<String> equality = GlazedListsImpl.INSTANCE.equalsComparator();
        final Function1<String, String> identity = GlazedListsImpl.INSTANCE.identityFunction();
        final BasicEventList<String> target = new BasicEventList<>();

        GlazedListsImpl.INSTANCE.replaceAll(target, java.util.List.of("value"), false, null);

        assertFalse(Modifier.isStatic(replaceAll.getModifiers()));
        assertFalse(Modifier.isStatic(equalsComparator.getModifiers()));
        assertFalse(Modifier.isStatic(identityFunction.getModifiers()));
        assertEquals(1, replaceAll.getTypeParameters().length);
        assertEquals(1, equalsComparator.getTypeParameters().length);
        assertEquals(1, identityFunction.getTypeParameters().length);
        assertEquals(0, equality.compare("same", String.join("", "sa", "me")));
        assertEquals("value", identity.invoke("value"));
        assertEquals(java.util.List.of("value"), target);
    }

    @Test
    void utilityFacadeIsExposedAsAKotlinObject() throws ReflectiveOperationException {
        assertTrue(Modifier.isFinal(GlazedListsImpl.class.getModifiers()));

        final Field instance = GlazedListsImpl.class.getDeclaredField("INSTANCE");

        assertTrue(Modifier.isPublic(instance.getModifiers()));
        assertTrue(Modifier.isStatic(instance.getModifiers()));
        assertTrue(Modifier.isFinal(instance.getModifiers()));
        assertSame(GlazedListsImpl.INSTANCE, instance.get(null));
        assertThrows(NoSuchFieldException.class, () -> GlazedListsImpl.class.getDeclaredField("Companion"));
    }
}
