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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package ca.odell.glazedlists.impl

import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.TransformedList
import ca.odell.glazedlists.event.ListEvent
import mediathek.tool.withReadLock
import mediathek.tool.withWriteLock
import java.util.Collections
import java.util.function.Consumer
import java.util.function.Predicate
import java.util.function.UnaryOperator

/**
 * An [EventList] that obtains its read-write lock for all operations.
 *
 * This provides some support for sharing [EventList]s between multiple threads.
 * Using a [ThreadSafeList] for concurrent access to lists can be expensive because a lock is
 * acquired and released for every operation.
 *
 * Although this class provides thread-safe access, it does not guarantee that changes will not
 * happen between method calls. For example, checking [size] and then calling [get] is not atomic.
 *
 * The objects returned by [iterator], [subList], [stream], [parallelStream], and [spliterator]
 * are not thread-safe.
 *
 * @author Kevin Maltby
 */
class ThreadSafeList<E>(source: EventList<E>) : TransformedList<E, E>(source) {
    init {
        source.addListEventListener(this)
    }

    override fun listChanged(listChanges: ListEvent<E>) {
        updates.forwardEvent(listChanges)
    }

    override fun get(index: Int): E = withReadLock { source[index] }

    override val size: Int
        get() = withReadLock { source.size }

    override fun isWritable(): Boolean = true

    override fun contains(element: E): Boolean = withReadLock { source.contains(element) }

    override fun containsAll(elements: Collection<E>): Boolean =
        withReadLock { HashSet(source).containsAll(elements) }

    override fun equals(other: Any?): Boolean = withReadLock { source == other }

    override fun hashCode(): Int = withReadLock { source.hashCode() }

    override fun indexOf(element: E): Int = withReadLock { source.indexOf(element) }

    override fun lastIndexOf(element: E): Int = withReadLock { source.lastIndexOf(element) }

    override fun isEmpty(): Boolean = withReadLock { source.isEmpty() }

    override fun forEach(action: Consumer<in E>) {
        withReadLock { source.forEach(action) }
    }

    override fun toArray(): Array<Any?> = withReadLock {
        Array(source.size) { index -> source[index] }
    }

    override fun <T> toArray(array: Array<T>): Array<T> = withReadLock {
        val size = source.size

        @Suppress("UNCHECKED_CAST")
        val result = if (array.size >= size) {
            array
        } else {
            java.lang.reflect.Array.newInstance(array.javaClass.componentType, size) as Array<T>
        }
        for (index in 0..<size) {
            @Suppress("UNCHECKED_CAST")
            result[index] = source[index] as T
        }
        if (result.size > size) {
            @Suppress("UNCHECKED_CAST")
            result[size] = null as T
        }
        result
    }

    override fun add(element: E): Boolean = withWriteLock { source.add(element) }

    override fun remove(element: E): Boolean = withWriteLock { source.remove(element) }

    override fun addAll(elements: Collection<E>): Boolean = withWriteLock { source.addAll(elements) }

    override fun addAll(index: Int, elements: Collection<E>): Boolean =
        withWriteLock { source.addAll(index, elements) }

    override fun removeAll(elements: Collection<E>): Boolean =
        withWriteLock { source.removeAll(elements) }

    override fun retainAll(elements: Collection<E>): Boolean =
        withWriteLock { source.retainAll(elements) }

    override fun clear() {
        withWriteLock { source.clear() }
    }

    override fun set(index: Int, element: E): E = withWriteLock { source.set(index, element) }

    override fun add(index: Int, element: E) {
        withWriteLock { source.add(index, element) }
    }

    override fun removeAt(index: Int): E = withWriteLock { source.removeAt(index) }

    override fun removeIf(filter: Predicate<in E>): Boolean =
        withWriteLock { source.removeIf(filter) }

    override fun replaceAll(operator: UnaryOperator<E>) {
        withWriteLock { source.replaceAll(operator) }
    }

    override fun sort(comparator: Comparator<in E>?) {
        withWriteLock { Collections.sort(source, comparator) }
    }

    override fun toString(): String = withReadLock { source.toString() }
}
