package com.twinhearts.app.feature.pairing

import com.twinhearts.app.feature.pairing.domain.elapsedMinutes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.OffsetDateTime

class ElapsedTimeTest {
    private val now = OffsetDateTime.parse("2026-10-25T10:30:00+00:00").toInstant().toEpochMilli()

    @Test fun fiveMinutesAgo() = assertEquals(5L, elapsedMinutes("2026-10-25T10:25:00+00:00", now))
    @Test fun zuluTimeWorks() = assertEquals(90L, elapsedMinutes("2026-10-25T09:00:00Z", now))
    @Test fun futureDateIsZero() = assertEquals(0L, elapsedMinutes("2026-10-25T11:00:00+00:00", now))
    @Test fun badOrMissingDateIsNull() {
        assertNull(elapsedMinutes("not a date", now))
        assertNull(elapsedMinutes(null, now))
    }
}
