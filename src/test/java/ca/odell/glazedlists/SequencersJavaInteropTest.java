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
    void monthSequencerRemainsAStaticFactoryReturningFreshInstances() throws NoSuchMethodException {
        final Method factory = Sequencers.class.getDeclaredMethod("monthSequencer");
        final SequenceList.Sequencer<Date> first = Sequencers.monthSequencer();
        final SequenceList.Sequencer<Date> second = Sequencers.monthSequencer();

        assertTrue(Modifier.isStatic(factory.getModifiers()));
        assertEquals(SequenceList.Sequencer.class, factory.getReturnType());
        assertNotSame(first, second);
        assertTrue(Modifier.isPrivate(first.getClass().getModifiers()));
        assertTrue(Modifier.isFinal(first.getClass().getModifiers()));
    }

    @Test
    void monthSequencerRejectsNullWithTheOriginalFailureContract() {
        final SequenceList.Sequencer<Date> sequencer = Sequencers.monthSequencer();

        final IllegalArgumentException previousFailure = assertThrows(
                IllegalArgumentException.class,
                () -> sequencer.previous(null));
        assertEquals("date may not be null", previousFailure.getMessage());

        final IllegalArgumentException nextFailure = assertThrows(
                IllegalArgumentException.class,
                () -> sequencer.next(null));
        assertEquals("date may not be null", nextFailure.getMessage());
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
