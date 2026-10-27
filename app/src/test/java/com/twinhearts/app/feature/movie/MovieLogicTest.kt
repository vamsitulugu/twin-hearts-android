package com.twinhearts.app.feature.movie

import com.twinhearts.app.feature.movie.data.mapper.parseIsoMs
import com.twinhearts.app.feature.movie.data.mapper.toDomain
import com.twinhearts.app.feature.movie.data.remote.RoomDto
import com.twinhearts.app.feature.movie.domain.LibraryFilter
import com.twinhearts.app.feature.movie.domain.OwnerFilter
import com.twinhearts.app.feature.movie.domain.applyFilter
import com.twinhearts.app.feature.movie.ui.components.nextSleepStep
import com.twinhearts.app.feature.movie.domain.formatClock
import com.twinhearts.app.feature.movie.domain.formatRuntime
import com.twinhearts.app.feature.movie.domain.model.MovieItem
import com.twinhearts.app.feature.movie.domain.model.RoomStatus
import com.twinhearts.app.feature.movie.domain.model.WatchStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Why this file exists: protects the room mapping, the sync maths and the library filters. */
class MovieLogicTest {

    private val dto = RoomDto(
        user1Title = "La La Land", user2Title = "La La Land", user1Duration = 7680.0, user2Duration = 7679.0,
        user1Ready = true, user2Ready = false, moviesMatch = true, status = "start_requested",
        startRequestedBy = "user2", scheduledStartAt = "2026-10-27T10:00:00.000Z", playing = true,
        positionSec = 10.0, updatedAt = "2026-10-27T10:00:00+00:00", actionSeq = 5,
    )

    @Test fun roomIsTurnedAroundForUser2() {
        val room = dto.toDomain("user2")
        assertFalse(room.myReady)
        assertTrue(room.partnerReady)
        assertTrue(room.startRequestedByMe)
        assertEquals(7679.0, room.myDurationSec!!, 0.0)
        assertEquals(RoomStatus.StartRequested, room.status)
    }

    @Test fun expectedPositionAddsElapsedTimeOnlyWhilePlaying() {
        val room = dto.toDomain("user1")
        assertEquals(15.0, room.expectedPositionSec(room.updatedAtMs + 5000), 0.001)
        assertEquals(10.0, room.copy(playing = false).expectedPositionSec(room.updatedAtMs + 5000), 0.001)
        assertEquals(10.0, room.expectedPositionSec(room.updatedAtMs - 5000), 0.001)
    }

    @Test fun isoDatesWithAndWithoutZoneParse() {
        assertNotNull(parseIsoMs("2026-10-27T10:00:00Z"))
        assertNotNull(parseIsoMs("2026-10-27T10:00:00.123456+00:00"))
        assertNotNull(parseIsoMs("2026-10-27T10:00:00"))
        assertEquals(null, parseIsoMs(null))
    }

    @Test fun timeTextIsReadable() {
        assertEquals("1:02:05", formatClock(3725.0))
        assertEquals("2:05", formatClock(125.0))
        assertEquals("1h 52m", formatRuntime(6720.0))
    }

    @Test fun libraryFiltersByStatusGenreAndTitle() {
        fun movie(id: String, title: String, genre: String?, status: WatchStatus) =
            MovieItem(id, null, title, null, genre, null, null, null, null, status)
        val list = listOf(
            movie("1", "Titanic", "Drama, Romance", WatchStatus.Watched),
            movie("2", "Dune", "Science Fiction", WatchStatus.Watchlist),
            movie("3", "Odd One", "Western", WatchStatus.Watchlist),
        )
        assertEquals(listOf("2", "3"), list.applyFilter(LibraryFilter.Watchlist, null, "").map { it.id })
        assertEquals(listOf("1"), list.applyFilter(LibraryFilter.All, "Romance", "").map { it.id })
        assertEquals(listOf("2"), list.applyFilter(LibraryFilter.All, "Sci-Fi", "").map { it.id })
        assertEquals(listOf("3"), list.applyFilter(LibraryFilter.All, "Other", "").map { it.id })
        assertEquals(listOf("2"), list.applyFilter(LibraryFilter.All, null, "dun").map { it.id })
    }

    @Test
    fun ownerAndFavoriteFilters() {
        fun m(id: String, by: String?) = MovieItem(id, null, id, null, null, null, null, null, null, WatchStatus.Watchlist, by)
        val list = listOf(m("1", "user1"), m("2", "user2"), m("3", "both"), m("4", null))
        assertEquals(listOf("1", "4"), list.applyFilter(LibraryFilter.All, null, "", OwnerFilter.Mine, "user1").map { it.id })
        assertEquals(listOf("2"), list.applyFilter(LibraryFilter.All, null, "", OwnerFilter.Partner, "user1").map { it.id })
        assertEquals(listOf("3"), list.applyFilter(LibraryFilter.All, null, "", OwnerFilter.Together, "user1").map { it.id })
        assertEquals(listOf("2", "3"), list.applyFilter(LibraryFilter.Favorites, null, "", favorites = setOf("2", "3")).map { it.id })
    }

    @Test
    fun sleepTimerCycles() {
        assertEquals(15, nextSleepStep(0))
        assertEquals(0, nextSleepStep(60))
    }
}
