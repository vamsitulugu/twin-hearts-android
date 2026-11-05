package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.GameChatKind
import com.twinhearts.app.feature.games.domain.model.GameChatMessage

/**
 * Why this file exists: the small rules of the in-game chat in one pure place, so they can be
 * unit-tested. Same numbers as the web app (game-comms.js).
 */
object GameChatRules {
    const val MAX_TEXT = 500
    /** My key only ever holds MY last messages, so the two phones never overwrite each other. */
    const val SAVED_PER_PLAYER = 20
    const val KEPT_IN_MEMORY = 40
    const val SHOW_LAST_MS = 3L * 60 * 60 * 1000
    const val MAX_RECORD_SECONDS = 120
    const val MIN_RECORD_MS = 700L

    /** Both sides together, oldest first, showing only the last three hours. */
    fun visible(mine: List<GameChatMessage>, theirs: List<GameChatMessage>, now: Long): List<GameChatMessage> =
        (mine + theirs).filter { it.sentAt > now - SHOW_LAST_MS }.sortedBy { it.sentAt }

    /** What goes into my key: text, and voice messages that already have a web address. */
    fun toSave(mine: List<GameChatMessage>): List<GameChatMessage> =
        mine.filter { it.kind == GameChatKind.Text || it.url.isNotBlank() }.takeLast(SAVED_PER_PLAYER)

    /** My partner's messages I have not seen yet. */
    fun fresh(known: Collection<GameChatMessage>, now: List<GameChatMessage>): List<GameChatMessage> {
        val seen = known.mapTo(HashSet()) { it.id }
        return now.filter { it.id !in seen }
    }

    /** My saved messages from the server, added to what this phone already has (nothing twice). */
    fun restoreMine(have: List<GameChatMessage>, saved: List<GameChatMessage>): List<GameChatMessage> {
        val ids = have.mapTo(HashSet()) { it.id }
        return (have + saved.filter { it.id !in ids }).distinctBy { it.id }.sortedBy { it.sentAt }.takeLast(KEPT_IN_MEMORY)
    }

    fun cleanText(raw: String): String = raw.trim().take(MAX_TEXT)
}
