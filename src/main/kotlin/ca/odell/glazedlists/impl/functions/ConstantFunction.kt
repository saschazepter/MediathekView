/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.functions

import ca.odell.glazedlists.FunctionList

/** A function that always returns the same value regardless of the input. */
open class ConstantFunction<E, V>(private val value: V) : FunctionList.Function<E, V> {
    @Suppress("UNUSED_PARAMETER")
    override fun evaluate(sourceValue: E): V = value
}
