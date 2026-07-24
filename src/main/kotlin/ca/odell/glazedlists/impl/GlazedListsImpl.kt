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
import ca.odell.glazedlists.GlazedLists
import java.util.function.Function

/** Internal utilities used by Glazed Lists implementations. */
@Suppress("UNCHECKED_CAST")
object GlazedListsImpl {
    @JvmStatic
    fun <E> replaceAll(
        target: EventList<E>,
        source: Collection<@JvmSuppressWildcards E>,
        updates: Boolean,
        comparator: Comparator<E>?,
    ) {
        val actualComparator = comparator
            ?: GlazedLists.comparableComparator<Comparable<Any?>>() as Comparator<E>
        val newValueNeeded = Any()
        val sourceIterator = source.iterator()
        var targetIndex = -1
        var targetObject: Any? = newValueNeeded
        var sourceObject: Any? = newValueNeeded

        while (true) {
            if (targetObject === newValueNeeded) {
                if (targetIndex < target.size) targetIndex++
                if (targetIndex < target.size) targetObject = target[targetIndex]
            }
            if (sourceObject === newValueNeeded && sourceIterator.hasNext()) {
                sourceObject = sourceIterator.next()
            }

            if (targetObject === newValueNeeded && sourceObject === newValueNeeded) break

            val compareResult = when {
                targetObject === newValueNeeded -> 1
                sourceObject === newValueNeeded -> -1
                else -> actualComparator.compare(targetObject as E, sourceObject as E)
            }

            when {
                compareResult < 0 -> {
                    target.removeAt(targetIndex)
                    targetIndex--
                    targetObject = newValueNeeded
                }

                compareResult == 0 -> {
                    if (updates) target[targetIndex] = sourceObject as E
                    targetObject = newValueNeeded
                    sourceObject = newValueNeeded
                }

                else -> {
                    target.add(targetIndex, sourceObject as E)
                    targetIndex++
                    sourceObject = newValueNeeded
                }
            }
        }
    }

    @JvmStatic
    fun <T> equalsComparator(): Comparator<T> = EqualsComparator()

    @JvmStatic
    fun <E> identityFunction(): Function<E, E> = Function.identity()

    private class EqualsComparator<T> : Comparator<T> {
        override fun compare(alpha: T, beta: T): Int = if (alpha == beta) 0 else 1
    }
}
