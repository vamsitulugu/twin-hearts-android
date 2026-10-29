package com.twinhearts.app.feature.bucket.domain

import com.twinhearts.app.feature.bucket.domain.model.BucketDream

/**
 * Why this file exists: the privacy rule of the web `canSeeItem()`, kept as pure code so it can
 * be unit tested. [by] is the role ("user1"/"user2") that added the item, [role] is ours.
 */
fun canSeeDream(visibility: String?, by: String?, me: Boolean, role: String): Boolean =
    when (visibility.orEmpty().ifBlank { "both" }) {
        "self" -> by == role || me || by.isNullOrEmpty()
        "partner" -> by != role && !me
        else -> true
    }

/** The "done/total" pill next to the card title (web `bucketTag`). */
fun progressLabel(dreams: List<BucketDream>): String = "${dreams.count { it.done }}/${dreams.size}"
