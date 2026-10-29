package com.twinhearts.app.feature.bucket.domain.model

import androidx.annotation.StringRes
import com.twinhearts.app.R
import java.time.LocalDate

/**
 * Why this file exists: the plain Kotlin shapes the Bucket List screen uses.
 * [key] values are what the web app stores in the shared state (`cat`), so never rename them.
 */
enum class BucketCategory(val key: String, val emoji: String, @StringRes val labelRes: Int) {
    Travel("Travel", "✈️", R.string.bucket_cat_travel),
    Experience("Experience", "🎭", R.string.bucket_cat_experience),
    Achievement("Achievement", "🏆", R.string.bucket_cat_achievement),
    Home("Home", "🏠", R.string.bucket_cat_home),
    Romance("Romance", "💕", R.string.bucket_cat_romance),
    Adventure("Adventure", "🏔️", R.string.bucket_cat_adventure),
    Other("Other", "💫", R.string.bucket_cat_other);

    companion object {
        fun fromKey(key: String?): BucketCategory = entries.firstOrNull { it.key == key } ?: Other
    }
}

/** Who may see a dream (`visibility` on the web). The add form offers only Both and Self. */
enum class BucketVisibility(
    val key: String,
    @StringRes val badgeRes: Int,
    @StringRes val formRes: Int,
) {
    Both("both", R.string.bucket_vis_both, R.string.bucket_vis_both_form),
    Self("self", R.string.bucket_vis_self, R.string.bucket_vis_self_form),
    Partner("partner", R.string.bucket_vis_partner, R.string.bucket_vis_partner);

    companion object {
        fun fromKey(key: String?): BucketVisibility = entries.firstOrNull { it.key == key } ?: Both
    }
}

/** One dream the signed-in person is allowed to see. */
data class BucketDream(
    val id: String,
    val title: String,
    val done: Boolean,
    val date: LocalDate?,
    val category: BucketCategory,
    val visibility: BucketVisibility,
)

/** What the "Add a Dream" form hands to the repository. */
data class NewDream(
    val title: String,
    val date: LocalDate?,
    val category: BucketCategory,
    val visibility: BucketVisibility,
)
