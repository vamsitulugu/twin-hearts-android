package com.twinhearts.app.feature.search.domain

/*
 * Why this file exists: the privacy rule as pure code, so it can be unit tested.
 */

/**
 * The privacy rule of the web `canSeeItem()` (same as Bucket List and My Space):
 * "self" = only who added it, "partner" = only the other person, anything else = both.
 * [by] is the role ("user1"/"user2") that added the item, [role] is mine.
 */
internal fun canSeeItem(visibility: String?, by: String?, me: Boolean, role: String): Boolean =
    when (visibility.orEmpty().ifBlank { "both" }) {
        "self" -> by == role || me || by.isNullOrEmpty()
        "partner" -> by != role && !me
        else -> true
    }
