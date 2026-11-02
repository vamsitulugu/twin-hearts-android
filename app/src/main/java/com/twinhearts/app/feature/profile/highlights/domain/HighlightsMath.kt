package com.twinhearts.app.feature.profile.highlights.domain

/** Pure rules of the Highlights feature (no Android), so they are easy to test. */
object HighlightsMath {
    /** The server refuses clips over 150 MB (routes/highlights.js). */
    const val MAX_BYTES: Long = 150L * 1024 * 1024
    const val MAX_PICK: Int = 10
    const val TITLE_MAX: Int = 24

    /** Categories that have at least one highlight, in the fixed menu order. Drives the filter chips. */
    fun usedCategories(all: List<Highlight>): List<HighlightCategory> =
        HighlightCategory.entries.filter { category -> all.any { it.category == category } }

    /** The circles to show: everything, or only one category. */
    fun visible(all: List<Highlight>, filter: HighlightCategory?): List<Highlight> =
        if (filter == null) all else all.filter { it.category == filter }

    /** Null when the file can be sent. */
    fun problemOf(mime: String, sizeBytes: Long): ClipProblem? = when {
        !mime.startsWith("image/", ignoreCase = true) && !mime.startsWith("video/", ignoreCase = true) -> ClipProblem.NotMedia
        sizeBytes > MAX_BYTES -> ClipProblem.TooBig
        else -> null
    }

    /** A blank name falls back to the category's name, like the web. */
    fun finalTitle(input: String, fallback: String): String = input.trim().take(TITLE_MAX).ifBlank { fallback.take(TITLE_MAX) }
}
