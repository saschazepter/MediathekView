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
package ca.odell.glazedlists.impl

import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.TransformedList
import ca.odell.glazedlists.event.ListEvent
import org.jspecify.annotations.NonNull
import java.lang.reflect.Array as ReflectArray
import java.util.Comparator
import java.util.HashSet
import java.util.function.Predicate
import java.util.function.UnaryOperator

/** An up-to-date, read-only view of an event list. */
class ReadOnlyList<E>(source: EventList<E>) : TransformedList<E, E>(source) {
    init {
        source.addListEventListener(this)
    }

    override fun isWritable(): Boolean = false

    override fun listChanged(listChanges: ListEvent<E>) {
        updates.forwardEvent(listChanges)
    }

    override fun contains(element: E): Boolean = source.contains(element)

    override fun toArray(): @NonNull Array<Any?> = Array(source.size) { source[it] }

    @Suppress("UNCHECKED_CAST")
    override fun <T> toArray(array: @NonNull Array<T>): @NonNull Array<T> {
        val sourceSize = source.size
        val result =
            if (array.size >= sourceSize) {
                array
            } else {
                ReflectArray.newInstance(array.javaClass.componentType, sourceSize) as Array<T>
            }

        for (index in 0..<sourceSize) {
            result[index] = source[index] as T
        }
        if (result.size > sourceSize) {
            result[sourceSize] = null as T
        }
        return result
    }

    override fun containsAll(elements: Collection<E>): Boolean = HashSet(source).containsAll(elements)

    override fun indexOf(element: E): Int = source.indexOf(element)

    override fun lastIndexOf(element: E): Int = source.lastIndexOf(element)

    override fun equals(other: Any?): Boolean = source == other

    override fun hashCode(): Int = source.hashCode()

    override fun add(element: E): Boolean = cannotModify()

    override fun add(index: Int, element: E) {
        cannotModify()
    }

    override fun addAll(elements: @NonNull Collection<E>): Boolean = cannotModify()

    override fun addAll(index: Int, elements: @NonNull Collection<E>): Boolean = cannotModify()

    override fun clear() {
        cannotModify()
    }

    override fun remove(element: E): Boolean = cannotModify()

    override fun removeAt(index: Int): E = cannotModify()

    override fun removeAll(elements: @NonNull Collection<E>): Boolean = cannotModify()

    override fun retainAll(elements: @NonNull Collection<E>): Boolean = cannotModify()

    override fun set(index: Int, element: E): E = cannotModify()

    override fun replaceAll(operator: @NonNull UnaryOperator<E>) {
        cannotModify()
    }

    override fun removeIf(filter: @NonNull Predicate<in E>): Boolean = cannotModify()

    override fun sort(comparator: Comparator<in E>?) {
        cannotModify()
    }

    private fun cannotModify(): Nothing = throw UnsupportedOperationException("ReadOnlyList cannot be modified")
}
