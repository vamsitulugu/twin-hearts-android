package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.domain.model.Recording
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.Song
import com.twinhearts.app.feature.music.domain.model.SongVisibility
import java.time.Instant

/** Small builders so each test only states what it cares about. */
fun song(
    id: String,
    title: String = "Song $id",
    artist: String = "Artist",
    by: String = "user1",
    url: String = "https://cdn.example.com/$id.mp3",
    liked: Boolean = false,
    lastPlayed: Instant? = null,
    hasLyrics: Boolean = false,
    created: Instant? = Instant.parse("2026-10-01T10:00:00Z"),
    plays: Int = 0,
) = Song(
    id = id, title = title, artist = artist, album = "", genre = "", year = "", audioUrl = url, coverUrl = null,
    durationSec = 100, visibility = SongVisibility.Both, uploadedBy = by, createdAt = created, liked = liked,
    partnerLiked = false, myPlayCount = plays, myLastPlayed = lastPlayed, hasLyrics = hasLyrics, lyricsSource = null,
)

fun recording(
    id: String,
    by: String = "user1",
    media: RecordingMedia = RecordingMedia.Audio,
    favorite: Boolean = false,
    at: String = "2026-10-01T10:00:00Z",
) = Recording(
    id = id, userRole = by, title = "Rec $id", media = media, mediaUrl = "https://cdn.example.com/$id", durationSec = 10,
    favorite = favorite, kind = RecordingKind.Voice, songId = null, createdAt = Instant.parse(at),
)
