package com.twinhearts.app.feature.music.domain

import com.twinhearts.app.feature.music.domain.model.LyricLine
import com.twinhearts.app.feature.music.domain.model.Lyrics

/**
 * Why this file exists: lyrics come as `.lrc` text ("[01:23.45] words") or as plain text.
 * This turns either into [Lyrics]. Pure Kotlin, so it is easy to test.
 */
object LrcParser {

    // [mm:ss], [mm:ss.xx] or [mm:ss.xxx]. One line can carry several stamps.
    private val stamp = Regex("""\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")

    // Header tags like [ar:Artist] or [ti:Title] are not lyrics.
    private val header = Regex("""^\[[a-zA-Z#]+:.*]\s*$""")

    fun parse(raw: String?, source: String? = null): Lyrics {
        val text = raw?.replace("\r", "")?.trim().orEmpty()
        if (text.isEmpty()) return Lyrics.None

        val timed = mutableListOf<LyricLine>()
        for (line in text.lines()) {
            if (header.matches(line.trim())) continue
            val stamps = stamp.findAll(line).toList()
            if (stamps.isEmpty()) continue
            val words = line.replace(stamp, "").trim()
            stamps.forEach { m -> timed += LyricLine(toMillis(m), words) }
        }
        if (timed.isNotEmpty()) {
            return Lyrics(timed.sortedBy { it.timeMs }, synced = true, source = source)
        }

        val plain = text.lines()
            .map { it.trim() }
            .filterNot { header.matches(it) }
            .map { LyricLine(null, it) }
        // Drop blank lines at the very start and end, keep blanks between verses.
        val trimmed = plain.dropWhile { it.text.isEmpty() }.dropLastWhile { it.text.isEmpty() }
        return Lyrics(trimmed, synced = false, source = source)
    }

    /** Index of the line that is being sung at [positionMs], or -1 before the first line. */
    fun activeIndex(lines: List<LyricLine>, positionMs: Long): Int {
        var active = -1
        for (i in lines.indices) {
            val t = lines[i].timeMs ?: return -1
            if (t <= positionMs) active = i else break
        }
        return active
    }

    private fun toMillis(m: MatchResult): Long {
        val minutes = m.groupValues[1].toLong()
        val seconds = m.groupValues[2].toLong()
        val fraction = m.groupValues[3]
        val ms = when (fraction.length) {
            0 -> 0L
            1 -> fraction.toLong() * 100
            2 -> fraction.toLong() * 10
            else -> fraction.take(3).toLong()
        }
        return (minutes * 60 + seconds) * 1000 + ms
    }
}
