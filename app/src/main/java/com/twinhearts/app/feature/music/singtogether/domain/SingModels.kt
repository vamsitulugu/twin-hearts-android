package com.twinhearts.app.feature.music.singtogether.domain

/*
 * Why this file exists: the plain Kotlin shapes of "Sing Together" (video-call karaoke with the
 * partner), plus the duet lyrics reader. No Android and no network types in here.
 */

/** Who sings a line. The web app marks lines with [vamsi] / [likitha] / [host] / [guest] / [both]. */
enum class Speaker { A, B, Both }

data class DuetLine(val timeMs: Long, val text: String, val speaker: Speaker)

/** Where the room is. */
enum class SingPhase {
    Idle,

    /** I sent an invite and wait for the partner. */
    Inviting,

    /** The partner invited me; the accept / decline card is showing. */
    Invited,

    /** In the room, video not connected yet. */
    Connecting,
    InRoom,
}

enum class LyricsLoad { Loading, Ready, Missing }

/** One short message for the screen (shown once, then cleared). */
enum class SingNotice {
    Declined,
    NoAnswer,
    PartnerLeft,
    OnlyHostControls,
    SongUnavailable,
    NoCameraOrMic,
    SwitchCameraFailed,
    CouldNotConnect,
}

data class FloatingReaction(val id: Long, val emoji: String, val xFraction: Float)

/** Reads lyrics in the `.lrc` format, with the optional duet speaker tag after the time stamp. */
object DuetLrcParser {
    private val stamp = Regex("""^\[(\d{1,3}):(\d{2})(?:[.:](\d{1,3}))?]""")
    private val speakerTag = Regex("""^\[(vamsi|partner ?a|host|likitha|partner ?b|guest|both)]""", RegexOption.IGNORE_CASE)
    private val hostWords = Regex("vamsi|partner ?a|host", RegexOption.IGNORE_CASE)
    private val guestWords = Regex("likitha|partner ?b|guest", RegexOption.IGNORE_CASE)

    fun parse(raw: String?): List<DuetLine> {
        val text = raw?.replace("\r", "")?.trim().orEmpty()
        if (text.isEmpty()) return emptyList()
        val out = mutableListOf<DuetLine>()
        for (line in text.lines()) {
            var rest = line.trim()
            val times = mutableListOf<Long>()
            while (true) {
                val m = stamp.find(rest) ?: break
                times += toMillis(m)
                rest = rest.substring(m.value.length).trim()
            }
            if (times.isEmpty()) continue
            var speaker = Speaker.Both
            speakerTag.find(rest)?.let { tag ->
                val who = tag.groupValues[1]
                speaker = when {
                    hostWords.containsMatchIn(who) -> Speaker.A
                    guestWords.containsMatchIn(who) -> Speaker.B
                    else -> Speaker.Both
                }
                rest = rest.substring(tag.value.length).trim()
            }
            times.forEach { out += DuetLine(it, rest, speaker) }
        }
        return out.sortedBy { it.timeMs }
    }

    /** Index of the line being sung at [positionMs], or -1 before the first line. */
    fun activeIndex(lines: List<DuetLine>, positionMs: Long): Int {
        var active = -1
        for (i in lines.indices) {
            if (lines[i].timeMs <= positionMs) active = i else break
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

/**
 * The guest follows the host's song position. The host's clock and the guest's clock are different,
 * so the web app learns the smallest message delay seen (clock offset + best network time) and only
 * adds the EXTRA delay of each message. This is that rule, kept pure so it can be tested.
 */
class SyncClock {
    private var minDelta: Long? = null

    fun reset() {
        minDelta = null
    }

    /** Seconds the host's position has moved on since it sent a message stamped [sentAtMs]. */
    fun travelSeconds(sentAtMs: Long, nowMs: Long): Double {
        val delta = nowMs - sentAtMs
        val best = minDelta?.let { minOf(delta, it + 2) } ?: delta
        minDelta = best
        return maxOf(0L, (delta - best) + TYPICAL_LATENCY_MS) / 1000.0
    }

    private companion object {
        const val TYPICAL_LATENCY_MS = 150L
    }
}
