package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import static org.junit.jupiter.api.Assertions.*;

class FreezableListJavaInteropTest {

    @Test
    void constructorAndLifecycleMethodsRemainJavaAccessible() throws NoSuchMethodException {
        final BasicEventList<String> source = new BasicEventList<>();
        source.add("value");
        final FreezableList<String> freezable = new FreezableList<>(source);
        final Method isFrozen = FreezableList.class.getDeclaredMethod("isFrozen");
        final Method freeze = FreezableList.class.getDeclaredMethod("freeze");
        final Method thaw = FreezableList.class.getDeclaredMethod("thaw");

        assertTrue(Modifier.isFinal(FreezableList.class.getModifiers()));
        assertTrue(Modifier.isPublic(FreezableList.class.getConstructor(EventList.class).getModifiers()));
        assertEquals(boolean.class, isFrozen.getReturnType());
        assertEquals(void.class, freeze.getReturnType());
        assertEquals(void.class, thaw.getReturnType());
        assertFalse(freezable.isFrozen());

        freezable.freeze();
        assertTrue(freezable.isFrozen());
        assertEquals("value", freezable.getFirst());
        assertEquals(1, freezable.size());

        freezable.thaw();
        assertFalse(freezable.isFrozen());
    }
}
