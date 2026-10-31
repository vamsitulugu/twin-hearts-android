package com.twinhearts.app.feature.games

import com.twinhearts.app.feature.games.domain.engine.*
import com.twinhearts.app.feature.games.domain.model.Side
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

/** Rules of Mind Reader, Draw & Guess, Don't Touch The Bomb and Picture Puzzle. Pure Kotlin. */
class NewGameEnginesTest {

    private fun check(name: String, ok: Boolean) = assertTrue(name, ok)

    @Test
    fun mindReader_rules() {
        val r = Random(7)
        check("mr exact", MindReaderEngine.classify("Pizza", "pizza!", false) == MrMatch.Exact)
        check("mr typo is exact", MindReaderEngine.classify("sushi roll", "sushi rolls", false) == MrMatch.Exact)
        check("mr near", MindReaderEngine.classify("the beach", "beach vacation", false) == MrMatch.Near)
        check("mr funny", MindReaderEngine.classify("pasta lol", "a donkey riding a bike", false) == MrMatch.Funny)
        check("mr diff", MindReaderEngine.classify("pizza", "sushi", false) == MrMatch.Different)
        check("mr choice exact only", MindReaderEngine.classify("Pizza", "Pizzas", true) == MrMatch.Different)
        val pool = (0 until 5).map { MrRef(it, "love") }
        var s = MindReaderEngine.start(MindReaderState(), MrMode.BestOfTen, "random", pool, 1000, r)!!
        check("mr countdown", s.phase == MrPhase.Countdown && s.roundIndex == 1 && s.targetRounds == 10)
        s = MindReaderEngine.startAnswering(s, 5000)!!
        s = MindReaderEngine.lock(s, Side.One, "pizza", false)!!
        check("mr one lock waits", s.phase == MrPhase.Answering && s.lockedOne && !s.lockedTwo)
        check("mr no relock", MindReaderEngine.lock(s, Side.One, "x", false) == null)
        s = MindReaderEngine.lock(s, Side.Two, "pizza", false)!!
        check("mr reveal exact 100%", s.phase == MrPhase.Reveal && s.match == MrMatch.Exact && s.compatPct == 100 && s.curStreak == 1)
        check("mr history+catstat", s.history.size == 1 && s.catStats["love"]?.matched == 1)
        s = MindReaderEngine.toggleFavorite(s)!!
        check("mr fav", s.history[0].fav)
        var cur = s
        for (i in 1..9) {
            cur = MindReaderEngine.next(cur, pool, 0, r)!!
            cur = MindReaderEngine.startAnswering(cur, 0)!!
            cur = MindReaderEngine.lock(cur, Side.One, "same", false)!!
            cur = MindReaderEngine.lock(cur, Side.Two, "same", false)!!
        }
        check("mr 10 rounds", cur.roundIndex == 10 && cur.totalRounds == 10)
        check("mr achievements", MindReaderEngine.ACH_SOULMATES in cur.achievements && MindReaderEngine.ACH_STREAK5 in cur.achievements && MindReaderEngine.ACH_TEN_ROUNDS in cur.achievements && MindReaderEngine.ACH_CATEGORY_MASTER in cur.achievements)
        check("mr summary after round 10", MindReaderEngine.next(cur, pool, 0, r)!!.phase == MrPhase.Summary)
        check("mr no question reuse in 5", run { var st = MindReaderEngine.start(MindReaderState(), MrMode.Endless, "random", pool, 0, r)!!; val seen = mutableSetOf(st.question); for (i in 1..4) { st = MindReaderEngine.startAnswering(st,0)!!; st = MindReaderEngine.lock(st, Side.One,"a",false)!!; st = MindReaderEngine.lock(st, Side.Two,"b",false)!!; st = MindReaderEngine.next(st, pool, 0, r)!!; seen.add(st.question) }; seen.size == 5 })
        check("mr empty pool", MindReaderEngine.start(MindReaderState(), MrMode.Random, "x", emptyList(), 0) == null)
    }

