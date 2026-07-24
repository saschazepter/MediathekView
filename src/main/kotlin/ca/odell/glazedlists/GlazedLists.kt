/*
 * Copyright (c) 2026 derreisende77.
 * This code was developed as part of the MediathekView project https://github.com/mediathekview/MediathekView
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package ca.odell.glazedlists

import ca.odell.glazedlists.event.ListEventListener
import ca.odell.glazedlists.event.ListEventPublisher
import ca.odell.glazedlists.gui.TableFormat
import ca.odell.glazedlists.impl.Diff
import ca.odell.glazedlists.impl.FunctionListMap
import ca.odell.glazedlists.impl.GlazedListsImpl
import ca.odell.glazedlists.impl.GroupingListMultiMap
import ca.odell.glazedlists.impl.ListCollectionListModel
import ca.odell.glazedlists.impl.ObservableConnector
import ca.odell.glazedlists.impl.ReadOnlyList
import ca.odell.glazedlists.impl.SimpleFunctionList
import ca.odell.glazedlists.impl.TypeSafetyListener
import ca.odell.glazedlists.impl.WeakReferenceProxy
import ca.odell.glazedlists.impl.beans.BeanConnector
import ca.odell.glazedlists.impl.beans.BeanFunction
import ca.odell.glazedlists.impl.beans.BeanTableFormat
import ca.odell.glazedlists.impl.beans.BeanTextFilterator
import ca.odell.glazedlists.impl.beans.BeanThresholdEvaluator
import ca.odell.glazedlists.impl.beans.StringBeanFunction
import ca.odell.glazedlists.impl.filter.StringTextFilterator
import ca.odell.glazedlists.impl.functions.ConstantFunction
import ca.odell.glazedlists.impl.sort.BeanPropertyComparator
import ca.odell.glazedlists.impl.sort.BooleanComparator
import ca.odell.glazedlists.impl.sort.ComparableComparator
import ca.odell.glazedlists.impl.sort.ComparatorChain
import ca.odell.glazedlists.impl.sort.ReverseComparator
import ca.odell.glazedlists.matchers.Matcher
import ca.odell.glazedlists.matchers.MatcherEditor
import ca.odell.glazedlists.matchers.Matchers
import java.beans.PropertyChangeEvent
import java.util.Comparator
import java.util.concurrent.locks.ReadWriteLock
import java.util.function.Function

/** A factory for creating objects used with Glazed Lists. */
@Suppress("NON_FINAL_MEMBER_IN_OBJECT", "UNCHECKED_CAST")
class GlazedLists private constructor() {
    init {
        throw UnsupportedOperationException()
    }

    /** Keeps mutable singleton storage out of the companion and its synthetic accessors. */
    private object Singletons {
        val BOOLEAN_COMPARATOR: Comparator<Boolean?> = BooleanComparator()
        val COMPARABLE_COMPARATOR: Comparator<*> = ComparableComparator<Comparable<Any?>>()
        val REVERSED_COMPARABLE: Comparator<*> =
            ReverseComparator(COMPARABLE_COMPARATOR as Comparator<Any?>)
        val STRING_TEXT_FILTERATOR: TextFilterator<Any?> = StringTextFilterator()
    }

