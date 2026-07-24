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

import ca.odell.glazedlists.event.ListEvent
import ca.odell.glazedlists.event.ListEventAssembler
import java.util.ArrayList
import java.util.RandomAccess
import java.util.function.Function
import java.util.function.Predicate

/** A writable, index-preserving view that maps each element of a source [EventList]. */
@Suppress("INAPPLICABLE_JVM_NAME", "NON_FINAL_MEMBER_IN_FINAL_CLASS", "UNCHECKED_CAST")
class FunctionList<S, E> : TransformedList<S, E>, RandomAccess {
    private val sourceElements = ArrayList<S>()
    private var needDispose = false
    private val mappedElements: MutableList<E>
    private lateinit var forward: AdvancedFunction<S, E>
    private var reverse: Function<E, S>? = null

    constructor(source: EventList<S>, forward: Function<S, E>?) : this(source, forward, null)

    constructor(
        source: EventList<S>,
        forward: Function<S, E>?,
        reverse: Function<E, S>?,
    ) : super(source) {
        updateForwardFunction(forward)
        this.reverse = reverse

        mappedElements = ArrayList(source.size)
        source.forEach { mappedElements.add(mapForward(it)) }
        source.addListEventListener(this)
    }

    /** The function used to map source values, or `null` only at the Kotlin boundary for legacy null validation. */
    open var forwardFunction: Function<S, E>?
        get() {
            val current = forward
            return if (current is AdvancedFunctionAdapter<*, *>) {
                (current as AdvancedFunctionAdapter<S, E>).delegate
            } else {
                current
            }
        }
        set(value) {
            updateForwardFunction(value)

            updates.beginEvent(true)
            val currentSource = source!!
            for (index in currentSource.indices) {
                val newValue = mapForward(currentSource[index])
                val oldValue = mappedElements.set(index, newValue)
                updates.elementUpdated(index, oldValue, newValue)
            }
            updates.commitEvent()
        }

    /** The optional function used to write mapped values back to the source. */
    open var reverseFunction: Function<E, S>?
        get() = reverse
        set(value) {
            reverse = value
        }

    private fun mapForward(sourceValue: S): E = forward.apply(sourceValue)

    private fun remapForward(transformedValue: E, sourceValue: S): E =
        forward.reevaluate(sourceValue, transformedValue)

    private fun mapReverse(value: E): S {
        val currentReverse = reverse
            ?: throw IllegalStateException(
                "A reverse mapping function must be specified to support this List operation",
            )
        return currentReverse.apply(value)
    }

    private fun updateForwardFunction(newForward: Function<S, E>?) {
        if (newForward == null) {
            throw IllegalArgumentException("forward Function may not be null")
        }

        if (newForward is AdvancedFunction<*, *>) {
            forward = newForward as AdvancedFunction<S, E>
            if (!needDispose) {
                needDispose = true
                val currentSource = source!!
                sourceElements.ensureCapacity(currentSource.size)
                sourceElements.clear()
                sourceElements.addAll(currentSource)
            }
        } else {
            forward = AdvancedFunctionAdapter(newForward)
            needDispose = false
            sourceElements.clear()
            sourceElements.trimToSize()
        }
    }

    open override fun isWritable(): Boolean = true

    open override fun listChanged(listChanges: ListEvent<S>) {
        updates.beginEvent(true)

        if (listChanges.isReordering) {
            val reorderMap = listChanges.reorderMap
            val originalMappedElements = ArrayList(mappedElements)
            for (index in reorderMap.indices) {
                mappedElements[index] = originalMappedElements[reorderMap[index]]
            }
            updates.reorder(reorderMap)
        } else {
            val currentSource = source!!
            while (listChanges.next()) {
                val changeIndex = listChanges.index
                when (listChanges.type) {
                    ListEvent.INSERT -> {
                        val newValue = currentSource[changeIndex]
                        val transformed = mapForward(newValue)
                        if (needDispose) sourceElements.add(changeIndex, newValue)
                        mappedElements.add(changeIndex, transformed)
                        updates.elementInserted(changeIndex, transformed)
                    }

                    ListEvent.UPDATE -> {
                        val oldTransformed = get(changeIndex)
                        val newValue = currentSource[changeIndex]
                        val newTransformed = remapForward(oldTransformed, newValue)
                        if (needDispose) sourceElements[changeIndex] = newValue
                        mappedElements[changeIndex] = newTransformed
                        updates.elementUpdated(changeIndex, oldTransformed, newTransformed)
                    }

                    ListEvent.DELETE -> {
                        val oldTransformed = mappedElements.removeAt(changeIndex)
                        if (needDispose) {
                            val oldValue = sourceElements.removeAt(changeIndex)
                            forward.dispose(oldValue, oldTransformed)
                        }
                        updates.elementDeleted(changeIndex, oldTransformed)
                    }
                }
            }
        }
        updates.commitEvent()
    }

    open override fun get(index: Int): E = mappedElements[index]

    @JvmName("remove")
    open override fun removeAt(index: Int): E {
        val removed = get(index)
        source!!.removeAt(index)
        return removed
    }

    open override fun set(index: Int, element: E): E {
        val updated = get(index)
        source!![index] = mapReverse(element)
        return updated
    }

    open override fun add(index: Int, element: E) {
        source!!.add(index, mapReverse(element))
    }

    open override fun removeIf(filter: Predicate<in E>): Boolean {
        val sourceUpdates = sourceUpdates()
        var foundMatch = false
        for (index in size - 1 downTo 0) {
            if (filter.test(mappedElements[index])) {
                if (sourceUpdates != null && !foundMatch) {
                    sourceUpdates.beginEvent(true)
                }
                foundMatch = true
                removeAt(index)
            }
        }
        if (sourceUpdates != null && foundMatch) {
            sourceUpdates.commitEvent()
        }
        return foundMatch
    }

    private fun sourceUpdates(): ListEventAssembler<*>? {
        val currentSource = source!!
        if (currentSource !is AbstractEventList<*>) return null
        return SourceUpdatesAccess.get(currentSource)
    }

    /** A mapping function with update and disposal lifecycle hooks. */
    interface AdvancedFunction<A, B> : Function<A, B> {
        fun reevaluate(sourceValue: A, transformedValue: B): B

        fun dispose(sourceValue: A, transformedValue: B)
    }

    private class AdvancedFunctionAdapter<A, B>(val delegate: Function<A, B>) : AdvancedFunction<A, B> {
        override fun apply(sourceValue: A): B = delegate.apply(sourceValue)

        override fun reevaluate(sourceValue: A, transformedValue: B): B = apply(sourceValue)

        override fun dispose(sourceValue: A, transformedValue: B) = Unit
    }

    private object SourceUpdatesAccess {
        private val updatesField = AbstractEventList::class.java.getDeclaredField("updates").apply {
            isAccessible = true
        }

        fun get(source: AbstractEventList<*>): ListEventAssembler<*> =
            updatesField.get(source) as ListEventAssembler<*>
    }
}
