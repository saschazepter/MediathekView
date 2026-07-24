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
@Suppress("UNCHECKED_CAST")
object GlazedLists {
    /** Groups the reusable stateless implementations returned by this facade. */
    private object Singletons {
        val BOOLEAN_COMPARATOR: Comparator<Boolean?> = BooleanComparator()
        val COMPARABLE_COMPARATOR: Comparator<*> = ComparableComparator<Comparable<Any?>>()
        val REVERSED_COMPARABLE: Comparator<*> =
            ReverseComparator(COMPARABLE_COMPARATOR as Comparator<Any?>)
        val STRING_TEXT_FILTERATOR: TextFilterator<Any?> = StringTextFilterator()
    }

    @JvmStatic
    fun <E> replaceAll(
        target: EventList<E>,
        source: List<@JvmSuppressWildcards E>,
        updates: Boolean,
    ) {
        Diff.replaceAll(target, source, updates)
    }

    @JvmStatic
    fun <E> replaceAll(
        target: EventList<E>,
        source: List<@JvmSuppressWildcards E>,
        updates: Boolean,
        comparator: Comparator<E>?,
    ) {
        Diff.replaceAll(target, source, updates, comparator)
    }

    @JvmStatic
    fun <E> replaceAllSorted(
        target: EventList<E>,
        source: Collection<@JvmSuppressWildcards E>,
        updates: Boolean,
        comparator: Comparator<E>?,
    ) {
        GlazedListsImpl.replaceAll(target, source, updates, comparator)
    }

