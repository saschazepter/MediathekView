/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.functions

import java.util.function.Function

/** A function that always returns the same value regardless of the input. */
open class ConstantFunction<E, V>(private val value: V) : Function<E, V> {
    @Suppress("UNUSED_PARAMETER")
    override fun apply(sourceValue: E): V = value
}
