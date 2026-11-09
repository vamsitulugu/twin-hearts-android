package com.twinhearts.app.feature.livemap

import com.twinhearts.app.feature.livemap.data.mapper.parseIsoMs
import com.twinhearts.app.feature.livemap.domain.LastSeen
import com.twinhearts.app.feature.livemap.domain.LiveMapFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

/** Why this file exists: guards the "last seen" wording and the server-time parser. */
class LastSeenTest {
    private val zone = ZoneId.of("UTC")
    private val now = ZonedDateTime.of(2026, 11, 10, 12, 0, 0, 0, zone).toInstant().toEpochMilli()
    private fun at(h: Int, d: Int = 10) = ZonedDateTime.of(2026, 11, d, h, 0, 0, 0, zone).toInstant().toEpochMilli()
    private fun seen(then: Long) = LiveMapFormat.lastSeen(now, then, zone, Locale.US)

    @Test fun placeholderEpochIsUnknown() = assertEquals(LastSeen.Unknown, seen(0L))
    @Test fun underAMinuteIsJustNow() = assertEquals(LastSeen.JustNow, seen(now - 20_000))
    @Test fun minutesAgo() = assertEquals(LastSeen.MinutesAgo(12), seen(now - 12 * 60_000))
    @Test fun hoursAgoSameDay() = assertEquals(LastSeen.HoursAgo(3), seen(at(9)))
    @Test fun yesterdayShowsClockTime() = assertTrue(seen(at(18, 9)) is LastSeen.Yesterday)
    @Test fun olderShowsDateAndTime() {
        val r = seen(at(18, 1)) as LastSeen.OnDay
        assertEquals("1 Nov", r.day)
    }
    @Test fun futureTimeNeverNegative() = assertEquals(LastSeen.JustNow, seen(now + 5_000))

    @Test fun parsesCommonServerFormats() {
        val expected = at(8) + 30 * 60_000L
        assertEquals(expected, parseIsoMs("2026-11-10T08:30:00+00:00"))
        assertEquals(expected, parseIsoMs("2026-11-10T08:30:00Z"))
        assertEquals(expected, parseIsoMs("2026-11-10T08:30:00"))
        assertEquals(expected, parseIsoMs("2026-11-10 08:30:00+00"))
        assertNull(parseIsoMs("not a date"))
        assertNull(parseIsoMs(null))
    }
}
