package com.twinhearts.app.feature.games

import com.twinhearts.app.feature.games.domain.engine.LudoEngine
import com.twinhearts.app.feature.games.domain.engine.LudoEventKind
import com.twinhearts.app.feature.games.domain.engine.LudoState
import com.twinhearts.app.feature.games.domain.engine.MemoryEngine
import com.twinhearts.app.feature.games.domain.engine.TicTacToeEngine
import com.twinhearts.app.feature.games.domain.engine.TicTacToeState
import com.twinhearts.app.feature.games.domain.engine.TodPhase
import com.twinhearts.app.feature.games.domain.engine.TodState
import com.twinhearts.app.feature.games.domain.engine.TodType
import com.twinhearts.app.feature.games.domain.engine.TruthOrDareEngine
import com.twinhearts.app.feature.games.domain.model.Outcome
import com.twinhearts.app.feature.games.domain.model.PlayerProfile
import com.twinhearts.app.feature.games.domain.model.Side
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.random.Random

/** Rules of the four games and the XP maths. Pure Kotlin, no Android needed. */
class GameEnginesTest {

    // ── Tic Tac Toe ──────────────────────────────────────────────

    @Test
    fun ttt_wrongTurnOrTakenCellIsRefused() {
        val s = TicTacToeState()
        assertNull(TicTacToeEngine.play(s, Side.Two, 0))
        val after = TicTacToeEngine.play(s, Side.One, 0)!!
        assertEquals("X", after.board[0])
        assertEquals(Side.Two, after.turn)
        assertNull(TicTacToeEngine.play(after, Side.Two, 0))
    }

    @Test
    fun ttt_rowWinScoresAndLocksTheBoard() {
        var s = TicTacToeState()
        for ((side, cell) in listOf(Side.One to 0, Side.Two to 3, Side.One to 1, Side.Two to 4, Side.One to 2)) {
            s = TicTacToeEngine.play(s, side, cell)!!
        }
        assertEquals(Outcome.One, s.outcome)
        assertEquals(listOf(0, 1, 2), s.winningLine)
        assertEquals(1, s.scoreOne)
        assertNull(TicTacToeEngine.play(s, Side.Two, 5))
    }

    @Test
    fun ttt_fullBoardIsADrawAndRematchKeepsScores() {
        // X O X / X O O / O X X : no line for anyone.
        val order = listOf(0, 1, 2, 4, 3, 5, 7, 6, 8)
        var s = TicTacToeState()
        var side = Side.One
        for (cell in order) {
            s = TicTacToeEngine.play(s, side, cell)!!
            side = side.other
        }
        assertEquals(Outcome.Draw, s.outcome)
        val again = TicTacToeEngine.rematch(s.copy(scoreOne = 2, scoreTwo = 1))
        assertEquals(2, again.scoreOne)
        assertEquals(Outcome.None, again.outcome)
        assertTrue(again.board.all { it.isEmpty() })
    }

    // ── Memory ───────────────────────────────────────────────────

    @Test
    fun memory_freshDeckHasEightPairs() {
        val s = MemoryEngine.fresh(Random(7))
        assertEquals(16, s.cards.size)
        assertEquals(8, s.cards.groupBy { it.emoji }.size)
        assertTrue(s.cards.groupBy { it.emoji }.values.all { it.size == 2 })
    }

    @Test
    fun memory_matchKeepsTurnAndMissPassesIt() {
        val s = MemoryEngine.fresh(Random(1))
        val first = 0
        val pair = s.cards.indices.first { it != first && s.cards[it].emoji == s.cards[first].emoji }
        val wrong = s.cards.indices.first { s.cards[it].emoji != s.cards[first].emoji }

        var hit = MemoryEngine.flip(s, Side.One, first)!!
        hit = MemoryEngine.flip(hit, Side.One, pair)!!
        val matched = MemoryEngine.resolve(hit, Side.One)!!
        assertTrue(matched.matched)
        assertEquals(Side.One, matched.state.turn)
        assertEquals(1, matched.state.scoreOne)

        var miss = MemoryEngine.flip(s, Side.One, first)!!
        miss = MemoryEngine.flip(miss, Side.One, wrong)!!
        val missed = MemoryEngine.resolve(miss, Side.One)!!
        assertFalse(missed.matched)
        assertEquals(Side.Two, missed.state.turn)
        assertTrue(missed.state.flipped.isEmpty())
    }

    @Test
    fun memory_thirdFlipAndWrongPlayerAreRefused() {
        val s = MemoryEngine.fresh(Random(3))
        assertNull(MemoryEngine.flip(s, Side.Two, 0))
        var t = MemoryEngine.flip(s, Side.One, 0)!!
        assertNull(MemoryEngine.flip(t, Side.One, 0))
        t = MemoryEngine.flip(t, Side.One, 1)!!
        assertNull(MemoryEngine.flip(t, Side.One, 2))
    }