    @Test
    fun draw_rules() {
        val r = Random(7)
        check("draw exact", DrawEngine.localMatch("Cat", "cat"))
        check("draw plural", DrawEngine.localMatch("stars", "star"))
        check("draw typo", DrawEngine.localMatch("rainbpw", "rainbow"))
        check("draw compound", DrawEngine.localMatch("icecream", "ice cream"))
        check("draw wrong", !DrawEngine.localMatch("dog", "cat"))
        check("draw semantic", DrawEngine.semanticMatch("boy", "human", setOf("boy", "man"), emptySet()))
        var d = DrawState()
        check("draw guesser cannot set word", DrawEngine.setWord(d, Side.Two, "cat", 0) == null)
        d = DrawEngine.setWord(d, Side.One, " cat ", 100)!!
        check("draw word", d.word == "cat" && d.wordSet)
        val st = DrawStroke(1, DrawTool.Pen, 0, 6, listOf(0f, 0f, 0.5f, 0.5f))
        check("draw only drawer strokes", DrawEngine.addStroke(d, Side.Two, st) == null)
        d = DrawEngine.addStroke(d, Side.One, st)!!
        check("draw undo", DrawEngine.undo(d, Side.One)!!.strokes.isEmpty())
        check("draw drawer cannot guess", DrawEngine.guess(d, Side.One, "cat", true, 5) == null)
        d = DrawEngine.guess(d, Side.Two, "dog", false, 200)!!
        check("draw wrong guess logged", d.guesses.size == 1 && d.scoreTwo == 0 && d.wordSet)
        d = DrawEngine.guess(d, Side.Two, "cat", true, 300)!!
        check("draw right guess scores + swaps", d.scoreTwo == 1 && d.drawer == Side.Two && !d.wordSet && d.round == 2 && d.result?.word == "cat" && d.result?.by == Side.Two && d.strokes.isEmpty())
        check("draw hint", DrawEngine.hint("ice cream", 0) == "_ _ _   _ _ _ _ _" && DrawEngine.hint("cat", 31).startsWith("C") && DrawEngine.hint("cat", 46).endsWith("(3 letters)"))
        check("draw seconds", DrawEngine.secondsLeft(d.copy(roundStartedAt = 0), 30_000) == 45)
    }

    @Test
    fun bomb_rules() {
        val r = Random(7)
        var b = BombEngine.start(BombState(), BombDifficulty.Easy, false, false, 0, r)!!
        check("bomb field", b.bombs.size == 8 && b.bombs.none { it in setOf(0, 63, 1, 8, 62, 55) } && b.bombs.toSet().size == b.bombs.size)
        check("bomb hard count", BombEngine.start(BombState(), BombDifficulty.Hard, false, false, 0, r)!!.bombs.size == 27)
        check("bomb extreme fog+moving", BombEngine.start(BombState(), BombDifficulty.Extreme, false, false, 0, r)!!.fogMode)
        b = BombEngine.beginPlaying(b, 1000)!!
        check("bomb guide cannot move", BombEngine.move(b, Side.One, 1, 2000) == null)
        check("bomb no diagonal", BombEngine.move(b, Side.Two, 9, 2000) == null)
        check("bomb safe step", BombEngine.move(b, Side.Two, 1, 2000)!!.pos == 1)
        val boom = BombEngine.move(b.copy(bombs = listOf(1)), Side.Two, 1, 2000)!!
        check("bomb explode", boom.result == BombResult.Exploded && boom.explodedAt == 1 && boom.phase == BombPhase.Result)
        val near = b.copy(bombs = emptyList(), pos = 62)
        val win = BombEngine.move(near, Side.Two, 63, 11_000)!!
        check("bomb win", win.result == BombResult.Won && win.finishMs == 10_000L && win.teamStreak == 1 && win.bestTimeMs == 10_000L)
        val nr = BombEngine.nextRound(win)!!
        check("bomb next round", nr.phase == BombPhase.Lobby && nr.round == 2 && nr.teamStreak == 1)
        val r2 = BombEngine.start(nr, BombDifficulty.Easy, false, false, 0, r)!!
        check("bomb swaps guide", r2.guide == Side.Two && r2.mover == Side.One)
        check("bomb timeout", BombEngine.timeout(b)!!.result == BombResult.Timeout)
        check("bomb ping only guide", BombEngine.ping(b, Side.Two, 5, 1) == null && BombEngine.ping(b, Side.One, 5, 1)!!.ping?.idx == 5)
        val rel = BombEngine.relocateBomb(b, r)!!
        check("bomb relocate keeps count and safe", rel.bombs.size == b.bombs.size && rel.bombs.toSet().size == rel.bombs.size && 0 !in rel.bombs && 63 !in rel.bombs)
        check("bomb adjacency wrap", !BombEngine.adjacent(8, 7, 8) && BombEngine.adjacent(8, 7, 15) && BombEngine.adjacent(8, 0, 1))
        check("bomb fog", BombEngine.isFogged(b.copy(fogMode = true), 40) && !BombEngine.isFogged(b.copy(fogMode = true), 63) && !BombEngine.isFogged(b.copy(fogMode = true), 1))
        check("bomb hint glow", BombEngine.isDanger(b.copy(hintMode = true, bombs = listOf(10)), 9) && !BombEngine.isDanger(b.copy(hintMode = true, bombs = listOf(7)), 8))
        check("bomb chat cap", run { var x = b; for (i in 1..30) x = BombEngine.chat(x, Side.One, "m$i", i.toLong())!!; x.chat.size == 20 && x.chat.last().text == "m30" })
        check("bomb blank chat refused", BombEngine.chat(b, Side.One, "  ", 1) == null)
    }

