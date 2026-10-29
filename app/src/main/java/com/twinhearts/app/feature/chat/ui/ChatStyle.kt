package com.twinhearts.app.feature.chat.ui

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Why this file exists: the Chat page is neutral (black / grey / white), with no pink shading.
 * The shared theme glass, glow and bubble gradient are pink-tinted, so Chat uses these instead.
 * They stay inside Chat; other screens keep the shared theme.
 */
internal object ChatStyle {
    /** Page background: the same near-black as the Home sheet, no burgundy glow. */
    val Base = Color(0xFF0A0A0C)

    /** Bars, pills and buttons: white glass instead of pink-tinted glass. */
    val Glass = Color.White.copy(alpha = 0.08f)

    /**
     * Header, pinned / selection bar and the typing bar: SOLID dark grey. They used to be see-through
     * white glass, which let a photo wallpaper show through them. (Same look as 8% white on [Base].)
     */
    val Bar = Color(0xFF1E1E20)

    /** The message box itself: a touch lighter grey than the bar around it, so it reads as a field. */
    val Field = Color(0xFF2C2C30)

    /** Call history: green for a call that was picked up, red for a missed one. */
    val CallOk = Color(0xFF22C55E)
    val CallMissed = Color(0xFFE5484D)

    /** Selected message row / chip highlight, neutral instead of pink. */
    val Selection = Color.White.copy(alpha = 0.14f)

    /** Reaction chip outline for my own reactions. */
    val MineOutline = Color.White.copy(alpha = 0.38f)

    /** My message bubble: burgundy, same family as the Home hero (#6B093A), slightly deeper at the bottom. */
    val SentBubble = Brush.linearGradient(listOf(Color(0xFF7A0B44), Color(0xFF5C0834)))

    /** Partner avatar in the chat header: neutral grey, so the burgundy stays for my messages. */
    val Avatar = Brush.linearGradient(listOf(Color(0xFF3C3C42), Color(0xFF2C2C31)))

    /** WhatsApp blue for "seen" ticks. */
    val TickSeen = Color(0xFF53BDEB)

    /** Delivered (partner online, not seen yet) and sent (partner offline): white ticks. */
    val TickDelivered = Color.White
    val TickSent = Color.White.copy(alpha = 0.78f)

    /** The little time under a message: plain, light and small, like WhatsApp. */
    val MetaText = Color.White.copy(alpha = 0.62f)
}
