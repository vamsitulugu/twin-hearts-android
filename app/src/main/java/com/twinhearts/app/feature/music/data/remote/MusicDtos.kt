package com.twinhearts.app.feature.music.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/*
 * Why this file exists: the exact JSON the old server (routes/music.js, routes/recordings.js,
 * routes/lyrics.js, routes/media.js) sends and receives. Numbers that may arrive as text are read
 * as JsonElement and cleaned in the mapper. Unknown fields are ignored.
 */

@Serializable
data class SongDto(
    val id: JsonElement? = null,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val year: JsonElement? = null,
    @SerialName("audio_url") val audioUrl: String? = null,
    @SerialName("cover_url") val coverUrl: String? = null,
    @SerialName("duration_sec") val durationSec: JsonElement? = null,
    val visibility: String? = null,
    @SerialName("uploaded_by") val uploadedBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val liked: Boolean = false,
    @SerialName("partner_liked") val partnerLiked: Boolean = false,
    @SerialName("my_play_count") val myPlayCount: JsonElement? = null,
    @SerialName("my_last_played") val myLastPlayed: String? = null,
    @SerialName("has_lyrics") val hasLyrics: Boolean = false,
    @SerialName("lyrics_source") val lyricsSource: String? = null,
    /** Only sent by `GET /api/music/song/:id` (the list leaves the lyrics out). */
    val lyrics: String? = null,
)

@Serializable
data class PlaylistDto(
    val id: JsonElement? = null,
    val name: String? = null,
    val description: String? = null,
    @SerialName("song_ids") val songIds: List<JsonElement> = emptyList(),
)

@Serializable
data class PlaylistsResponse(
    val available: Boolean = true,
    val playlists: List<PlaylistDto> = emptyList(),
)

@Serializable
data class OnlineSearchResponse(val results: List<OnlineTrackDto> = emptyList())

@Serializable
data class OnlineTrackDto(
    val id: String = "",
    val title: String = "",
    val artist: String? = null,
    val album: String? = null,
    val duration: Int = 0,
    val cover: String? = null,
    val preview: String? = null,
)

@Serializable
data class RecordingDto(
    val id: JsonElement? = null,
    @SerialName("user_id") val userId: String = "",
    val title: String? = null,
    @SerialName("media_type") val mediaType: String? = null,
    @SerialName("media_url") val mediaUrl: String? = null,
    @SerialName("duration_sec") val durationSec: JsonElement? = null,
    @SerialName("is_favorite") val isFavorite: Boolean = false,
    val kind: String? = null,
    @SerialName("song_id") val songId: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
)

/** `GET /api/lyrics/status/:songId` — `state` is "cached", "missing" or "unknown". */
@Serializable
data class LyricsStatusDto(
    val state: String = "unknown",
    val provider: String? = null,
    val lrc: String? = null,
    val syncType: String? = null,
)

/** routes/media.js answers `{url, path}` after an upload. */
@Serializable
data class UploadDto(val url: String, val path: String? = null)

/** Several routes answer `{ok: true}`; PATCH answers the whole row, which we ignore. */
@Serializable
data class OkDto(val ok: Boolean? = null)

// ── request bodies ──

@Serializable
data class AddSongBody(
    val coupleId: String,
    val uploadedBy: String,
    val title: String,
    val artist: String,
    val audioUrl: String,
    val coverUrl: String? = null,
    val durationSec: Int,
    val album: String? = null,
    val genre: String? = null,
    val year: String? = null,
    val visibility: String,
    /** "deezer" for songs added from Search Online (the server then allows only Deezer hosts). */
    val source: String? = null,
)

@Serializable
data class EditSongBody(
    val coupleId: String,
    val viewer: String,
    val title: String? = null,
    val artist: String? = null,
    val album: String? = null,
    val genre: String? = null,
    val visibility: String? = null,
    val lyrics: String? = null,
    val lyricsSource: String? = null,
)

@Serializable
data class LikeBody(val coupleId: String, val viewer: String, val liked: Boolean)

@Serializable
data class ViewerBody(val coupleId: String, val viewer: String)

@Serializable
data class CreatePlaylistBody(
    val coupleId: String,
    val viewer: String,
    val name: String,
    val songIds: List<String>,
)

@Serializable
data class RenamePlaylistBody(val coupleId: String, val name: String)

@Serializable
data class CoupleBody(val coupleId: String)

@Serializable
data class PlaylistSongsBody(val coupleId: String, val viewer: String, val songIds: List<String>)

@Serializable
data class CreateRecordingBody(
    val coupleId: String,
    val userId: String,
    val title: String,
    val mediaType: String,
    val mediaUrl: String,
    val storagePath: String,
    val durationSec: Int,
    val kind: String,
    val songId: String? = null,
)

@Serializable
data class RecordingPatchBody(
    val coupleId: String,
    val userId: String,
    val title: String? = null,
    val isFavorite: Boolean? = null,
)

@Serializable
data class RecordingDeleteBody(val coupleId: String, val userId: String)
