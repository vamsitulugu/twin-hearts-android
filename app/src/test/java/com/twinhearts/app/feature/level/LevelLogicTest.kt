package com.twinhearts.app.feature.level

import com.twinhearts.app.feature.level.data.parseXp
import com.twinhearts.app.feature.level.domain.Badge
import com.twinhearts.app.feature.level.domain.levelFromXp
import com.twinhearts.app.feature.level.domain.levelState
import com.twinhearts.app.feature.level.domain.xpForLevel
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelLogicTest {

    @Test
    fun levelFollowsTheWebFormula() {
        assertEquals(1, levelFromXp(0))
        assertEquals(1, levelFromXp(24))
        assertEquals(2, levelFromXp(25))
        assertEquals(3, levelFromXp(100))
        assertEquals(5, levelFromXp(600))
        assertEquals(25, xpForLevel(2))
        assertEquals(100, xpForLevel(3))
    }

    @Test
    fun startingCoupleHasNothingEarned() {
        val s = levelState(0)
        assertEquals(1, s.level)
        assertEquals(25, s.nextLevelXp)
        assertEquals(0, s.levelPercent)
        assertEquals(25, s.xpToNext)
        assertTrue(s.earned.isEmpty())
        assertEquals(Badge.SweetStart, s.nextBadge)
    }

    @Test
    fun midwayCoupleShowsProgress() {
        val s = levelState(60)
        assertEquals(2, s.level)
        assertEquals(100, s.nextLevelXp)
        assertEquals(60, s.levelPercent)
        assertEquals(40, s.xpToNext)
        assertEquals(listOf(Badge.SweetStart), s.earned)
        assertEquals(Badge.GettingClose, s.nextBadge)
        assertEquals(40, s.nextBadgePercent)
    }

    @Test
    fun everyBadgeUnlocked() {
        val s = levelState(1000)
        assertTrue(s.locked.isEmpty())
        assertNull(s.nextBadge)
        assertEquals(Badge.entries.size, s.earned.size)
    }

    @Test
    fun negativeXpCountsAsZero() {
        assertEquals(0, levelState(-5).xp)
    }

    @Test
    fun xpIsReadFromTheSavedState() {
        assertEquals(120, parseXp(JsonObject(mapOf("xp" to JsonPrimitive(120)))))
        assertEquals(7, parseXp(JsonObject(mapOf("xp" to JsonPrimitive("7.9")))))
        assertEquals(0, parseXp(JsonObject(mapOf("xp" to JsonPrimitive("abc")))))
        assertEquals(0, parseXp(JsonObject(emptyMap())))
        assertEquals(0, parseXp(null))
    }
}
