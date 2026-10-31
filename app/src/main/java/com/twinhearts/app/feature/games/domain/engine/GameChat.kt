package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.Serializable

/** One short chat line or emoji sent during a co-op game (Bomb, Picture Puzzle). */
@Serializable
data class ChatLine(val who: Side, val text: String, val ts: Long)

object GameChat {
    const val KEEP = 20

    /** Adds [line] and keeps only the newest [KEEP] lines. Blank text is refused. */
    fun add(log: List<ChatLine>, who: Side, text: String, ts: Long): List<ChatLine>? {
        val clean = text.trim()
        if (clean.isEmpty()) return null
        return (log + ChatLine(who, clean, ts)).takeLast(KEEP)
    }

    /**
     * Puts back lines I sent that the other phone's newer save did not contain (two phones wrote at
     * the same moment and one write won). Lines are matched by their send time and writer.
     */
    fun restore(log: List<ChatLine>, mine: List<ChatLine>): List<ChatLine> {
        val missing = mine.filter { line -> log.none { it.ts == line.ts && it.who == line.who } }
        return if (missing.isEmpty()) log else (log + missing).sortedBy { it.ts }.takeLast(KEEP)
    }
}
