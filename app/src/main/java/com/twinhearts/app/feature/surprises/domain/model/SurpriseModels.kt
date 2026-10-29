package com.twinhearts.app.feature.surprises.domain.model

import androidx.annotation.StringRes
import com.twinhearts.app.R
import java.time.LocalDate

/**
 * Why this file exists: the plain Kotlin shapes the Surprises screen works with. They hold no
 * JSON and no Android classes, so the UI and the tests never see how the web app stores them.
 */

/** The "Type" choices of the web's New Surprise form. The web saves the emoji as the item's `type`. */
enum class SurpriseKind(val emoji: String, @StringRes val labelRes: Int) {
    Gift("🎁", R.string.surprises_kind_gift),
    Letter("💌", R.string.surprises_kind_letter),
    Photo("📷", R.string.surprises_kind_photo),
    Video("🎬", R.string.surprises_kind_video),
    Voice("🎙️", R.string.surprises_kind_voice),
}

/** What kind of file is attached to a surprise. */
enum class MediaKind(val key: String) {
    Image("image"),
    Video("video"),
    Audio("audio"),
}

/** One hidden surprise (web: an item of `surprises`). */
data class Surprise(
    val id: String,
    /** The type emoji exactly as saved, so a value this app does not know is still shown. */
    val emoji: String,
    val title: String,
    val message: String,
    /** Link (or `data:` link, when the web app saved it) of the attached file. Empty when none. */
    val mediaUrl: String,
    /** Null when nothing is attached. */
    val mediaKind: MediaKind?,
    val unlock: LocalDate,
    /** True when I hid it. The web only hides the content from the partner, never from the author. */
    val mine: Boolean,
)

/** One sealed message (web: an item of `capsules`). Both partners see the list. */
data class Capsule(
    val id: String,
    val message: String,
    val unlock: LocalDate,
    val opened: Boolean,
)

/** Everything the page shows, newest first. */
data class SurprisesData(
    val surprises: List<Surprise>,
    val capsules: List<Capsule>,
)

/** A file the person picked in the form. Checked by `checkPickedMedia` before it is accepted. */
data class PickedMedia(
    val uri: String,
    val name: String,
    val mime: String,
    val sizeBytes: Long,
)

data class NewSurprise(
    val kind: SurpriseKind,
    val title: String,
    val message: String,
    val media: PickedMedia?,
    val unlock: LocalDate,
)

data class NewCapsule(
    val message: String,
    val unlock: LocalDate,
)
