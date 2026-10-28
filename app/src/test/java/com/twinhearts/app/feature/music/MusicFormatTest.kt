package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.domain.MusicFormat
import com.twinhearts.app.feature.music.domain.model.SongSort
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class MusicFormatTest {

    @Test
    fun clock_formatsMinutesAndSeconds() {
        assertEquals("0:00", MusicFormat.clock(0))
        assertEquals("2:05", MusicFormat.clock(125))
        assertEquals("61:01", MusicFormat.clock(3661))
        assertEquals("0:00", MusicFormat.clock(-4))
        assertEquals("1:30", MusicFormat.clockMs(90_999))
    }

    @Test
    fun search_ignoresCaseAndAccents() {
        val s = song("1", title = "Perfect", artist = "Beyoncé")
        assertTrue(MusicFormat.matches(s, "beyonce"))
        assertTrue(MusicFormat.matches(s, "  PERF "))
        assertFalse(MusicFormat.matches(s, "shape"))
        assertTrue(MusicFormat.matches(s, ""))
    }

    @Test
    fun sorting_worksForEveryOrder() {
        val a = song("a", title = "Banana", artist = "Zed", plays = 1, created = Instant.parse("2026-10-01T00:00:00Z"))
        val b = song("b", title = "apple", artist = "Amy", plays = 9, created = Instant.parse("2026-10-03T00:00:00Z"))
        val c = song("c", title = "Cherry", artist = "Mia", plays = 5, created = Instant.parse("2026-10-02T00:00:00Z"))
        val all = listOf(a, b, c)
        assertEquals(listOf("b", "c", "a"), MusicFormat.sorted(all, SongSort.Recent).map { it.id })
        assertEquals(listOf("b", "a", "c"), MusicFormat.sorted(all, SongSort.Title).map { it.id })
        assertEquals(listOf("b", "c", "a"), MusicFormat.sorted(all, SongSort.Artist).map { it.id })
        assertEquals(listOf("b", "c", "a"), MusicFormat.sorted(all, SongSort.MostPlayed).map { it.id })
    }
}
