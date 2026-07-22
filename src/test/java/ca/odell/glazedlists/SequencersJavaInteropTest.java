package ca.odell.glazedlists;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
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
    void utilityClassRemainsFinalAndRejectsReflectiveConstruction() throws NoSuchMethodException {
        assertTrue(Modifier.isFinal(Sequencers.class.getModifiers()));

        final Constructor<Sequencers> constructor = Sequencers.class.getDeclaredConstructor();
        assertTrue(Modifier.isPrivate(constructor.getModifiers()));
        constructor.setAccessible(true);

        final InvocationTargetException failure = assertThrows(InvocationTargetException.class, constructor::newInstance);
        assertInstanceOf(UnsupportedOperationException.class, failure.getCause());
    }
}