    @Test
    fun memory_lastPairEndsTheGameWithTheHigherScoreWinning() {
        val cards = MemoryEngine.fresh(Random(2)).cards.map { it.copy(matched = true) }.toMutableList()
        cards[0] = cards[0].copy(matched = false)
        val mate = cards.indices.first { it != 0 && cards[it].emoji == cards[0].emoji }
        cards[mate] = cards[mate].copy(matched = false)
        var s = com.twinhearts.app.feature.games.domain.engine.MemoryState(cards = cards, scoreOne = 4, scoreTwo = 3)
        s = MemoryEngine.flip(s, Side.One, 0)!!
        s = MemoryEngine.flip(s, Side.One, mate)!!
        val r = MemoryEngine.resolve(s, Side.One)!!
        assertTrue(r.finished)
        assertEquals(Outcome.One, r.state.outcome) // 5 - 3
    }

    // ── Ludo ─────────────────────────────────────────────────────

    @Test
    fun ludo_needsASixToLeaveBase() {
        val s = LudoState()
        assertTrue(LudoEngine.movable(s.one, 5).isEmpty())
        assertEquals(listOf(0, 1, 2, 3), LudoEngine.movable(s.one, 6))
        val rolled = LudoEngine.roll(s, Side.One, 3)!!
        assertTrue(rolled.noMove)
        val passed = LudoEngine.passAfterNoMove(rolled, Side.One)
        assertEquals(Side.Two, passed.turn)
        assertEquals(0, passed.dice)
        assertEquals(LudoEventKind.NoMove, passed.history.first().kind)
    }

    @Test
    fun ludo_sixGivesAnotherTurnAndEntersOnTheStartCell() {
        val rolled = LudoEngine.roll(LudoState(), Side.One, 6)!!
        val move = LudoEngine.move(rolled, Side.One, 2)!!
        assertEquals(0, move.state.one[2])
        assertEquals(Side.One, move.state.turn)
        assertEquals(0, move.state.dice)
    }

    @Test
    fun ludo_cannotRollTwiceOrOutOfTurn() {
        val s = LudoState()
        assertNull(LudoEngine.roll(s, Side.Two, 4))
        val rolled = LudoEngine.roll(s, Side.One, 6)!!
        assertNull(LudoEngine.roll(rolled, Side.One, 2))
        assertNull(LudoEngine.move(rolled, Side.Two, 0))
    }

    @Test
    fun ludo_landingOnAnOpponentOutsideASafeCellCapturesIt() {
        // User one at ring 3 (cell 3, not safe). User two token at its own step 3 + ... find the step
        // whose ring cell is 5: start 26 + p = 5 (mod 52) -> p = 31.
        val state = LudoState(
            one = listOf(3, -1, -1, -1),
            two = listOf(31, -1, -1, -1),
            turn = Side.One,
            dice = 2,
        )
        val r = LudoEngine.move(state, Side.One, 0)!!
        assertEquals(5, r.state.one[0])
        assertEquals(listOf(0), r.captured)
        assertEquals(-1, r.state.two[0])
        assertEquals(Side.Two, r.state.turn)
    }

    @Test
    fun ludo_safeCellProtectsAToken() {
        // Ring cell 8 is a safe star. User one moves from 6 to 8; user two sits on ring 8 (p = 34).
        val state = LudoState(one = listOf(6, -1, -1, -1), two = listOf(34, -1, -1, -1), dice = 2)
        val r = LudoEngine.move(state, Side.One, 0)!!
        assertTrue(r.captured.isEmpty())
        assertEquals(34, r.state.two[0])
    }

    @Test
    fun ludo_needsTheExactRollToGoHomeAndAllHomeWins() {
        val almost = LudoState(one = listOf(57, 57, 57, 55), dice = 3)
        assertTrue(LudoEngine.movable(almost.one, 3).isEmpty())
        val exact = almost.copy(dice = 2)
        val r = LudoEngine.move(exact, Side.One, 3)!!
        assertTrue(r.reachedHome)
        assertTrue(r.won)
        assertEquals(Outcome.One, r.state.outcome)
        assertNull(LudoEngine.roll(r.state, Side.Two, 4))
    }

    @Test
    fun ludo_timeOutSkipsTheTurnOrAutoMoves() {
        val skipped = LudoEngine.timeOut(LudoState(), Side.One)!!
        assertEquals(Side.Two, skipped.turn)
        assertEquals(LudoEventKind.TimedOut, skipped.history.first().kind)

        val rolled = LudoEngine.roll(LudoState(), Side.One, 6)!!
        val auto = LudoEngine.timeOut(rolled, Side.One)!!
        assertEquals(0, auto.one[0])
    }

