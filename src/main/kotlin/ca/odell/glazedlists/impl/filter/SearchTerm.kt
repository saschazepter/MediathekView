package ca.odell.glazedlists.impl.filter

import ca.odell.glazedlists.matchers.SearchEngineTextMatcherEditor
import java.io.Serializable

/** Immutable metadata for one piece of text in a search-engine query. */
data class SearchTerm<E>(
    val text: String,
    val isNegated: Boolean,
    val isRequired: Boolean,
    val field: SearchEngineTextMatcherEditor.Field<E>?,
) : Serializable {
    constructor(text: String) : this(text, false, false, null)

    /** Reused while matching to avoid allocating an extraction list per element. */
    val fieldFilterStrings: MutableList<String> = ArrayList()

    fun newSearchTerm(text: String): SearchTerm<E> = copy(text = text)

    fun isConstrainment(term: SearchTerm<*>): Boolean {
        if (isNegated != term.isNegated || field != term.field || text == term.text) return false
        return if (isNegated) text in term.text else term.text in text
    }

    fun isRelaxation(term: SearchTerm<*>): Boolean = term.isConstrainment(this)
}
