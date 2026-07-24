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
import java.util.EventObject

abstract class ListEvent<E> protected constructor(sourceList: EventList<E>?) : EventObject(sourceList) {
    @JvmField
    protected var sourceList: EventList<E> = sourceList as EventList<E>

    @get:JvmName("getIndexProperty")
    @get:JvmSynthetic
    val index: Int
        get() = getIndex()

    @get:JvmName("getBlockStartIndexProperty")
    @get:JvmSynthetic
    val blockStartIndex: Int
        get() = getBlockStartIndex()

    @get:JvmName("getBlockEndIndexProperty")
    @get:JvmSynthetic
    val blockEndIndex: Int
        get() = getBlockEndIndex()

    @get:JvmName("getTypeProperty")
    @get:JvmSynthetic
    val type: Int
        get() = getType()

    @get:JvmName("getOldValueProperty")
    @get:JvmSynthetic
    @Suppress("UNCHECKED_CAST")
    val oldValue: E
        get() = getOldValue() as E

    @get:JvmName("getNewValueProperty")
    @get:JvmSynthetic
    @Suppress("UNCHECKED_CAST")
    val newValue: E
        get() = getNewValue() as E

    @get:JvmName("getBlocksRemainingProperty")
    @get:JvmSynthetic
    val blocksRemaining: Int
        get() = getBlocksRemaining()

    @get:JvmName("getReorderMapProperty")
    @get:JvmSynthetic
    val reorderMap: IntArray
        get() = getReorderMap()

    @get:JvmName("isReorderingProperty")
    @get:JvmSynthetic
    val isReordering: Boolean
        get() = isReordering()

    abstract fun copy(): ListEvent<E>

    abstract fun reset()

    abstract fun next(): Boolean

    abstract fun hasNext(): Boolean

    abstract fun nextBlock(): Boolean

    abstract fun isReordering(): Boolean

    abstract fun getReorderMap(): IntArray

    abstract fun getIndex(): Int

    abstract fun getBlockStartIndex(): Int

    abstract fun getBlockEndIndex(): Int

    abstract fun getType(): Int

    abstract fun getOldValue(): E?

    abstract fun getNewValue(): E?

    abstract fun getBlocksRemaining(): Int

    open fun getSourceList(): EventList<E> = sourceList

    abstract override fun toString(): String

    companion object {
        const val DELETE = 0
        const val UPDATE = 1
        const val INSERT = 2

        @JvmField
        val UNKNOWN_VALUE: Any = "UNKNOWN VALUE"

        @JvmStatic
        @Suppress("UNCHECKED_CAST")
        fun <E> unknownValue(): E = UNKNOWN_VALUE as E
    }
}

@get:JvmSynthetic
val <E> ListEvent<E>.sourceList: EventList<E>
    get() = getSourceList()