    companion object {
        @JvmStatic
        open fun <E> replaceAll(
            target: EventList<E>,
            source: List<@JvmSuppressWildcards E>,
            updates: Boolean,
        ) {
            Diff.replaceAll(target, source, updates)
        }

        @JvmStatic
        open fun <E> replaceAll(
            target: EventList<E>,
            source: List<@JvmSuppressWildcards E>,
            updates: Boolean,
            comparator: Comparator<E>?,
        ) {
            Diff.replaceAll(target, source, updates, comparator)
        }

        @JvmStatic
        open fun <E> replaceAllSorted(
            target: EventList<E>,
            source: Collection<@JvmSuppressWildcards E>,
            updates: Boolean,
            comparator: Comparator<E>?,
        ) {
            GlazedListsImpl.replaceAll(target, source, updates, comparator)
        }

        @JvmStatic
        open fun <T> beanPropertyComparator(
            clazz: Class<T>,
            property: String,
            vararg properties: String,
        ): Comparator<T> {
            val firstComparator = beanPropertyComparator(clazz, property, Singletons.COMPARABLE_COMPARATOR)
            if (properties.isEmpty()) return firstComparator
            return chainComparators(
                buildList(properties.size + 1) {
                    add(firstComparator)
                    properties.forEach {
                        add(beanPropertyComparator(clazz, it, Singletons.COMPARABLE_COMPARATOR))
                    }
                },
            )
        }

        @JvmStatic
        open fun <T> beanPropertyComparator(
            className: Class<T>,
            property: String,
            propertyComparator: Comparator<*>,
        ): Comparator<T> = BeanPropertyComparator(className, property, propertyComparator) as Comparator<T>

        @JvmStatic
        open fun booleanComparator(): Comparator<Boolean?> = Singletons.BOOLEAN_COMPARATOR

        @JvmStatic
        open fun caseInsensitiveComparator(): Comparator<String> = String.CASE_INSENSITIVE_ORDER

        @JvmStatic
        open fun <T> chainComparators(
            comparators: List<@JvmSuppressWildcards Comparator<T>>,
        ): Comparator<T> = ComparatorChain(comparators)

        @JvmStatic
        open fun <T> chainComparators(vararg comparators: Comparator<T>): Comparator<T> =
            ComparatorChain<T>(comparators.toList())

        @JvmStatic
        open fun <T> comparableComparator(): Comparator<T> where T : Comparable<T> =
            Singletons.COMPARABLE_COMPARATOR as Comparator<T>

        @JvmStatic
        open fun <T> reverseComparator(): Comparator<T> where T : Comparable<T> =
            Singletons.REVERSED_COMPARABLE as Comparator<T>

        @JvmStatic
        open fun <T> reverseComparator(forward: Comparator<T>?): Comparator<T> = ReverseComparator(forward!!)

        @JvmStatic
        open fun <T> tableFormat(
            propertyNames: Array<String>?,
            columnLabels: Array<String>?,
        ): TableFormat<T> = BeanTableFormat(null, propertyNames!!, columnLabels!!)

        @JvmStatic
        open fun <T> tableFormat(
            baseClass: Class<T>?,
            propertyNames: Array<String>?,
            columnLabels: Array<String>?,
        ): TableFormat<T> = BeanTableFormat(baseClass, propertyNames!!, columnLabels!!)

        @JvmStatic
        open fun <T> tableFormat(
            propertyNames: Array<String>?,
            columnLabels: Array<String>?,
            editable: BooleanArray?,
        ): TableFormat<T> = BeanTableFormat(null, propertyNames!!, columnLabels!!, editable!!)

        @JvmStatic
        open fun <T> tableFormat(
            baseClass: Class<T>?,
            propertyNames: Array<String>?,
            columnLabels: Array<String>?,
            editable: BooleanArray?,
        ): TableFormat<T> = BeanTableFormat(baseClass, propertyNames!!, columnLabels!!, editable!!)

        @JvmStatic
        open fun <E> textFilterator(vararg propertyNames: String): TextFilterator<E> =
            BeanTextFilterator<Any?, E>(*propertyNames)

        @JvmStatic
        open fun <E> textFilterator(
            beanClass: Class<E>,
            vararg propertyNames: String,
        ): TextFilterator<E> = BeanTextFilterator<Any?, E>(beanClass, *propertyNames)

        @JvmStatic
        open fun <D, E> filterator(vararg propertyNames: String): Filterator<D, E> =
            BeanTextFilterator(*propertyNames)

        @JvmStatic
        open fun <D, E> filterator(
            beanClass: Class<E>,
            vararg propertyNames: String,
        ): Filterator<D, E> = BeanTextFilterator(beanClass, *propertyNames)

        @JvmStatic
        open fun <E> toStringTextFilterator(): TextFilterator<E> =
            Singletons.STRING_TEXT_FILTERATOR as TextFilterator<E>

        @JvmStatic
        open fun <E> thresholdEvaluator(propertyName: String): ThresholdList.Evaluator<E?> =
            BeanThresholdEvaluator<Any>(propertyName) as ThresholdList.Evaluator<E?>

        @JvmStatic
        open fun <E> listCollectionListModel(): CollectionList.Model<List<@JvmSuppressWildcards E>, E> =
            ListCollectionListModel()

        @JvmStatic
        open fun <E> eventListOf(vararg contents: E): EventList<E> = eventList(contents.asList())

        @JvmStatic
        open fun <E> eventList(contents: Collection<E>?): EventList<E> {
            val result = BasicEventList<E>(contents?.size ?: 0)
            if (contents != null) result.addAll(contents)
            return result
        }

        @JvmStatic
        open fun <E> eventListOf(
            publisher: ListEventPublisher?,
            lock: ReadWriteLock?,
            vararg contents: E,
        ): EventList<E> = eventList(publisher, lock, contents.asList())

        @JvmStatic
        open fun <E> eventList(
            publisher: ListEventPublisher?,
            lock: ReadWriteLock?,
            contents: Collection<E>?,
        ): EventList<E> {
            val result = BasicEventList<E>(contents?.size ?: 0, publisher, lock)
            if (contents != null) result.addAll(contents)
            return result
        }

        @JvmStatic
        open fun <E> readOnlyList(source: EventList<out E>): TransformedList<E, E> =
            ReadOnlyList(source as EventList<E>)

        @JvmStatic
        open fun <S, E> transformByFunction(
            source: EventList<S>,
            function: Function<S, E>,
        ): TransformedList<S, E> = SimpleFunctionList(source, function)

        @JvmStatic
        open fun <E> weakReferenceProxy(
            source: EventList<E>,
            target: ListEventListener<E>,
        ): ListEventListener<E> = WeakReferenceProxy(source, target)

        @JvmStatic
        open fun <E> beanConnector(beanClass: Class<E>): ObservableElementList.Connector<E> =
            BeanConnector(beanClass)

        @JvmStatic
        open fun <E> beanConnector(
            beanClass: Class<E>,
            matchPropertyNames: Boolean,
            vararg propertyNames: String,
        ): ObservableElementList.Connector<E> =
            beanConnector(beanClass, Matchers.propertyEventNameMatcher(matchPropertyNames, *propertyNames))

        @JvmStatic
        open fun <E> beanConnector(
            beanClass: Class<E>,
            eventMatcher: Matcher<PropertyChangeEvent>,
        ): ObservableElementList.Connector<E> = BeanConnector(beanClass, eventMatcher)

        @JvmStatic
        open fun <E> beanConnector(
            beanClass: Class<E>,
            addListener: String,
            removeListener: String,
        ): ObservableElementList.Connector<E> = BeanConnector(beanClass, addListener, removeListener)

        @JvmStatic
        open fun <E> beanConnector(
            beanClass: Class<E>,
            addListener: String,
            removeListener: String,
            eventMatcher: Matcher<PropertyChangeEvent>,
        ): ObservableElementList.Connector<E> =
            BeanConnector(beanClass, addListener, removeListener, eventMatcher)

        @JvmStatic
        open fun <E> observableConnector(): ObservableElementList.Connector<E>
            where E : ObservableConnector.PropertyChangeObservable = ObservableConnector()

        @JvmStatic
        open fun <E> fixedMatcherEditor(matcher: Matcher<E>): MatcherEditor<E> = MatcherEditor.fromMatcher(matcher)

        @JvmStatic
        open fun <E, V> constantFunction(value: V): Function<E, V> = ConstantFunction(value)

        @JvmStatic
        open fun <E> toStringFunction(
            beanClass: Class<E>,
            propertyName: String,
        ): Function<E?, String?> = StringBeanFunction<E>(beanClass, propertyName) as Function<E?, String?>

        @JvmStatic
        open fun <E, V> beanFunction(beanClass: Class<E>, propertyName: String): Function<E?, V> =
            BeanFunction<E, V>(beanClass, propertyName) as Function<E?, V>

        @JvmStatic
        open fun <E> syncEventListToList(source: EventList<E>, target: MutableList<E>): SyncListener<E> =
            SyncListener(source, target)

        @JvmStatic
        open fun <E> typeSafetyListener(
            source: EventList<E>,
            types: Set<@JvmSuppressWildcards Class<*>?>,
        ): ListEventListener<E> = TypeSafetyListener(source, types)

        @JvmStatic
        open fun <K, V> syncEventListToMultiMap(
            source: EventList<V>,
            keyMaker: Function<V, out K>,
        ): DisposableMap<K, MutableList<V>> where K : Comparable<K> =
            syncEventListToMultiMap(source, keyMaker, comparableComparator())

        @JvmStatic
        open fun <K, V> syncEventListToMultiMap(
            source: EventList<V>,
            keyMaker: Function<V, out K>,
            keyGrouper: Comparator<in K>,
        ): DisposableMap<K, MutableList<V>> =
            GroupingListMultiMap(source, keyMaker, keyGrouper) as DisposableMap<K, MutableList<V>>

        @JvmStatic
        open fun <K, V> syncEventListToMap(
            source: EventList<V>,
            keyMaker: Function<V, K>,
        ): DisposableMap<K, V> = FunctionListMap(source, keyMaker)
    }
}

/**
 * Restores the Java platform-type source ergonomics that allow assigning a read-only Kotlin list
 * to a list-valued map while retaining mutable group values returned by the facade.
 */
@Suppress("UNCHECKED_CAST")
@JvmSynthetic
operator fun <K, V> DisposableMap<K, MutableList<V>>.set(key: K, value: List<V>) {
    (this as DisposableMap<K, List<V>>).put(key, value)
}
