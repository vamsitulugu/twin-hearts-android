package com.twinhearts.app.feature.movie.data.mapper

import com.twinhearts.app.feature.movie.data.remote.ChatMessageDto
import com.twinhearts.app.feature.movie.data.remote.FavoriteDto
import com.twinhearts.app.feature.movie.data.remote.HistoryDto
import com.twinhearts.app.feature.movie.data.remote.LibraryDto
import com.twinhearts.app.feature.movie.data.remote.ReactionDto
import com.twinhearts.app.feature.movie.data.remote.RoomDto
import com.twinhearts.app.feature.movie.data.remote.SearchItemDto
import com.twinhearts.app.feature.movie.domain.model.ChatMessage
import com.twinhearts.app.feature.movie.domain.model.CloudLibrary
import com.twinhearts.app.feature.movie.domain.model.HistoryItem
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.MovieReaction
import com.twinhearts.app.feature.movie.domain.model.RoomStatus
import com.twinhearts.app.feature.movie.domain.model.SharedMovie
import com.twinhearts.app.feature.movie.domain.model.WatchRoom
import com.twinhearts.app.feature.movie.domain.model.WatchStatus
import java.time.OffsetDateTime

/** Why this file exists: one place that turns server JSON shapes into the app's own models (R-40). */

/** Server dates are ISO text; some have `Z`, some `+00:00`, some no zone at all. */
internal fun parseIsoMs(text: String?): Long? {
    if (text.isNullOrBlank()) return null
    return runCatching { OffsetDateTime.parse(text).toInstant().toEpochMilli() }
        .recoverCatching { OffsetDateTime.parse(text + "Z").toInstant().toEpochMilli() }
        .getOrNull()
}

/** [role] is "user1" or "user2"; it decides which columns are mine and which are my partner's. */
fun RoomDto.toDomain(role: String): WatchRoom {
    val mine = role == "user1"
    val sched = scheduledStartAt
    return WatchRoom(
        myTitle = if (mine) user1Title else user2Title,
        partnerTitle = if (mine) user2Title else user1Title,
        myDurationSec = if (mine) user1Duration else user2Duration,
        partnerDurationSec = if (mine) user2Duration else user1Duration,
        myReady = if (mine) user1Ready else user2Ready,
        partnerReady = if (mine) user2Ready else user1Ready,
        moviesMatch = moviesMatch,
        status = RoomStatus.from(status),
        startRequestedByMe = startRequestedBy == role,
        scheduledStartRaw = sched,
        scheduledStartMs = parseIsoMs(sched),
        playing = playing,
        positionSec = positionSec ?: 0.0,
        playbackRate = playbackRate ?: 1.0,
        actionSeq = actionSeq ?: 0L,
        updatedAtMs = parseIsoMs(updatedAt) ?: 0L,
        sharedMovieId = sharedMovieId?.content?.takeIf { it != "null" && it.isNotBlank() },
    )
}

fun FavoriteDto.toDomain() = MovieItem(
    id = id?.content.orEmpty(), tmdbId = tmdbId?.content?.takeIf { it != "null" }, title = title, year = year,
    genre = genre, posterUrl = posterUrl, backdropUrl = backdropUrl, overview = overview, rating = rating,
    status = WatchStatus.from(status), addedBy = addedBy,
)

fun SearchItemDto.toDomain() = MovieItem(
    id = "", tmdbId = tmdbId?.content?.takeIf { it != "null" }, title = title, year = year, genre = genre,
    posterUrl = posterUrl, backdropUrl = backdropUrl, overview = overview, rating = rating,
    status = WatchStatus.Watchlist,
)

fun ChatMessageDto.toDomain(role: String) = ChatMessage(
    id = id?.content ?: createdAt, fromMe = senderRole == role, text = text, positionSec = positionSec, createdAt = createdAt,
)

fun HistoryDto.toDomain() = HistoryItem(
    id = id?.content.orEmpty(), title = title, durationSec = durationSec,
    completedPct = (completedPct ?: 0.0).toInt().coerceIn(0, 100), watchedAtMs = parseIsoMs(watchedAt), chatCount = chatCount,
)

fun LibraryDto.toDomain() = CloudLibrary(
    // Unfinished uploads stay in the list so they can be deleted, like on the web.
    movies = movies.map { SharedMovie(it.id, it.title, it.sizeBytes, it.durationSec, ready = it.status == "ready") },
    usedBytes = usage.usedBytes, limitBytes = usage.limitBytes, maxFileBytes = usage.maxFileBytes,
)

/** Reactions from the partner only; this phone already showed its own the moment it was tapped. */
fun ReactionDto.toDomain(role: String) = MovieReaction(id = at, emoji = emoji, fromMe = this.role == role)