    @Test
    fun ludo_historyKeepsOnlyTheNewestEight() {
        var s = LudoState()
        repeat(12) {
            val rolled = LudoEngine.roll(s, s.turn, 1)!!
            s = LudoEngine.passAfterNoMove(rolled, rolled.turn)
        }
        assertEquals(LudoState.HISTORY_LIMIT, s.history.size)
    }

    @Test
    fun ludo_ringHas52UniqueCellsAndTokensMapToCells() {
        assertEquals(52, LudoEngine.ringCells.size)
        assertEquals(52, LudoEngine.ringCells.toSet().size)
        assertEquals(LudoEngine.ringCells[26], LudoEngine.cellOf(Side.Two, 0, 0))
        assertEquals(LudoEngine.center, LudoEngine.cellOf(Side.One, 0, 57))
        assertEquals(1 to 1, LudoEngine.cellOf(Side.One, 0, -1))
        assertEquals(10 to 10, LudoEngine.cellOf(Side.Two, 0, -1))
    }

    // ── Truth or Dare ────────────────────────────────────────────

    @Test
    fun tod_fullRoundPassesTheTurnAndCountsTheStreak() {
        var s = TodState()
        s = TruthOrDareEngine.startSpin(s, Side.One)!!
        assertEquals(TodPhase.Spinning, s.phase)
        assertNull(TruthOrDareEngine.startSpin(s, Side.One))
        s = TruthOrDareEngine.finishSpin(s)!!
        assertNull(TruthOrDareEngine.chooseType(s, Side.Two, TodType.Truth))
        s = TruthOrDareEngine.chooseType(s, Side.One, TodType.Truth)!!
        assertNull(TruthOrDareEngine.send(s, Side.One, "   "))
        s = TruthOrDareEngine.send(s, Side.One, "  Favourite memory?  ")!!
        assertEquals("Favourite memory?", s.current!!.text)
        assertNull(TruthOrDareEngine.answer(s, Side.One, "me"))
        s = TruthOrDareEngine.answer(s, Side.Two, "Our first trip")!!
        s = TruthOrDareEngine.complete(s)!!
        assertEquals(Side.Two, s.turn)
        assertEquals(1, s.streak)
        assertEquals(TodPhase.Idle, s.phase)
        assertEquals("Our first trip", s.history.first().answer)
    }

    @Test
    fun tod_skipResetsTheStreakAndSwapsRoles() {
        var s = TodState(streak = 4, phase = TodPhase.Compose, pendingType = TodType.Dare)
        s = TruthOrDareEngine.send(s, Side.One, "Dance")!!
        val skipped = TruthOrDareEngine.skip(s, "Skipped")!!
        assertEquals(0, skipped.streak)
        assertEquals(Side.Two, skipped.turn)
        assertEquals("Skipped", skipped.history.first().answer)
        assertEquals(Side.Two, TruthOrDareEngine.answererOf(s))
    }

    @Test
    fun tod_historyIsCappedAtTen() {
        var s = TodState()
        repeat(14) {
            s = s.copy(phase = TodPhase.Compose)
            s = TruthOrDareEngine.send(s, s.turn, "Q$it")!!
            s = TruthOrDareEngine.dareDone(s, s.turn.other, "Completed")!!
            s = TruthOrDareEngine.complete(s)!!
        }
        assertEquals(10, s.history.size)
        assertEquals("Q13", s.history.first().text)
    }

    // ── Profile / XP ─────────────────────────────────────────────

    @Test
    fun profile_levelsFollowTheWebFormula() {
        assertEquals(1, PlayerProfile.levelFor(0))
        assertEquals(2, PlayerProfile.levelFor(28))
        assertEquals(3, PlayerProfile.levelFor(112))
        assertEquals(28, PlayerProfile.xpForLevel(2))
        assertEquals(112, PlayerProfile.xpForLevel(3))
    }

    @Test
    fun profile_xpNeverGoesBelowZeroAndStreakCountsConsecutiveDays() {
        val day1 = LocalDate.of(2026, 10, 26)
        var p = PlayerProfile().withXp(8, day1)
        assertEquals(1, p.streakDays)
        p = p.withXp(8, day1)
        assertEquals(1, p.streakDays)
        p = p.withXp(8, day1.plusDays(1))
        assertEquals(2, p.streakDays)
        p = p.withXp(8, day1.plusDays(5))
        assertEquals(1, p.streakDays)
        assertEquals(0, PlayerProfile().withXp(-5, day1).xp)
    }

    @Test
    fun profile_favoritesToggle() {
        val p = PlayerProfile().toggleFavorite(com.twinhearts.app.feature.games.domain.model.GameId.Ludo)
        assertEquals(listOf("ludo"), p.favorites)
        assertTrue(p.toggleFavorite(com.twinhearts.app.feature.games.domain.model.GameId.Ludo).favorites.isEmpty())
        assertNotNull(p.levelProgress)
    }
}
