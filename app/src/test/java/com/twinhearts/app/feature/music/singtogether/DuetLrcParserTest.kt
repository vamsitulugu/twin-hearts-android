package com.twinhearts.app.feature.music.singtogether

import com.twinhearts.app.feature.music.singtogether.domain.DuetLrcParser
import com.twinhearts.app.feature.music.singtogether.domain.Speaker
import com.twinhearts.app.feature.music.singtogether.domain.SyncClock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuetLrcParserTest {
    @Test
    fun readsTimeAndSpeaker() {
        val lines = DuetLrcParser.parse("[00:05.50][host] Hello\n[00:09.00] [guest] Hi\n[00:12.5]all together")
        assertEquals(3, lines.size)
        assertEquals(5500L, lines[0].timeMs)
        assertEquals(Speaker.A, lines[0].speaker)
        assertEquals("Hello", lines[0].text)
        assertEquals(Speaker.B, lines[1].speaker)
        assertEquals(Speaker.Both, lines[2].speaker)
        assertEquals(12500L, lines[2].timeMs)
    }

    @Test
    fun plainTextHasNoTimedLines() {
        assertTrue(DuetLrcParser.parse("just words\nno stamps").isEmpty())
        assertTrue(DuetLrcParser.parse(null).isEmpty())
    }

    @Test
    fun activeIndexFollowsPosition() {
        val lines = DuetLrcParser.parse("[00:01.00]a\n[00:03.00]b")
        assertEquals(-1, DuetLrcParser.activeIndex(lines, 500))
        assertEquals(0, DuetLrcParser.activeIndex(lines, 2000))
        assertEquals(1, DuetLrcParser.activeIndex(lines, 9000))
    }
}

class SyncClockTest {
    @Test
    fun firstMessageAddsOnlyTypicalLatency() {
        val c = SyncClock()
        assertEquals(0.15, c.travelSeconds(1_000, 6_000), 0.001)
    }

    @Test
    fun extraDelayIsAdded() {
        val c = SyncClock()
        c.travelSeconds(1_000, 6_000)
        assertEquals(1.15, c.travelSeconds(2_000, 8_000), 0.01)
    }
}
