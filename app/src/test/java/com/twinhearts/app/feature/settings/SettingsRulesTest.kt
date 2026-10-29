package com.twinhearts.app.feature.settings

import com.twinhearts.app.feature.settings.domain.PinProblem
import com.twinhearts.app.feature.settings.domain.SentAgo
import com.twinhearts.app.feature.settings.domain.daysTogether
import com.twinhearts.app.feature.settings.domain.formatBytes
import com.twinhearts.app.feature.settings.domain.matchesSearch
import com.twinhearts.app.feature.settings.domain.model.StorageSummary
import com.twinhearts.app.feature.settings.domain.pinProblem
import com.twinhearts.app.feature.settings.domain.sentAgo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

class SettingsRulesTest {
    @Test
    fun pin_rules_match_the_server() {
        assertEquals(PinProblem.NotFourDigits, pinProblem("123"))
        assertEquals(PinProblem.NotFourDigits, pinProblem("12a4"))
        assertEquals(PinProblem.NotFourDigits, pinProblem(""))
        assertEquals(PinProblem.RepeatedDigits, pinProblem("1111"))
        assertEquals(PinProblem.Sequence, pinProblem("1234"))
        assertEquals(PinProblem.Sequence, pinProblem("9876"))
        assertNull(pinProblem("1357"))
        assertNull(pinProblem("2468"))
    }

    @Test
    fun search_matches_title_or_keywords_ignoring_case() {
        assertTrue(matchesSearch("PIN", "Change vault PIN", "password code"))
        assertTrue(matchesSearch("passw", "Change vault PIN", "password code"))
        assertFalse(matchesSearch("camera", "Change vault PIN", "password code"))
    }

    @Test
    fun search_with_blank_query_matches_nothing() {
        assertFalse(matchesSearch("  ", "Profile", "names"))
    }

    @Test
    fun sentAgo_buckets() {
        val now = Instant.parse("2026-10-29T12:00:00Z")
        assertEquals(SentAgo.Unknown, sentAgo(null, now))
        assertEquals(SentAgo.JustNow, sentAgo(now.minusSeconds(20), now))
        assertEquals(SentAgo.JustNow, sentAgo(now.plusSeconds(60), now)) // clock skew never goes negative
        assertEquals(SentAgo.Minutes(5), sentAgo(now.minusSeconds(5 * 60), now))
        assertEquals(SentAgo.Hours(3), sentAgo(now.minusSeconds(3 * 3600 + 10), now))
        assertEquals(SentAgo.Days(2), sentAgo(now.minusSeconds(49 * 3600), now))
    }

    @Test
    fun daysTogether_is_never_negative() {
        val today = LocalDate.of(2026, 10, 29)
        assertEquals(17, daysTogether(LocalDate.of(2026, 10, 12), today))
        assertEquals(0, daysTogether(LocalDate.of(2027, 1, 1), today))
    }

    @Test
    fun formatBytes_uses_the_web_decimals() {
        assertEquals("512 B", formatBytes(512, Locale.US))
        assertEquals("1.5 KB", formatBytes(1536, Locale.US))
        assertEquals("2.00 MB", formatBytes(2L * 1024 * 1024, Locale.US))
        assertEquals("1.50 GB", formatBytes(1536L * 1024 * 1024, Locale.US))
    }

    @Test
    fun storageSummary_percent_and_available() {
        val summary = StorageSummary(emptyList(), totalBytes = 50, capacityBytes = 200)
        assertEquals(25, summary.percent)
        assertEquals(150, summary.availableBytes)
        assertEquals(100, StorageSummary(emptyList(), 900, 200).percent)
        assertEquals(0, StorageSummary(emptyList(), 900, 200).availableBytes)
        assertEquals(0, StorageSummary(emptyList(), 10, 0).percent)
    }
}
