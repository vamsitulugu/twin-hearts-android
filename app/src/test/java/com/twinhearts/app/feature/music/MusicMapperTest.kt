package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.data.mapper.toDomain
import com.twinhearts.app.feature.music.data.mapper.toDomainOrNull
import com.twinhearts.app.feature.music.data.remote.PlaylistDto
import com.twinhearts.app.feature.music.data.remote.RecordingDto
import com.twinhearts.app.feature.music.data.remote.SongDto
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.SongVisibility
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicMapperTest {
    private val json = Json { ignoreUnknownKeys = true; explicitNulls = false }

    @Test
    fun song_withNumbersAsText_isCleaned() {
        val dto = json.decodeFromString<SongDto>(
            """{"id":42,"title":" Perfect ","artist":null,"audio_url":"https://x/y.mp3","duration_sec":"215","year":2017,
               "visibility":"self","uploaded_by":"user2","created_at":"2026-10-25T10:52:00.123+00:00","liked":true,
               "my_play_count":"3","has_lyrics":true,"extra":"ignored"}""",
        )
        val song = dto.toDomainOrNull()!!
        assertEquals("42", song.id)
        assertEquals("Perfect", song.title)
        assertEquals("Unknown Artist", song.artist)
        assertEquals(215, song.durationSec)
        assertEquals("2017", song.year)
        assertEquals(SongVisibility.OnlyMe, song.visibility)
        assertEquals(3, song.myPlayCount)
        assertTrue(song.liked)
        assertNotNull(song.createdAt)
    }

    @Test
    fun song_withoutId_isDropped() {
        assertNull(SongDto(title = "x").toDomainOrNull())
        assertEquals(1, listOf(SongDto(title = "x"), SongDto(id = JsonPrimitive("a"), title = "y")).toDomain().size)
        assertNull(SongDto(id = JsonNull).toDomainOrNull())
    }

    @Test
    fun songLinks_onDeezer_areFeaturedPreviews() {
        val song = SongDto(id = JsonPrimitive("1"), audioUrl = "https://cdnt-preview.dzcdn.net/a.mp3").toDomainOrNull()!!
        assertTrue(song.isPreview)
        assertFalse(SongDto(id = JsonPrimitive("2"), audioUrl = "https://x.supabase.co/a.mp3").toDomainOrNull()!!.isPreview)
    }

    @Test
    fun visibility_knowsTheOldWords() {
        assertEquals(SongVisibility.OnlyMe, SongVisibility.fromWire("private"))
        assertEquals(SongVisibility.Partner, SongVisibility.fromWire("partner"))
        assertEquals(SongVisibility.Both, SongVisibility.fromWire(null))
        assertEquals(SongVisibility.Both, SongVisibility.fromWire("something-new"))
    }

    @Test
    fun playlist_idsMayBeNumbers() {
        val dto = json.decodeFromString<PlaylistDto>("""{"id":"p1","name":"Road","song_ids":[1,"2"]}""")
        assertEquals(listOf("1", "2"), dto.toDomainOrNull()!!.songIds)
    }

    @Test
    fun recording_needsIdAndUrl() {
        val ok = RecordingDto(id = JsonPrimitive(7), userId = "user1", mediaType = "video", mediaUrl = "https://x/v.mp4", kind = "karaoke", isFavorite = true)
            .toDomainOrNull()!!
        assertEquals(RecordingMedia.Video, ok.media)
        assertEquals(RecordingKind.Karaoke, ok.kind)
        assertTrue(ok.favorite)
        assertNull(RecordingDto(id = JsonPrimitive(8), userId = "user1").toDomainOrNull())
    }
}
