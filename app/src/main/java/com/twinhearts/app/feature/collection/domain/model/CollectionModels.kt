package com.twinhearts.app.feature.collection.domain.model

/** One voice message, from the chat (Voice tab) or the saved Library. */
data class VoiceItem(
    val id: String,
    val url: String?,
    /** "user1" or "user2". */
    val by: String,
    val durationSec: Int,
    /** ISO time as the server sent it; sorts correctly as text. */
    val createdAt: String,
    val starredBy: List<String>,
)

/** One photo or video in the Photo Collection. */
data class PhotoItem(
    val id: String,
    val url: String,
    val isVideo: Boolean,
    /** "YYYY-MM-DD" or empty. */
    val date: String,
    val name: String,
    val fromChat: Boolean,
)

enum class CollectionTab { Voice, Library, Photos }

enum class VoiceFilter { All, Mine, Partner, Favorites, Recent, Longest }

enum class VoiceSort { Newest, Oldest, Longest, Shortest, Favorites }

enum class PhotoFilter { All, Photos, Videos, Favorites }

/** Everything the screen needs, loaded in one go. */
data class CollectionData(
    val voices: List<VoiceItem>,
    val library: List<VoiceItem>,
    val photos: List<PhotoItem>,
    val role: String,
    val myName: String,
    val partnerName: String,
)
