package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings("UseOfObsoleteDateTimeApi")
class SequencersJavaInteropTest {

    @Test
    void monthSequencerReturnsFreshInstancesWithoutAStaticBridge() throws NoSuchMethodException {
        final Method factory = Sequencers.class.getDeclaredMethod("monthSequencer");
        final SequenceList.Sequencer<Date> first = Sequencers.INSTANCE.monthSequencer();
        final SequenceList.Sequencer<Date> second = Sequencers.INSTANCE.monthSequencer();

        assertFalse(Modifier.isStatic(factory.getModifiers()));
        assertEquals(SequenceList.Sequencer.class, factory.getReturnType());
        assertNotSame(first, second);
        assertTrue(Modifier.isPrivate(first.getClass().getModifiers()));
        assertTrue(Modifier.isFinal(first.getClass().getModifiers()));
    }

    @Test
    void utilityFacadeIsExposedAsAKotlinObject() throws ReflectiveOperationException {
        assertTrue(Modifier.isFinal(Sequencers.class.getModifiers()));

        final Field instance = Sequencers.class.getDeclaredField("INSTANCE");

        assertTrue(Modifier.isPublic(instance.getModifiers()));
        assertTrue(Modifier.isStatic(instance.getModifiers()));
        assertTrue(Modifier.isFinal(instance.getModifiers()));
        assertSame(Sequencers.INSTANCE, instance.get(null));
        assertThrows(NoSuchFieldException.class, () -> Sequencers.class.getDeclaredField("Companion"));
    }
}
