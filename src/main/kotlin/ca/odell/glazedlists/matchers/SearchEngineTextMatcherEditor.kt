/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.matchers

import ca.odell.glazedlists.TextFilterator
import ca.odell.glazedlists.impl.filter.TextMatcher
import ca.odell.glazedlists.impl.filter.TextMatchers
import java.io.Serializable

/**
 * Matches elements against search-engine-style input containing phrases, required or excluded
 * terms, and optionally named fields.
 */
open class SearchEngineTextMatcherEditor<E> @JvmOverloads constructor(
    textFilterator: TextFilterator<in E>? = null,
) : TextMatcherEditor<E>(textFilterator) {
    private val configuredFields = HashSet<Field<E>>()

    /** A defensive copy of the fields recognized while parsing search input. */
    open var fields: Set<@JvmSuppressWildcards Field<E>>
        get() = HashSet(configuredFields)
        set(value) {
            configuredFields.clear()
            configuredFields.addAll(value)
        }

    /** Parses [inputText] and applies the resulting search terms. */
    open fun refilter(inputText: String) {
        val filterTerms = TextMatchers.parse(inputText, fields)
        setTextMatcher(TextMatcher(filterTerms, filterator, mode, strategy))
    }

    /** Identifies a named search field and extracts the values searched within that field. */
    @JvmRecord
    data class Field<E>(
        val name: String?,
        val textFilterator: TextFilterator<in E>?,
    ) : Serializable {
        init {
            requireNotNull(name) { "name may not be null" }
            requireNotNull(textFilterator) { "textFilterator may not be null" }
        }

        /** JavaBean-compatible accessor retained alongside the record component accessor. */
        fun getName(): String = requireNotNull(name)

        /** JavaBean-compatible accessor retained alongside the record component accessor. */
        fun getTextFilterator(): TextFilterator<in E> = requireNotNull(textFilterator)

        override fun equals(other: Any?): Boolean =
            this === other || other is Field<*> && name == other.name

        override fun hashCode(): Int = requireNotNull(name).hashCode()
    }
}
