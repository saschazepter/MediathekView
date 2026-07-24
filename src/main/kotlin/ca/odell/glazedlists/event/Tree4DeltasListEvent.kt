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
package ca.odell.glazedlists.event

import ca.odell.glazedlists.EventList
import ca.odell.glazedlists.impl.event.BlockSequence
import ca.odell.glazedlists.impl.event.Tree4Deltas

private open class Tree4DeltasListEvent<E>(
    private val deltasAssembler: ListEventAssembler<E>,
    sourceList: EventList<E>,
) : ListEvent<E>(sourceList) {
    private var deltasIterator: Tree4Deltas.Iterator<E>? = null
    private var linearIterator: BlockSequence<E>.Iterator? = null

    override fun copy(): ListEvent<E> {
        val result = Tree4DeltasListEvent(deltasAssembler, sourceList)
        result.deltasIterator = deltasIterator?.copy()
        result.linearIterator = linearIterator?.copy()
        return result
    }

    override fun reset() {
        if (deltasAssembler.useListBlocksLinear) {
            linearIterator = deltasAssembler.listBlocksLinear.iterator()
            deltasIterator = null
        } else {
            deltasIterator = deltasAssembler.listDeltas.iterator()
            linearIterator = null
        }
    }

    override fun next(): Boolean =
        if (linearIterator != null) {
            linearIterator!!.next()
        } else {
            deltasIterator!!.next()
        }

    override fun hasNext(): Boolean =
        if (linearIterator != null) {
            linearIterator!!.hasNext()
        } else {
            deltasIterator!!.hasNext()
        }

    override fun nextBlock(): Boolean =
        if (linearIterator != null) {
            linearIterator!!.nextBlock()
        } else {
            deltasIterator!!.nextNode()
        }

    override fun isReordering(): Boolean = deltasAssembler.reorderMap != null

    override fun getReorderMap(): IntArray =
        deltasAssembler.reorderMap
            ?: throw IllegalStateException("Cannot get reorder map for a non-reordering change")

    override fun getIndex(): Int =
        if (linearIterator != null) {
            linearIterator!!.index
        } else {
            deltasIterator!!.index
        }

    override fun getBlockStartIndex(): Int =
        if (linearIterator != null) {
            linearIterator!!.blockStart
        } else {
            deltasIterator!!.index
        }

    override fun getBlockEndIndex(): Int =
        if (linearIterator != null) {
            linearIterator!!.blockEnd - 1
        } else {
            deltasIterator!!.endIndex - 1
        }

    override fun getType(): Int =
        if (linearIterator != null) {
            linearIterator!!.type
        } else {
            deltasIterator!!.type
        }

    override fun getOldValue(): E =
        if (linearIterator != null) {
            linearIterator!!.oldValue
        } else {
            deltasIterator!!.oldValue
        }

    override fun getNewValue(): E =
        if (linearIterator != null) {
            linearIterator!!.newValue
        } else {
            deltasIterator!!.newValue
        }

    override fun getBlocksRemaining(): Int {
        var result = 0
        if (linearIterator != null) {
            val iteratorCopy = linearIterator!!.copy()
            while (iteratorCopy.nextBlock()) {
                result++
            }
        } else {
            val iteratorCopy = deltasIterator!!.copy()
            while (iteratorCopy.nextNode()) {
                result++
            }
        }
        return result
    }

    override fun toString(): String =
        if (linearIterator != null) {
            "ListEvent: ${deltasAssembler.listBlocksLinear}"
        } else {
            "ListEvent: ${deltasAssembler.listDeltas}"
        }
}

@JvmSynthetic
internal fun <E> createTree4DeltasListEvent(
    deltasAssembler: ListEventAssembler<E>,
    sourceList: EventList<E>,
): ListEvent<E> = Tree4DeltasListEvent(deltasAssembler, sourceList)
