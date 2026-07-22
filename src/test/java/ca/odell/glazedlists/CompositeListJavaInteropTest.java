package ca.odell.glazedlists;

import ca.odell.glazedlists.event.ListEventPublisher;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.locks.ReadWriteLock;

import static org.junit.jupiter.api.Assertions.*;

@SuppressWarnings({"resource", "MismatchedQueryAndUpdateOfCollection"})
class CompositeListJavaInteropTest {

    @Test
    void constructorsAndClassRemainPublicAndExtensible() throws NoSuchMethodException {
        assertTrue(Modifier.isPublic(CompositeList.class.getModifiers()));
        assertFalse(Modifier.isFinal(CompositeList.class.getModifiers()));

        final Constructor<?> defaultConstructor = CompositeList.class.getConstructor();
        final Constructor<?> infrastructureConstructor = CompositeList.class.getConstructor(
                ListEventPublisher.class,
                ReadWriteLock.class);

        assertTrue(Modifier.isPublic(defaultConstructor.getModifiers()));
        assertTrue(Modifier.isPublic(infrastructureConstructor.getModifiers()));
        assertInstanceOf(CompositeList.class, new CompositeListSubclass());
    }

    @Test
    void createMemberListKeepsItsIndependentMethodTypeParameter() throws NoSuchMethodException {
        final CompositeList<String> composite = new CompositeList<>();
        final EventList<Integer> numbers = composite.createMemberList();
        final Method factory = CompositeList.class.getMethod("createMemberList");
        final Method addMember = CompositeList.class.getMethod("addMemberList", EventList.class);
        final Method removeMember = CompositeList.class.getMethod("removeMemberList", EventList.class);

        assertEquals(1, factory.getTypeParameters().length);
        assertEquals(EventList.class, factory.getReturnType());
        assertFalse(Modifier.isFinal(factory.getModifiers()));
        assertFalse(Modifier.isFinal(addMember.getModifiers()));
        assertFalse(Modifier.isFinal(removeMember.getModifiers()));
        assertSame(composite.getPublisher(), numbers.getPublisher());
        assertSame(composite.getReadWriteLock(), numbers.getReadWriteLock());
    }

    @Test
    void infrastructureConstructorKeepsAcceptingNullFallbacks() {
        final CompositeList<String> composite = new CompositeList<>(null, null);

        assertNotNull(composite.getPublisher());
        assertNotNull(composite.getReadWriteLock());
    }

    private static final class CompositeListSubclass extends CompositeList<String> {
    }
}
