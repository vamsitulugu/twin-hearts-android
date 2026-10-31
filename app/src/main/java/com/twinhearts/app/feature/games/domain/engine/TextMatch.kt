package com.twinhearts.app.feature.games.domain.engine

/** Small text helpers shared by Mind Reader (answers) and Draw & Guess (guesses). */
object TextMatch {

    /** Lowercase, keep letters, digits and single spaces. */
    fun loose(text: String): String =
        text.lowercase().trim().replace(Regex("[^a-z0-9 ]+"), "").replace(Regex("\\s+"), " ")

    /** Lowercase letters and digits only ("Ice cream!" becomes "icecream"). */
    fun tight(text: String): String = text.lowercase().trim().replace(Regex("[^a-z0-9]+"), "")

    /** Cheap plural handling: "stars" and "star" share a stem. */
    fun stem(text: String): String = text.replace(Regex("(ies)$"), "y").replace(Regex("(es|s)$"), "")

    /** Number of single-letter edits needed to turn [a] into [b]. */
    fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var previous = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            val current = IntArray(b.length + 1)
            current[0] = i
            for (j in 1..b.length) {
                current[j] = if (a[i - 1] == b[j - 1]) {
                    previous[j - 1]
                } else {
                    1 + minOf(previous[j - 1], previous[j], current[j - 1])
                }
            }
            previous = current
        }
        return previous[b.length]
    }
}
