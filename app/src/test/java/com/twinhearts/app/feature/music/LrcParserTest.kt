package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.domain.LrcParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LrcParserTest {

    @Test
    fun timedLines_areParsedAndSorted() {
        val lyrics = LrcParser.parse("[00:12.50] second\n[00:01.00] first\n[01:02.345] third")
        assertTrue(lyrics.synced)
        assertEquals(listOf("first", "second", "third"), lyrics.lines.map { it.text })
        assertEquals(listOf(1000L, 12_500L, 62_345L), lyrics.lines.map { it.timeMs })
    }

    @Test
    fun headerTags_areNotLyrics() {
        val lyrics = LrcParser.parse("[ar:Someone]\n[ti:Title]\n[00:05.00] hello")
        assertEquals(1, lyrics.lines.size)
        assertEquals("hello", lyrics.lines[0].text)
    }

    @Test
    fun oneLineWithTwoStamps_repeatsTheLine() {
        val lyrics = LrcParser.parse("[00:10.00][00:40.00] chorus")
        assertEquals(listOf(10_000L, 40_000L), lyrics.lines.map { it.timeMs })
    }

    @Test
    fun plainText_isNotSynced_andKeepsVerseBreaks() {
        val lyrics = LrcParser.parse("\nline one\nline two\n\nline three\n")
        assertFalse(lyrics.synced)
        assertEquals(listOf("line one", "line two", "", "line three"), lyrics.lines.map { it.text })
    }

    @Test
    fun blankOrNull_isEmpty() {
        assertTrue(LrcParser.parse(null).isEmpty)
        assertTrue(LrcParser.parse("   \n ").isEmpty)
    }

    @Test
    fun activeIndex_followsThePosition() {
        val lines = LrcParser.parse("[00:01.00] a\n[00:05.00] b\n[00:09.00] c").lines
        assertEquals(-1, LrcParser.activeIndex(lines, 500))
        assertEquals(0, LrcParser.activeIndex(lines, 1000))
        assertEquals(1, LrcParser.activeIndex(lines, 8999))
        assertEquals(2, LrcParser.activeIndex(lines, 60_000))
    }

    @Test
    fun activeIndex_ofPlainLyrics_isNone() {
        assertEquals(-1, LrcParser.activeIndex(LrcParser.parse("just words").lines, 5000))
    }
}