    @Test
    fun puzzle_rules() {
        val r = Random(7)
        var p = PuzzleState()
        check("puzzle needs photo", PuzzleEngine.start(p, 3, "", 0, r) == null)
        p = PuzzleEngine.start(p, 3, "http://x/y.jpg", 0, r)!!
        check("puzzle deal", p.pieces.size == 9 && p.pieces.count { it.owner == Side.One } == 5 && p.pieces.map { it.id }.toSet().size == 9)
        p = PuzzleEngine.beginPlaying(p, 1000)!!
        val mine = p.pieces.first { it.owner == Side.One }
        check("puzzle cannot place partner piece", PuzzleEngine.place(p, Side.Two, mine.id, mine.row, mine.col, 0) == null)
        // piece turned wrong does not stick
        val turned = p.copy(pieces = p.pieces.map { if (it.id == mine.id) it.copy(rot = 90) else it })
        check("puzzle wrong rotation rejected", PuzzleEngine.place(turned, Side.One, mine.id, mine.row, mine.col, 0)!!.let { !it.correct && it.state.moves == 1 })
        val upright = p.copy(pieces = p.pieces.map { if (it.id == mine.id) it.copy(rot = 0) else it })
        check("puzzle wrong slot rejected", PuzzleEngine.place(upright, Side.One, mine.id, (mine.row + 1) % 3, mine.col, 0)!!.correct.not())
        check("puzzle correct sticks", PuzzleEngine.place(upright, Side.One, mine.id, mine.row, mine.col, 0)!!.let { it.correct && it.state.placedCount == 1 })
        check("puzzle rotate cycles", run { var x = upright; repeat(4) { x = PuzzleEngine.rotate(x, Side.One, mine.id)!! }; x.pieces.first { it.id == mine.id }.rot == 0 })
        var full = p.copy(pieces = p.pieces.map { it.copy(rot = 0) })
        var last: PuzzlePlacement? = null
        for (pc in full.pieces) { last = PuzzleEngine.place(full, pc.owner, pc.id, pc.row, pc.col, 61_000)!!; full = last.state }
        check("puzzle completes", last!!.finished && full.phase == PuzzlePhase.Result && full.finishMs == 60_000L && full.teamStreak == 1 && full.bestMs == 60_000L)
        check("puzzle next keeps streak", PuzzleEngine.nextRound(full)!!.let { it.phase == PuzzlePhase.Setup && it.teamStreak == 1 && it.round == 2 })
        check("puzzle restore", PuzzleEngine.restorePlaced(p, mine.id, 0)!!.pieces.first { it.id == mine.id }.placed)
        check("puzzle elapsed", PuzzleEngine.elapsedText(125_000) == "2:05")
        check("puzzle rewards", PuzzleEngine.xpFor(5) == 25 && PuzzleEngine.coinsFor(5) == 10)
    }

    @Test
    fun chat_restore() {
        val r = Random(7)
        val mineLines = listOf(ChatLine(Side.One, "hi", 5))
        check("chat restore", GameChat.restore(emptyList(), mineLines).size == 1 && GameChat.restore(mineLines, mineLines).size == 1)
    }

}
