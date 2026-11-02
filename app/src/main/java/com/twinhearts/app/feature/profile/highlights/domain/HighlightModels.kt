package com.twinhearts.app.feature.profile.highlights.domain

import androidx.annotation.StringRes
import com.twinhearts.app.R

/**
 * Why this file exists: the plain data the Highlights row, the add sheet and the viewer use.
 * They never see the server's field names (see HighlightMapper).
 */

/** The nine categories. [wire] is what the server stores, so it must match routes/highlights.js. */
enum class HighlightCategory(val wire: String, val emoji: String, @StringRes val label: Int) {
    Travel("travel", "✈️", R.string.profile_hl_cat_travel),
    Love("love", "💕", R.string.profile_hl_cat_love),
    Dates("dates", "🍷", R.string.profile_hl_cat_dates),
    Friends("friends", "👯", R.string.profile_hl_cat_friends),
    Family("family", "👨‍👩‍👧", R.string.profile_hl_cat_family),
    Food("food", "🍕", R.string.profile_hl_cat_food),
    Events("events", "🎉", R.string.profile_hl_cat_events),
    Fun("fun", "😂", R.string.profile_hl_cat_fun),
    Other("other", "✨", R.string.profile_hl_cat_other);

    companion object {
        /** An unknown word from the server counts as Other, so a new category never hides a highlight. */
        fun fromWire(value: String?): HighlightCategory = entries.firstOrNull { it.wire == value } ?: Other
    }
}

enum class ClipKind { Image, Video }

data class HighlightClip(
    val id: String,
    val kind: ClipKind,
    val url: String,
    val createdAtMs: Long?,
)

data class Highlight(
    val id: String,
    val title: String,
    val category: HighlightCategory,
    /** True when it belongs to me; only then can I edit or delete it. */
    val mine: Boolean,
    val clips: List<HighlightClip>,
) {
    /** The circle shows the first clip. */
    val cover: HighlightClip get() = clips.first()
}

/** Everything the section needs in one piece: the circles and the two names for the viewer. */
data class HighlightsSnapshot(
    val highlights: List<Highlight>,
    val myName: String,
    val partnerName: String,
)

/** A photo or video chosen on this phone, not yet sent. [uri] is the text form of its address. */
data class PickedClip(val uri: String, val name: String, val mime: String, val sizeBytes: Long) {
    val isVideo: Boolean get() = mime.startsWith("video/", ignoreCase = true)
}

/** Where the clips go: into a brand-new highlight, or into one of mine. */
sealed interface UploadTarget {
    data class NewHighlight(val title: String, val category: HighlightCategory) : UploadTarget
    data class Existing(val highlightId: String) : UploadTarget
}

enum class ClipProblem { NotMedia, TooBig }
