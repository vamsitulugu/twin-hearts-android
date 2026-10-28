package com.twinhearts.app.feature.music.domain.model

import java.time.Instant

/*
 * Why this file exists: the plain Kotlin shapes the Music screens use. The UI never sees the
 * server's JSON field names (R-40); mappers in data/mapper turn the server data into these.
 */

/** Who is using Music. [myRole] is "user1" or "user2". */
data class MusicIdentity(
    val coupleId: String,
    val myRole: String,
    val myName: String,
    val partnerName: String,
) {
    val partnerRole: String get() = if (myRole == "user1") "user2" else "user1"
}

/** Who may see a song. Matches the server's `visibility` column. */
enum class SongVisibility(val wire: String) {
    Both("both"),
    OnlyMe("my"),
    Partner("partner"),
    ;

    companion object {
        /** The server also knows the older words "self" and "private"; they mean "only me". */
        fun fromWire(value: String?): SongVisibility = when (value) {
            "my", "self", "private" -> OnlyMe
            "partner" -> Partner
            else -> Both
        }
    }
}

data class Song(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val year: String,
    val audioUrl: String,
    val coverUrl: String?,
    val durationSec: Int,
    val visibility: SongVisibility,
    val uploadedBy: String,
    val createdAt: Instant?,
    val liked: Boolean,
    val partnerLiked: Boolean,
    val myPlayCount: Int,
    val myLastPlayed: Instant?,
    val hasLyrics: Boolean,
    val lyricsSource: String?,
) {
    /** Songs added from "Search Online" are 30-second previews that live on Deezer's servers. */
    val isPreview: Boolean get() = audioUrl.contains("dzcdn.net") || audioUrl.contains("deezer.com")

    fun isMine(role: String): Boolean = uploadedBy == role
}

data class Playlist(
    val id: String,
    val name: String,
    val description: String,
    val songIds: List<String>,
)

/** The playlists call can answer "not set up yet" when the database update was never applied. */
data class PlaylistsResult(val available: Boolean, val playlists: List<Playlist>)

/** Everything the library screens draw, loaded in one go. */
data class MusicLibrary(
    val songs: List<Song>,
    val playlists: PlaylistsResult,
)

/** One row from "Search Online" (Deezer through our server). */
data class OnlineTrack(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationSec: Int,
    val coverUrl: String?,
    val previewUrl: String?,
)

enum class RecordingKind { Voice, Karaoke }

enum class RecordingMedia { Audio, Video }

data class Recording(
    val id: String,
    val userRole: String,
    val title: String,
    val media: RecordingMedia,
    val mediaUrl: String,
    val durationSec: Int,
    val favorite: Boolean,
    val kind: RecordingKind,
    val songId: String?,
    val createdAt: Instant?,
)

/** One line of lyrics. [timeMs] is null when the lyrics are plain text with no timing. */
data class LyricLine(val timeMs: Long?, val text: String)

/** Lyrics of one song: either timed lines or plain text lines. */
data class Lyrics(val lines: List<LyricLine>, val synced: Boolean, val source: String?) {
    val isEmpty: Boolean get() = lines.isEmpty()

    companion object {
        val None = Lyrics(emptyList(), synced = false, source = null)
    }
}

/** What the person typed in "Edit details". */
data class SongEdit(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val visibility: SongVisibility,
)

/** Tags read from a song file on the phone, before it is uploaded. */
class SongFileInfo(
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val year: String,
    val durationSec: Int,
    val cover: ByteArray?,
)

enum class SongSort { Recent, Title, Artist, MostPlayed }

/** The song lists on the Music home. */
enum class LibraryScope { All, Mine, Partner, Liked, Featured, Recent }

enum class RecordingFilter { Audio, Video, Favorites }

enum class RecordingOwner { Everyone, Mine, Partner }