    @JvmStatic
    fun <T> beanPropertyComparator(
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
    fun <T> beanPropertyComparator(
        className: Class<T>,
        property: String,
        propertyComparator: Comparator<*>,
    ): Comparator<T> = BeanPropertyComparator(className, property, propertyComparator) as Comparator<T>

    @JvmStatic
    fun booleanComparator(): Comparator<Boolean?> = Singletons.BOOLEAN_COMPARATOR

    @JvmStatic
    fun caseInsensitiveComparator(): Comparator<String> = String.CASE_INSENSITIVE_ORDER

    @JvmStatic
    fun <T> chainComparators(
        comparators: List<@JvmSuppressWildcards Comparator<T>>,
    ): Comparator<T> = ComparatorChain(comparators)

    @JvmStatic
    fun <T> chainComparators(vararg comparators: Comparator<T>): Comparator<T> =
        ComparatorChain<T>(comparators.toList())

    @JvmStatic
    fun <T> comparableComparator(): Comparator<T> where T : Comparable<T> =
        Singletons.COMPARABLE_COMPARATOR as Comparator<T>

    @JvmStatic
    fun <T> reverseComparator(): Comparator<T> where T : Comparable<T> =
        Singletons.REVERSED_COMPARABLE as Comparator<T>

    @JvmStatic
    fun <T> reverseComparator(forward: Comparator<T>?): Comparator<T> = ReverseComparator(forward!!)

    @JvmStatic
    fun <T> tableFormat(
        propertyNames: Array<String>?,
        columnLabels: Array<String>?,
    ): TableFormat<T> = BeanTableFormat(null, propertyNames!!, columnLabels!!)

    @JvmStatic
    fun <T> tableFormat(
        baseClass: Class<T>?,
        propertyNames: Array<String>?,
        columnLabels: Array<String>?,
    ): TableFormat<T> = BeanTableFormat(baseClass, propertyNames!!, columnLabels!!)

    @JvmStatic
    fun <T> tableFormat(
        propertyNames: Array<String>?,
        columnLabels: Array<String>?,
        editable: BooleanArray?,
    ): TableFormat<T> = BeanTableFormat(null, propertyNames!!, columnLabels!!, editable!!)

    @JvmStatic
    fun <T> tableFormat(
        baseClass: Class<T>?,
        propertyNames: Array<String>?,
        columnLabels: Array<String>?,
        editable: BooleanArray?,
    ): TableFormat<T> = BeanTableFormat(baseClass, propertyNames!!, columnLabels!!, editable!!)

    @JvmStatic
    fun <E> textFilterator(vararg propertyNames: String): TextFilterator<E> =
        BeanTextFilterator<Any?, E>(*propertyNames)

    @JvmStatic
    fun <E> textFilterator(
        beanClass: Class<E>,
        vararg propertyNames: String,
    ): TextFilterator<E> = BeanTextFilterator<Any?, E>(beanClass, *propertyNames)

    @JvmStatic
    fun <D, E> filterator(vararg propertyNames: String): Filterator<D, E> =
        BeanTextFilterator(*propertyNames)

    @JvmStatic
    fun <D, E> filterator(
        beanClass: Class<E>,
        vararg propertyNames: String,
    ): Filterator<D, E> = BeanTextFilterator(beanClass, *propertyNames)

    @JvmStatic
    fun <E> toStringTextFilterator(): TextFilterator<E> =
        Singletons.STRING_TEXT_FILTERATOR as TextFilterator<E>

    @JvmStatic
    fun <E> thresholdEvaluator(propertyName: String): ThresholdList.Evaluator<E?> =
        BeanThresholdEvaluator<Any>(propertyName) as ThresholdList.Evaluator<E?>

    @JvmStatic
    fun <E> listCollectionListModel(): CollectionList.Model<List<@JvmSuppressWildcards E>, E> =
        ListCollectionListModel()

    @JvmStatic
    fun <E> eventListOf(vararg contents: E): EventList<E> = eventList(contents.asList())

    @JvmStatic
    fun <E> eventList(contents: Collection<E>?): EventList<E> {
        val result = BasicEventList<E>(contents?.size ?: 0)
        if (contents != null) result.addAll(contents)
        return result
    }

    @JvmStatic
    fun <E> eventListOf(
        publisher: ListEventPublisher?,
        lock: ReadWriteLock?,
        vararg contents: E,
    ): EventList<E> = eventList(publisher, lock, contents.asList())

    @JvmStatic
    fun <E> eventList(
        publisher: ListEventPublisher?,
        lock: ReadWriteLock?,
        contents: Collection<E>?,
    ): EventList<E> {
        val result = BasicEventList<E>(contents?.size ?: 0, publisher, lock)
        if (contents != null) result.addAll(contents)
        return result
    }

    @JvmStatic
    fun <E> readOnlyList(source: EventList<out E>): TransformedList<E, E> =
        ReadOnlyList(source as EventList<E>)

    @JvmStatic
    fun <S, E> transformByFunction(
        source: EventList<S>,
        function: Function<S, E>,
    ): TransformedList<S, E> = SimpleFunctionList(source, function::apply)

    @JvmStatic
    fun <E> weakReferenceProxy(
        source: EventList<E>,
        target: ListEventListener<E>,
    ): ListEventListener<E> = WeakReferenceProxy(source, target)

    @JvmStatic
    fun <E> beanConnector(beanClass: Class<E>): ObservableElementList.Connector<E> =
        BeanConnector(beanClass)

    @JvmStatic
    fun <E> beanConnector(
        beanClass: Class<E>,
        matchPropertyNames: Boolean,
        vararg propertyNames: String,
    ): ObservableElementList.Connector<E> =
        beanConnector(beanClass, Matchers.propertyEventNameMatcher(matchPropertyNames, *propertyNames))

    @JvmStatic
    fun <E> beanConnector(
        beanClass: Class<E>,
        eventMatcher: Matcher<PropertyChangeEvent>,
    ): ObservableElementList.Connector<E> = BeanConnector(beanClass, eventMatcher)

    @JvmStatic
    fun <E> beanConnector(
        beanClass: Class<E>,
        addListener: String,
        removeListener: String,
    ): ObservableElementList.Connector<E> = BeanConnector(beanClass, addListener, removeListener)

    @JvmStatic
    fun <E> beanConnector(
        beanClass: Class<E>,
        addListener: String,
        removeListener: String,
        eventMatcher: Matcher<PropertyChangeEvent>,
    ): ObservableElementList.Connector<E> =
        BeanConnector(beanClass, addListener, removeListener, eventMatcher)

    @JvmStatic
    fun <E> observableConnector(): ObservableElementList.Connector<E>
        where E : ObservableConnector.PropertyChangeObservable = ObservableConnector()

    @JvmStatic
    fun <E> fixedMatcherEditor(matcher: Matcher<E>): MatcherEditor<E> = MatcherEditor.fromMatcher(matcher)

    @JvmStatic
    fun <E, V> constantFunction(value: V): Function<E, V> =
        Function(ConstantFunction<E, V>(value))

    @JvmStatic
    fun <E> toStringFunction(
        beanClass: Class<E>,
        propertyName: String,
    ): Function<E?, String?> {
        val function = StringBeanFunction<E>(beanClass, propertyName)
        return Function { value -> function(value!!) }
    }

    @JvmStatic
    fun <E, V> beanFunction(beanClass: Class<E>, propertyName: String): Function<E?, V> {
        val function = BeanFunction<E, V>(beanClass, propertyName)
        return Function { value -> function(value!!) }
    }

    @JvmStatic
    fun <E> syncEventListToList(source: EventList<E>, target: MutableList<E>): SyncListener<E> =
        SyncListener(source, target)

    @JvmStatic
    fun <E> typeSafetyListener(
        source: EventList<E>,
        types: Set<@JvmSuppressWildcards Class<*>?>,
    ): ListEventListener<E> = TypeSafetyListener(source, types)

    @JvmStatic
    fun <K, V> syncEventListToMultiMap(
        source: EventList<V>,
        keyMaker: Function<V, out K>,
    ): DisposableMap<K, MutableList<V>> where K : Comparable<K> =
        syncEventListToMultiMap(source, keyMaker, comparableComparator())

    @JvmStatic
    fun <K, V> syncEventListToMultiMap(
        source: EventList<V>,
        keyMaker: Function<V, out K>,
        keyGrouper: Comparator<in K>,
    ): DisposableMap<K, MutableList<V>> =
        GroupingListMultiMap(source, keyMaker::apply, keyGrouper) as DisposableMap<K, MutableList<V>>

    @JvmStatic
    fun <K, V> syncEventListToMap(
        source: EventList<V>,
        keyMaker: Function<V, K>,
    ): DisposableMap<K, V> = FunctionListMap(source, keyMaker::apply)
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
