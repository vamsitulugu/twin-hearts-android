package com.twinhearts.app.feature.games

import com.twinhearts.app.feature.games.domain.engine.GameChatRules
import com.twinhearts.app.feature.games.domain.model.GameChatKind
import com.twinhearts.app.feature.games.domain.model.GameChatMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The small rules of the in-game chat: what is shown, what is saved, what counts as new. */
class GameChatRulesTest {

    private fun text(id: String, at: Long, mine: Boolean = true) =
        GameChatMessage(id = id, kind = GameChatKind.Text, text = id, sentAt = at, mine = mine)

    private fun voice(id: String, at: Long, url: String = "", mine: Boolean = true) =
        GameChatMessage(id = id, kind = GameChatKind.Voice, url = url, seconds = 3, sentAt = at, mine = mine)

    @Test
    fun visibleMixesBothSidesOldestFirstAndDropsOldOnes() {
        val now = 10 * HOUR
        val shown = GameChatRules.visible(
            mine = listOf(text("m1", now - 1 * HOUR), text("old", now - 4 * HOUR)),
            theirs = listOf(text("t1", now - 2 * HOUR, mine = false)),
            now = now,
        )
        assertEquals(listOf("t1", "m1"), shown.map { it.id })
    }

    @Test
    fun saveKeepsTextAndUploadedVoiceButNotVoiceStillUploading() {
        val saved = GameChatRules.toSave(listOf(text("a", 1), voice("v-wait", 2), voice("v-done", 3, url = "https://x/y.m4a")))
        assertEquals(listOf("a", "v-done"), saved.map { it.id })
    }

    @Test
    fun saveKeepsOnlyTheLastTwenty() {
        val many = (1..30).map { text("m$it", it.toLong()) }
        val saved = GameChatRules.toSave(many)
        assertEquals(GameChatRules.SAVED_PER_PLAYER, saved.size)
        assertEquals("m30", saved.last().id)
        assertEquals("m11", saved.first().id)
    }

    @Test
    fun freshIsOnlyWhatWasNotSeenBefore() {
        val known = listOf(text("a", 1, mine = false))
        val now = listOf(text("a", 1, mine = false), text("b", 2, mine = false))
        assertEquals(listOf("b"), GameChatRules.fresh(known, now).map { it.id })
        assertTrue(GameChatRules.fresh(now, now).isEmpty())
    }

    @Test
    fun restoreAddsSavedMessagesOnceAndKeepsTheLocalOnes() {
        val have = listOf(voice("local", 5)) // still uploading, not on the server
        val saved = listOf(text("s1", 1), text("s2", 2), text("s2", 2))
        val merged = GameChatRules.restoreMine(have, saved)
        assertEquals(listOf("s1", "s2", "local"), merged.map { it.id })
    }

    @Test
    fun cleanTextTrimsAndCutsAtFiveHundred() {
        assertEquals("hi", GameChatRules.cleanText("   hi \n"))
        assertEquals(GameChatRules.MAX_TEXT, GameChatRules.cleanText("x".repeat(900)).length)
        assertEquals("", GameChatRules.cleanText("   "))
    }

    private companion object {
        const val HOUR = 3_600_000L
    }
}
