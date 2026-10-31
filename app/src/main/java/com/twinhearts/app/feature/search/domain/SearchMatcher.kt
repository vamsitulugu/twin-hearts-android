package com.twinhearts.app.feature.search.domain

import com.twinhearts.app.feature.search.domain.model.SearchDoc
import com.twinhearts.app.feature.search.domain.model.SearchResult
import java.text.Normalizer
import java.util.Locale

/**
 * Why this file exists: the one ranking rule for Search, as pure code so it can be unit tested.
 *
 * Every word the person typed must be found somewhere in a row (title, keywords or detail).
 * Words that start a title word rank highest, then words inside the title, then keywords,
 * then detail. A one-letter typo in a word of 4+ letters still matches, but ranks lowest.
 */
object SearchMatcher {

    private const val TITLE_PREFIX = 100
    private const val TITLE_INSIDE = 60
    private const val KEYWORD_PREFIX = 40
    private const val KEYWORD_INSIDE = 25
    private const val DETAIL_INSIDE = 15
    private const val TYPO = 10
    private const val EXACT_TITLE_BONUS = 80
    private const val TITLE_STARTS_BONUS = 40
    private const val MIN_TYPO_LENGTH = 4

    /** Lower-case, no accents, only letters/digits separated by single spaces. */
    fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        val sb = StringBuilder(decomposed.length)
        var lastSpace = true
        for (ch in decomposed) {
            when {
                Character.getType(ch) == Character.NON_SPACING_MARK.toInt() -> Unit
                ch.isLetterOrDigit() -> { sb.append(ch); lastSpace = false }
                !lastSpace -> { sb.append(' '); lastSpace = true }
            }
        }
        return sb.toString().trim()
    }

    fun tokens(query: String): List<String> = normalize(query).split(' ').filter { it.isNotEmpty() }.distinct()

    /**
     * Ranked results for [query] over [docs]; empty when the query has no words. A row that only
     * matched through a typo is left out when some other row matched exactly, so a correct word
     * is never drowned in near-misses ("fight" must not list "light").
     */
    fun rank(query: String, docs: List<SearchDoc>, limit: Int = Int.MAX_VALUE): List<SearchResult> {
        val words = tokens(query)
        if (words.isEmpty()) return emptyList()
        val whole = normalize(query)
        val scored = docs.mapNotNull { doc -> score(words, whole, doc)?.let { Triple(doc, it.points, it.usedTypo) } }
        val keep = if (scored.any { !it.third }) scored.filter { !it.third } else scored
        return keep.asSequence()
            .sortedWith(compareByDescending<Triple<SearchDoc, Int, Boolean>> { it.second }.thenBy { it.first.title.length })
            .take(limit)
            .map { (d, points, _) -> SearchResult(d.key, d.section, d.title, d.detail, d.where, d.emoji, d.destination, points) }
            .toList()
    }

    internal data class Scored(val points: Int, val usedTypo: Boolean)

    /** `null` when any typed word is missing from the row. */
    internal fun score(words: List<String>, whole: String, doc: SearchDoc): Scored? {
        val title = normalize(doc.title)
        val keywords = normalize(doc.keywords)
        val detail = normalize(doc.detail)
        var total = 0
        var typo = false
        for (word in words) {
            val points = wordScore(word, title, keywords, detail) ?: return null
            if (points == TYPO) typo = true
            total += points
        }
        if (title == whole) total += EXACT_TITLE_BONUS else if (title.startsWith(whole)) total += TITLE_STARTS_BONUS
        return Scored(total, typo)
    }

    private fun wordScore(word: String, title: String, keywords: String, detail: String): Int? {
        if (startsAWord(title, word)) return TITLE_PREFIX
        if (title.contains(word)) return TITLE_INSIDE
        if (startsAWord(keywords, word)) return KEYWORD_PREFIX
        if (keywords.contains(word)) return KEYWORD_INSIDE
        if (detail.contains(word)) return DETAIL_INSIDE
        if (word.length >= MIN_TYPO_LENGTH &&
            (nearAWord(title, word) || nearAWord(keywords, word))
        ) {
            return TYPO
        }
        return null
    }

    private fun startsAWord(text: String, word: String): Boolean =
        text.startsWith(word) || text.contains(" $word")

    /**
     * True when the start of some word of [text] is one edit away from [word]. The start is cut at
     * the typed length, one shorter and one longer, so a missing or an extra letter both match.
     */
    private fun nearAWord(text: String, word: String): Boolean =
        text.split(' ').any { w ->
            (word.length - 1..word.length + 1).any { cut ->
                w.length >= cut && editDistanceAtMostOne(w.take(cut), word)
            }
        }

    /** Fast check for "same word, or one letter added / removed / changed". */
    internal fun editDistanceAtMostOne(a: String, b: String): Boolean {
        if (a == b) return true
        val diff = a.length - b.length
        if (diff > 1 || diff < -1) return false
        var i = 0
        var j = 0
        var edits = 0
        while (i < a.length && j < b.length) {
            if (a[i] == b[j]) { i++; j++; continue }
            if (++edits > 1) return false
            when {
                a.length > b.length -> i++
                a.length < b.length -> j++
                else -> { i++; j++ }
            }
        }
        return edits + (a.length - i) + (b.length - j) <= 1
    }

    /** The part of [text] around the first match of any [words], trimmed for a one-line preview. */
    fun snippet(text: String, words: List<String>, radius: Int = 48): String {
        val clean = text.replace('\n', ' ').trim()
        if (clean.length <= radius * 2) return clean
        val lower = clean.lowercase(Locale.ROOT)
        val hit = words.map { lower.indexOf(it) }.filter { it >= 0 }.minOrNull() ?: 0
        val start = (hit - radius).coerceAtLeast(0)
        val end = (hit + radius).coerceAtMost(clean.length)
        return (if (start > 0) "…" else "") + clean.substring(start, end).trim() + (if (end < clean.length) "…" else "")
    }
}
