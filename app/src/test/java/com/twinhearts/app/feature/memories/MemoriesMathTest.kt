package com.twinhearts.app.feature.memories

import com.twinhearts.app.feature.memories.domain.isUploadable
import com.twinhearts.app.feature.memories.domain.isVideoMime
import com.twinhearts.app.feature.memories.domain.memoryInsight
import com.twinhearts.app.feature.memories.domain.model.Memory
import com.twinhearts.app.feature.memories.domain.model.MemoryInsight
import com.twinhearts.app.feature.memories.domain.monthlyCounts
import com.twinhearts.app.feature.memories.domain.newMemoryId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MemoriesMathTest {

    private val today = LocalDate.of(2026, 10, 29)

    private fun memory(id: String, date: String?, video: Boolean = false) =
        Memory(id, "https://x/$id", "n", video, date?.let(LocalDate::parse), null, false)

    @Test fun `six months are listed oldest first ending this month`() {
        val counts = monthlyCounts(emptyList(), today)
        assertEquals(6, counts.size)
        assertEquals(YearMonth.of(2026, 5), counts.first().month)
        assertEquals(YearMonth.of(2026, 10), counts.last().month)
    }

    @Test fun `counts are per month and older or undated items are ignored`() {
        val items = listOf(
            memory("a", "2026-10-01"), memory("b", "2026-10-28"), memory("c", "2026-09-15"),
            memory("old", "2025-01-01"), memory("none", null),
        )
        val counts = monthlyCounts(items, today).associate { it.month to it.count }
        assertEquals(2, counts[YearMonth.of(2026, 10)])
        assertEquals(1, counts[YearMonth.of(2026, 9)])
        assertEquals(3, counts.values.sum())
    }

    @Test fun `insight asks for a first memory when the album is empty`() {
        assertEquals(MemoryInsight.AddFirst, memoryInsight(emptyList(), today))
    }

    @Test fun `insight counts this month with totals and videos`() {
        val items = listOf(memory("a", "2026-10-02", video = true), memory("b", "2026-10-03"), memory("c", "2026-08-03"))
        assertEquals(MemoryInsight.ThisMonth(2, 3, 1), memoryInsight(items, today))
    }

    @Test fun `insight falls back to the total when nothing was added this month`() {
        assertEquals(MemoryInsight.SoFar(1), memoryInsight(listOf(memory("a", "2026-08-03")), today))
    }

    @Test fun `only the types the server accepts can be uploaded`() {
        assertTrue(isUploadable("image/jpeg"))
        assertTrue(isUploadable("IMAGE/HEIC"))
        assertTrue(isUploadable("video/mp4"))
        assertTrue(isUploadable("video/quicktime"))
        assertEquals(false, isUploadable("image/svg+xml"))
        assertEquals(false, isUploadable("text/html"))
        assertEquals(false, isUploadable("application/octet-stream"))
    }

    @Test fun `video types are told apart from photos`() {
        assertTrue(isVideoMime("video/webm"))
        assertEquals(false, isVideoMime("image/png"))
    }

    @Test fun `new ids are the time then five characters`() {
        var n = 0
        val id = newMemoryId(1700000000000L) { n++ }
        assertEquals("1700000000000_abcde", id)
        assertEquals(19, id.length)
    }
}
