/* Glazed Lists                                                 (c) 2003-2006 */
/* http://publicobject.com/glazedlists/                      publicobject.com,*/
/*                                                     O'Dell Engineering Ltd.*/
package ca.odell.glazedlists.impl.filter

/** Case-insensitive strategy optimized for a single-character subtext. */
open class SingleCharacterCaseInsensitiveTextSearchStrategy : AbstractTextSearchStrategy() {
    private var subtextCharLower = '\u0000'
    private var subtextCharUpper = '\u0000'
    private var subtextInitialized = false

    override fun setSubtext(subtext: String?) {
        val validatedSubtext = requireNotNull(subtext) { "subtext may not be null" }
        require(validatedSubtext.length == 1) {
            "subtext ($validatedSubtext) must contain a single character"
        }

        val character = validatedSubtext[0]
        subtextCharLower = character.lowercaseChar()
        subtextCharUpper = character.uppercaseChar()
        subtextInitialized = true
    }

    override fun indexOf(text: String): Int {
        check(subtextInitialized) {
            "setSubtext must be called with a valid value before this method can operate"
        }

        for (index in text.indices) {
            val character = map(text[index])
            if (character == subtextCharLower || character == subtextCharUpper) return index
        }
        return -1
    }
}
