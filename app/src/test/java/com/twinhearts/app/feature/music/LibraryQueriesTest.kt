package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.domain.LibraryQueries
import com.twinhearts.app.feature.music.domain.model.LibraryScope
import com.twinhearts.app.feature.music.domain.model.Playlist
import com.twinhearts.app.feature.music.domain.model.RecordingFilter
import com.twinhearts.app.feature.music.domain.model.RecordingMedia
import com.twinhearts.app.feature.music.domain.model.RecordingOwner
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class LibraryQueriesTest {
    private val preview = song("p", url = "https://cdn-preview.dzcdn.net/x.mp3")
    private val mine = song("m", by = "user1", liked = true, lastPlayed = Instant.parse("2026-10-05T00:00:00Z"), hasLyrics = true)
    private val theirs = song("t", by = "user2", lastPlayed = Instant.parse("2026-10-06T00:00:00Z"))
    private val all = listOf(preview, mine, theirs)

    @Test
    fun scopes_splitTheLibrary() {
        assertEquals(listOf("m", "t"), LibraryQueries.forScope(LibraryScope.All, all, "user1").map { it.id })
        assertEquals(listOf("m"), LibraryQueries.forScope(LibraryScope.Mine, all, "user1").map { it.id })
        assertEquals(listOf("t"), LibraryQueries.forScope(LibraryScope.Partner, all, "user1").map { it.id })
        assertEquals(listOf("m"), LibraryQueries.forScope(LibraryScope.Liked, all, "user1").map { it.id })
        assertEquals(listOf("p"), LibraryQueries.forScope(LibraryScope.Featured, all, "user1").map { it.id })
    }

    @Test
    fun recent_isNewestFirst_andSkipsNeverPlayed() {
        assertEquals(listOf("t", "m"), LibraryQueries.forScope(LibraryScope.Recent, all, "user1").map { it.id })
    }

    @Test
    fun partnerView_isMirrored() {
        assertEquals(listOf("t"), LibraryQueries.forScope(LibraryScope.Mine, all, "user2").map { it.id })
    }

    @Test
    fun playlist_keepsItsOwnOrder_andSkipsMissingSongs() {
        val playlist = Playlist("1", "Road", "", listOf("t", "gone", "m"))
        assertEquals(listOf("t", "m"), LibraryQueries.inPlaylist(playlist, all).map { it.id })
    }

    @Test
    fun singable_needsLyrics_andFullSong() {
        assertEquals(listOf("m"), LibraryQueries.singable(all).map { it.id })
    }

    @Test
    fun recordings_filterByKindAndOwner() {
        val list = listOf(
            recording("a", by = "user1", at = "2026-10-01T00:00:00Z"),
            recording("b", by = "user2", media = RecordingMedia.Video, at = "2026-10-02T00:00:00Z"),
            recording("c", by = "user2", favorite = true, at = "2026-10-03T00:00:00Z"),
        )
        fun ids(f: RecordingFilter, o: RecordingOwner) = LibraryQueries.filterRecordings(list, f, o, "user1").map { it.id }
        assertEquals(listOf("c", "a"), ids(RecordingFilter.Audio, RecordingOwner.Everyone))
        assertEquals(listOf("b"), ids(RecordingFilter.Video, RecordingOwner.Everyone))
        assertEquals(listOf("c"), ids(RecordingFilter.Favorites, RecordingOwner.Everyone))
        assertEquals(listOf("a"), ids(RecordingFilter.Audio, RecordingOwner.Mine))
        assertEquals(listOf("c"), ids(RecordingFilter.Audio, RecordingOwner.Partner))
    }
}
