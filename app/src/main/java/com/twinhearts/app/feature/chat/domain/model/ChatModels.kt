package com.twinhearts.app.feature.chat.domain.model

import java.time.Instant

/**
 * Why this file exists: the plain Kotlin shapes the Chat screens use. They know nothing about
 * JSON or Retrofit (R-40), so the UI and the tests stay simple.
 */

/** What a message holds. Kinds Chat cannot play yet are shown as [Other] with a short label. */
enum class MessageKind { Text, Image, ImageGroup, Video, Voice, Audio, File, Gif, Sticker, Other }

/** The ticks under my own messages. */
enum class DeliveryState { Sending, Failed, Sent, Delivered, Read }

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
    val durationSec: Int = 0,
    val replyToId: Long? = null,
    val createdAt: Instant,
    val delivery: DeliveryState = DeliveryState.Sent,
    val edited: Boolean = false,
    val deleted: Boolean = false,
    /** Starred by ME. Stars are private to each person. */
    val starred: Boolean = false,
    val reactions: List<String> = emptyList(),
    /** A view-once photo that was not opened yet: the server hides its link. */
    val viewOncePending: Boolean = false,
) {
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
