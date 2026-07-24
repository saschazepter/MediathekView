package ca.odell.glazedlists.matchers

import ca.odell.glazedlists.BasicEventList
import ca.odell.glazedlists.FilterList
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier
import java.util.function.Consumer
import java.util.function.Predicate

internal class MatcherContractMigrationBehaviorTest {
    @Test
    fun matcherRemainsAJdkPredicateAndSam() {
        val matcher = Matcher<String> { value -> value.length > 2 }
        val predicate: Predicate<String> = matcher

        assertFalse(predicate.test("ab"))
        assertTrue(predicate.test("abc"))
        assertTrue(Matcher::class.java.getMethod("test", Any::class.java).isDefault)
    }

    @Test
    fun listenerRemainsAJdkConsumerAndSam() {
        val matcher = Matcher<String> { true }
        val editor = MatcherEditor.fromMatcher(matcher)
        val event = MatcherEditor.Event(editor, MatcherEditor.Event.CHANGED, matcher)
        var received: MatcherEditor.Event<String>? = null
        val listener = MatcherEditor.Listener { event: MatcherEditor.Event<String> -> received = event }
        val consumer: Consumer<MatcherEditor.Event<String>> = listener

        consumer.accept(event)

        assertSame(event, received)
        assertTrue(
            MatcherEditor.Listener::class.java
                .getMethod("accept", MatcherEditor.Event::class.java)
                .isDefault,
        )
    }

    @Test
    fun companionFactoryKeepsMatcherIdentityWithoutAStaticBridge() {
        val matcher = Matcher<String> { value -> value.isNotEmpty() }

        val editor = MatcherEditor.fromMatcher(matcher)

        assertSame(matcher, editor.matcher)
        assertFalse(
            Modifier.isStatic(
                MatcherEditor.Companion::class.java.getMethod("fromMatcher", Matcher::class.java).modifiers,
            ),
        )
    }

    @Test
    fun eventsKeepBothSourceShapesAndConstants() {
        val matcher = Matcher<String> { true }
        val editor = MatcherEditor.fromMatcher(matcher)
        val editorEvent = MatcherEditor.Event(editor, MatcherEditor.Event.CONSTRAINED, matcher)
        val source = BasicEventList<String>()
        val filterList = FilterList(source)

        try {
            val filterEvent = MatcherEditor.Event(filterList, MatcherEditor.Event.RELAXED, matcher)

            assertSame(editor, editorEvent.matcherEditor)
            assertSame(editor, editorEvent.source)
            assertSame(matcher, editorEvent.matcher)
            assertEquals(MatcherEditor.Event.CONSTRAINED, editorEvent.type)
            assertNull(filterEvent.matcherEditor)
            assertSame(filterList, filterEvent.source)
            assertEquals(
                listOf(0, 1, 2, 3, 4),
                listOf(
                    MatcherEditor.Event.MATCH_ALL,
                    MatcherEditor.Event.MATCH_NONE,
                    MatcherEditor.Event.CONSTRAINED,
                    MatcherEditor.Event.RELAXED,
                    MatcherEditor.Event.CHANGED,
                ),
            )
        } finally {
            filterList.dispose()
        }
    }
}
