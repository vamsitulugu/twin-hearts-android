package com.twinhearts.app.feature.movie.domain.model

/** Why this file exists: the plain Kotlin shapes the Watch Together screens use (the UI never sees DTOs). */

/** Who is signed in, in the words the movie routes use ("user1" / "user2"). */
data class MovieIdentity(
    val userId: String,
    val coupleId: String,
    val role: String,
    val myName: String,
    val partnerName: String,
)

enum class RoomStatus(val api: String) {
    Selecting("selecting"), Waiting("waiting"), Ready("ready"), StartRequested("start_requested"),
    Countdown("countdown"), Watching("watching"), Ended("ended");

    companion object {
        fun from(value: String?): RoomStatus = entries.firstOrNull { it.api == value } ?: Selecting
    }
}

/** The shared watch room, already turned around so "my" and "partner" are correct for this phone. */
data class WatchRoom(
    val myTitle: String?,
    val partnerTitle: String?,
    val myDurationSec: Double?,
    val partnerDurationSec: Double?,
    val myReady: Boolean,
    val partnerReady: Boolean,
    val moviesMatch: Boolean,
    val status: RoomStatus,
    val startRequestedByMe: Boolean,
    /** Exactly as the server wrote it. It is also the chat "session key", so it must not be reformatted. */
    val scheduledStartRaw: String?,
    val scheduledStartMs: Long?,
    val playing: Boolean,
    val positionSec: Double,
    val playbackRate: Double,
    val actionSeq: Long,
    val updatedAtMs: Long,
    val sharedMovieId: String?,
) {
    /** Where the movie should be right now, given the server clock (same maths as the web app). */
    fun expectedPositionSec(serverNowMs: Long): Double {
        if (!playing) return positionSec
        val elapsed = (serverNowMs - updatedAtMs) / 1000.0
        return positionSec + elapsed.coerceAtLeast(0.0)
    }
}

enum class WatchStatus(val api: String) {
    Watchlist("watchlist"), Watching("watching"), Watched("watched");

    companion object {
        fun from(value: String?): WatchStatus = entries.firstOrNull { it.api == value } ?: Watchlist
    }
}

/** A movie in the couple's list. [id] is empty for an online search result that is not saved yet. */
data class MovieItem(
    val id: String,
    val tmdbId: String?,
    val title: String,
    val year: String?,
    val genre: String?,
    val posterUrl: String?,
    val backdropUrl: String?,
    val overview: String?,
    val rating: Double?,
    val status: WatchStatus,
    /** Who added it: "user1", "user2" or "both" (watched together). Null for older rows. */
    val addedBy: String? = null,
)

/** The three "belongs to" choices of the web form and the owner chips of the Library tab. */
enum class MovieOwner { Mine, Partner, Together }

data class ChatMessage(
    val id: String,
    val fromMe: Boolean,
    val text: String,
    val positionSec: Double?,
    val createdAt: String,
)

data class HistoryItem(
    val id: String,
    val title: String,
    val durationSec: Double?,
    val completedPct: Int,
    val watchedAtMs: Long?,
    val chatCount: Int,
)

/** [ready] is false for an upload that never finished (the web shows it as "Incomplete upload"). */
data class SharedMovie(val id: String, val title: String, val sizeBytes: Long, val durationSec: Double, val ready: Boolean = true)

data class CloudLibrary(val movies: List<SharedMovie>, val usedBytes: Long, val limitBytes: Long, val maxFileBytes: Long = 0)

/** A short emoji the partner (or this phone) sent while watching. [id] is unique so each one floats once. */
data class MovieReaction(val id: Long, val emoji: String, val fromMe: Boolean, val shownAtMs: Long = System.currentTimeMillis())
