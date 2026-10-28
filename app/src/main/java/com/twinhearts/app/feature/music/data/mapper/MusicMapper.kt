package com.twinhearts.app.feature.music.data.mapper

import com.twinhearts.app.feature.music.data.remote.OnlineTrackDto
import com.twinhearts.app.feature.music.data.remote.PlaylistDto
import com.twinhearts.app.feature.music.data.remote.RecordingDto
import com.twinhearts.app.feature.music.data.remote.SongDto
import com.twinhearts.app.feature.music.domain.model.OnlineTrack
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.Recording
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongVisibility
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import java.time.Instant
import java.time.OffsetDateTime

/*
 * Why this file exists: turns server JSON shapes into the domain models (R-03). It is the one
 * place that knows the server may send numbers as text, or leave a field out.
 */

/** "12" or 12 both become "12". Null, missing and JSON null become null. */
internal fun JsonElement?.asText(): String? =
    (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content?.takeIf { it.isNotBlank() }

private fun JsonElement?.asInt(): Int =
    asText()?.toDoubleOrNull()?.toInt()?.coerceAtLeast(0) ?: 0

/** Postgres sends "2026-10-25T10:52:00.123+00:00"; Instant.parse alone cannot read every form. */
internal fun parseServerTime(text: String?): Instant? {
    if (text.isNullOrBlank()) return null
    return try {
        Instant.parse(text)
    } catch (ignored: Exception) {
        try {
            OffsetDateTime.parse(text).toInstant()
        } catch (ignoredToo: Exception) {
            null
        }
    }
}

/** Null when the server sent no id (it does that for "already being added" answers). */
fun SongDto.toDomainOrNull(): Song? {
    val songId = id.asText() ?: return null
    return Song(
        id = songId,
        title = title?.trim().orEmpty().ifEmpty { "Untitled" },
        artist = artist?.trim().orEmpty().ifEmpty { "Unknown Artist" },
        album = album.orEmpty(),
        genre = genre.orEmpty(),
        year = year.asText().orEmpty(),
        audioUrl = audioUrl.orEmpty(),
        coverUrl = coverUrl?.takeIf { it.isNotBlank() },
        durationSec = durationSec.asInt(),
        visibility = SongVisibility.fromWire(visibility),
        uploadedBy = uploadedBy.orEmpty(),
        createdAt = parseServerTime(createdAt),
        liked = liked,
        partnerLiked = partnerLiked,
        myPlayCount = myPlayCount.asInt(),
        myLastPlayed = parseServerTime(myLastPlayed),
        hasLyrics = hasLyrics,
        lyricsSource = lyricsSource?.takeIf { it.isNotBlank() },
    )
}

fun List<SongDto>.toDomain(): List<Song> = mapNotNull { it.toDomainOrNull() }

fun PlaylistDto.toDomainOrNull(): Playlist? {
    val playlistId = id.asText() ?: return null
    return Playlist(
        id = playlistId,
        name = name?.trim().orEmpty().ifEmpty { "Playlist" },
        description = description.orEmpty(),
        songIds = songIds.mapNotNull { it.asText() },
    )
}

fun OnlineTrackDto.toDomain(): OnlineTrack = OnlineTrack(
    id = id,
    title = title,
    artist = artist?.takeIf { it.isNotBlank() } ?: "Unknown Artist",
    album = album.orEmpty(),
    durationSec = duration,
    coverUrl = cover?.takeIf { it.isNotBlank() },
    previewUrl = preview?.takeIf { it.isNotBlank() },
)

fun RecordingDto.toDomainOrNull(): Recording? {
    val recordingId = id.asText() ?: return null
    val url = mediaUrl?.takeIf { it.isNotBlank() } ?: return null
    return Recording(
        id = recordingId,
        userRole = userId,
        title = title?.trim().orEmpty().ifEmpty { "Untitled" },
        media = if (mediaType == "video") RecordingMedia.Video else RecordingMedia.Audio,
        mediaUrl = url,
        durationSec = durationSec.asInt(),
        favorite = isFavorite,
        kind = if (kind == "karaoke") RecordingKind.Karaoke else RecordingKind.Voice,
        songId = songId?.takeIf { it.isNotBlank() },
        createdAt = parseServerTime(createdAt),
    )
}
