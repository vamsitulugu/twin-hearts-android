package com.twinhearts.app.feature.chat.domain.model

import java.time.Instant

/**
 * Why this file exists: the plain Kotlin shapes the Chat screens use. They know nothing about
 * JSON or Retrofit (R-40), so the UI and the tests stay simple.
 */

/** What a message holds. Kinds Chat cannot show are [Other] with a short label. */
enum class MessageKind {
    Text, Image, ImageGroup, Video, Voice, Audio, File, Gif, Sticker, Location, Gift, Contact, Poll, CallLog, Other
}

/** The ticks under my own messages. */
enum class DeliveryState { Sending, Failed, Sent, Delivered, Read }

/** One emoji pill under a bubble: how many people used it and whether I am one of them. */
data class ReactionGroup(val emoji: String, val count: Int, val mine: Boolean)

data class ChatMessage(
    /** Server id. [LOCAL_ID] while the message is still on its way. */
    val id: Long,
    /** Our own key for a message; it is how a local copy and the server copy are matched. */
    val clientId: String,
    val mine: Boolean,
    val kind: MessageKind,
    val text: String?,
    val mediaUrl: String?,
    /** Every picture of an [MessageKind.ImageGroup]; empty for other kinds. */
    val groupUrls: List<String> = emptyList(),
    val fileName: String? = null,
    val fileSize: Long = 0L,
    val durationSec: Int = 0,
    val replyToId: Long? = null,
    val createdAt: Instant,
    val delivery: DeliveryState = DeliveryState.Sent,
    val deliveredAt: Instant? = null,
    val readAt: Instant? = null,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    /** Starred by ME. Stars are private to each person. */
    val starred: Boolean = false,
    val pinned: Boolean = false,
    val forwarded: Boolean = false,
    val reactions: List<ReactionGroup> = emptyList(),
    /** A view-once photo or video. */
    val viewOnce: Boolean = false,
    /** A view-once item that was not opened yet: the server hides its link. */
    val viewOncePending: Boolean = false,
    /** A view-once item that was already opened (and burned). */
    val viewed: Boolean = false,
    /** Emoji of a sticker or gift. */
    val emoji: String? = null,
    /** Name of a sticker, gift or contact card. */
    val label: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val pollOptions: List<String> = emptyList(),
    val effect: String? = null,
    /** The server's type word and `media_meta` as JSON text; kept only so a message can be forwarded as it is. */
    val rawType: String? = null,
    val metaJson: String? = null,
) {
    /** True for a view-once item whose media link is hidden (tap-to-view tile or "Opened"). */
    val isViewOnceTile: Boolean
        get() = viewOnce && mediaUrl == null && !deleted &&
            (kind == MessageKind.Image || kind == MessageKind.Video || kind == MessageKind.Gif)

    companion object {
        const val LOCAL_ID = -1L
    }
}

enum class PresenceState { Online, Away, Offline }

/** My partner's last heartbeat, already turned into a state. */
data class PartnerPresence(val state: PresenceState, val lastSeen: Instant?)

data class ScheduledMessage(
    val id: String,
    val text: String,
    val sendAt: Instant,
)

/** The four chips of the search bar (web: All / Media / Links / Docs). */
enum class SearchFilter(val apiValue: String) {
    All("all"),
    Media("media"),
    Links("links"),
    Docs("docs"),
}

/** Whose chat this is, taken from the saved Session. */
data class ChatIdentity(
    val coupleId: String,
    val myRole: String,
    val myName: String,
    val partnerName: String,
) {
    val partnerRole: String get() = if (myRole == "user1") "user2" else "user1"
}

/** Small switches that live only on this phone (web keeps them in localStorage). */
data class ChatSettings(
    val muted: Boolean = false,
    /** A pet name only I see in the header. Blank = show the real name. */
    val nickname: String = "",
    /** Do-not-disturb window as "HH:mm", both null when off. */
    val dndStart: String? = null,
    val dndEnd: String? = null,
    /** Lock the chat after this many idle minutes (phone screen lock opens it again); 0 = off. */
    val autoLockMinutes: Int = 0,
)

enum class WallpaperMode(val apiValue: String) {
    Default("default"),
    Swatch("swatch"),
    Custom("custom"),
    ;

    companion object {
        fun fromApi(value: String?): WallpaperMode = entries.firstOrNull { it.apiValue == value } ?: Default
    }
}

/** The chat background. [dim] is 0..90 (percent of black laid over it). */
data class Wallpaper(
    val mode: WallpaperMode = WallpaperMode.Default,
    val swatch: String? = null,
    /** A web link (shared wallpaper) or a local file path (this phone only). */
    val image: String? = null,
    val dim: Int = 0,
)

/** The full-screen picture viewer. */
data class ViewerState(val urls: List<String>, val index: Int = 0)

/** What the person is playing with a view-once item right now (the link is never stored). */
data class ViewOnceResult(val url: String, val isVideo: Boolean)

/** The animations a message can be sent with (long-press the send button). */
enum class SendEffect(val apiValue: String, val symbols: List<String>) {
    Confetti("confetti", listOf("🎉", "✨", "🎊")),
    Hearts("hearts", listOf("❤️", "💕", "💖")),
    Balloons("balloons", listOf("🎈", "🎈", "🎈")),
    ;

    companion object {
        fun fromApi(value: String?): SendEffect? = entries.firstOrNull { it.apiValue == value }
    }
}

/** A GIF from the search list. */
data class GifItem(val id: String, val url: String)
