package com.twinhearts.app.feature.home.domain.model

/** The three little signals on the Stay Connected card. */
enum class LoveKind { Hug, Touch, MissYou }

/** The newest signal between the two of you. [fromMe] is true when I sent it. */
data class LoveInteraction(
    val kind: LoveKind,
    val fromMe: Boolean,
    val timestampMillis: Long,
)

/**
 * What the Stay Connected card needs: the newest signal, and a hug from my partner that is
 * waiting for me to accept ([pendingIncomingHugId], null when none).
 */
data class LoveCardState(
    val last: LoveInteraction? = null,
    val pendingIncomingHugId: Long? = null,
)
