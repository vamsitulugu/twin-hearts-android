package com.twinhearts.app.feature.movie.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonPrimitive

/** Why this file exists: the exact JSON shapes of routes-movie.js, movie-search.js, movie-favorites.js and movie-library.js (R-65). */

@Serializable data class TimeDto(val now: Long = 0)

@Serializable
data class RoomDto(
    @SerialName("user1_movie_title") val user1Title: String? = null,
    @SerialName("user2_movie_title") val user2Title: String? = null,
    @SerialName("user1_duration_sec") val user1Duration: Double? = null,
    @SerialName("user2_duration_sec") val user2Duration: Double? = null,
    @SerialName("user1_ready") val user1Ready: Boolean = false,
    @SerialName("user2_ready") val user2Ready: Boolean = false,
    @SerialName("movies_match") val moviesMatch: Boolean = false,
    val status: String? = null,
    @SerialName("start_requested_by") val startRequestedBy: String? = null,
    @SerialName("scheduled_start_at") val scheduledStartAt: String? = null,
    val playing: Boolean = false,
    @SerialName("position_sec") val positionSec: Double? = null,
    @SerialName("playback_rate") val playbackRate: Double? = null,
    @SerialName("action_seq") val actionSeq: Long? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("shared_movie_id") val sharedMovieId: JsonPrimitive? = null,
    @SerialName("updated_by") val updatedBy: String? = null,
)

@Serializable data class PickMovieBody(val role: String, val title: String, val durationSec: Int)
@Serializable data class ReadyBody(val role: String, val ready: Boolean)
@Serializable data class RoleBody(val role: String)
@Serializable data class AcceptBody(val role: String, val countdownMs: Int)
@Serializable
data class StateBody(val role: String, val playing: Boolean, val positionSec: Double, val actionSeq: Long, val playbackRate: Double = 1.0)
@Serializable
data class EndBody(
    val role: String, val movieTitle: String?, val durationSec: Int?, val completedPct: Int,
    val sessionKey: String?, val positionSec: Double,
)
@Serializable
data class ChatBody(val role: String, val sessionKey: String, val movieTitle: String?, val text: String, val positionSec: Double)
@Serializable data class OkDto(val ok: Boolean = true)

@Serializable
data class ChatMessageDto(
    val id: JsonPrimitive? = null,
    @SerialName("sender_role") val senderRole: String = "",
    val text: String = "",
    @SerialName("playback_position_sec") val positionSec: Double? = null,
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class HistoryDto(
    val id: JsonPrimitive? = null,
    @SerialName("movie_title") val title: String = "",
    @SerialName("duration_sec") val durationSec: Double? = null,
    @SerialName("completed_pct") val completedPct: Double? = null,
    @SerialName("watched_at") val watchedAt: String? = null,
    @SerialName("chat_count") val chatCount: Int = 0,
)

@Serializable data class FavoritesDto(val favorites: List<FavoriteDto> = emptyList())
@Serializable data class FavoriteOneDto(val favorite: FavoriteDto? = null)

@Serializable
data class FavoriteDto(
    val id: JsonPrimitive? = null,
    @SerialName("tmdb_id") val tmdbId: JsonPrimitive? = null,
    val title: String = "",
    val year: String? = null,
    val genre: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    val overview: String? = null,
    val rating: Double? = null,
    val status: String? = null,
    @SerialName("added_by") val addedBy: String? = null,
    val scene: String? = null,
)

@Serializable
data class AddFavoriteBody(
    val coupleId: String, val tmdbId: String?, val title: String, val year: String?, val genre: String?,
    val posterUrl: String?, val backdropUrl: String?, val overview: String?, val rating: Double?,
    val status: String, val addedBy: String,
    val scene: String? = null,
)
@Serializable
data class UpdateFavoriteBody(
    val status: String? = null, val rating: Double? = null,
    // Edit Movie: needs backend-changes/movie/movie-favorites.js. Empty text clears a field.
    val title: String? = null, val year: String? = null, val genre: String? = null, val posterUrl: String? = null,
    val overview: String? = null, val scene: String? = null, val addedBy: String? = null,
)

@Serializable data class PosterUploadDto(val url: String = "")

@Serializable data class SearchDto(val results: List<SearchItemDto> = emptyList())
@Serializable
data class SearchItemDto(
    @SerialName("tmdb_id") val tmdbId: JsonPrimitive? = null,
    val title: String = "",
    val year: String? = null,
    val genre: String? = null,
    @SerialName("poster_url") val posterUrl: String? = null,
    @SerialName("backdrop_url") val backdropUrl: String? = null,
    val overview: String? = null,
    val rating: Double? = null,
)
@Serializable data class DetailsDto(val genre: String? = null)

@Serializable data class LibraryDto(val movies: List<SharedMovieDto> = emptyList(), val usage: UsageDto = UsageDto())
@Serializable data class UsageDto(val usedBytes: Long = 0, val limitBytes: Long = 0, val maxFileBytes: Long = 0)
@Serializable
data class SharedMovieDto(
    val id: String = "", val title: String = "", val sizeBytes: Long = 0, val durationSec: Double = 0.0,
    val status: String = "",
)
@Serializable data class UrlDto(val url: String = "")

// ── Cloud upload (movie-library.js: uploads / parts / complete / abort) ──
@Serializable data class StartUploadBody(val title: String, val size: Long, val mime: String, val durationSec: Double)
@Serializable data class StartUploadDto(val movieId: String = "", val partSize: Long = 0, val totalParts: Int = 0)
@Serializable data class PartsBody(val partNumbers: List<Int>)
@Serializable data class PartsDto(val urls: Map<String, String> = emptyMap())
@Serializable data class PartEtagDto(@SerialName("PartNumber") val partNumber: Int, @SerialName("ETag") val etag: String)
@Serializable data class CompleteBody(val parts: List<PartEtagDto>)

// ── Reactions (needs backend-changes/movie/reactions) ──
@Serializable data class ReactionBody(val role: String, val emoji: String, val relay: Boolean = true)
@Serializable
data class ReactionDto(val at: Long = 0, val role: String = "", val emoji: String = "")
