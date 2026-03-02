package mediathek.tool

/**
 * This class check if titel contains specific keywords.
 */
class TrailerTeaserChecker {
    /**
     * Check if a string might belong to a trailer, teaser, etc.
     */
    fun check(content: String): Boolean {
        if (content.length < MIN_KEYWORD_LENGTH) {
            return false
        }

        var index = 0
        val end = content.length
        while (index < end) {
            when (content[index].lowercaseChar()) {
                't' -> {
                    if (matchesKeywordAt(content, index, KEYWORD_TRAILER) ||
                        matchesKeywordAt(content, index, KEYWORD_TEASER)
                    ) {
                        return true
                    }
                }

                'v' -> {
                    if (matchesKeywordAt(content, index, KEYWORD_VORSCHAU)) {
                        return true
                    }
                }
            }
            index++
        }
        return false
    }

    private fun matchesKeywordAt(content: String, start: Int, keyword: String): Boolean {
        return start + keyword.length <= content.length &&
                content.regionMatches(start, keyword, 0, keyword.length, ignoreCase = true)
    }

    private companion object {
        private const val KEYWORD_TRAILER = "trailer"
        private const val KEYWORD_TEASER = "teaser"
        private const val KEYWORD_VORSCHAU = "vorschau"
        private const val MIN_KEYWORD_LENGTH = 6
    }
}
