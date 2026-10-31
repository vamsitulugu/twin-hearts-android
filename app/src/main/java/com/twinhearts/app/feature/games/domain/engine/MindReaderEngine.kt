package com.twinhearts.app.feature.games.domain.engine

import com.twinhearts.app.feature.games.domain.model.Side
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.random.Random

@Serializable
enum class MrPhase {
    @SerialName("lobby") Lobby,
    @SerialName("countdown") Countdown,
    @SerialName("answering") Answering,
    @SerialName("reveal") Reveal,
    @SerialName("summary") Summary,
}

@Serializable
enum class MrMode {
    @SerialName("random") Random,
    @SerialName("category") Category,
    @SerialName("best10") BestOfTen,
    @SerialName("endless") Endless,
}

/** How alike the two answers were. */
@Serializable
enum class MrMatch {
    @SerialName("exact") Exact,
    @SerialName("near") Near,
    @SerialName("funny") Funny,
    @SerialName("diff") Different,
}

/** One question the engine may pick: its catalogue index and its category key. */
data class MrRef(val index: Int, val cat: String)

@Serializable
data class MrCatStat(val total: Int = 0, val matched: Int = 0)

@Serializable
data class MrHistoryItem(
    /** Index into the question catalogue. */
    val q: Int,
    val cat: String,
    val answerOne: String,
    val answerTwo: String,
    val match: MrMatch,
    val fav: Boolean = false,
)

/**
 * Mind Reader. Both answer the same question in secret, lock in, then flip together.
 * Everything about the running match (scores, streaks, achievements) is kept in this one state.
 */
@Serializable
data class MindReaderState(
    val phase: MrPhase = MrPhase.Lobby,
    val mode: MrMode = MrMode.Random,
    val category: String = MindReaderEngine.RANDOM,
    val roundIndex: Int = 0,
    /** 0 means "no limit". */
    val targetRounds: Int = 0,
    /** Index into the question catalogue, or -1 before the first round. */
    val question: Int = -1,
    val questionCat: String = "",
    val used: List<Int> = emptyList(),
    val countdownAt: Long = 0,
    val answerOne: String? = null,
    val answerTwo: String? = null,
    val lockedOne: Boolean = false,
    val lockedTwo: Boolean = false,
    val roundStartedAt: Long = 0,
    val match: MrMatch? = null,
    val compatPct: Int = 0,
    val totalPts: Int = 0,
    val maxPts: Int = 0,
    val totalRounds: Int = 0,
    val bestStreak: Int = 0,
    val curStreak: Int = 0,
    val catStats: Map<String, MrCatStat> = emptyMap(),
    val funnyMatches: Int = 0,
    val achievements: List<String> = emptyList(),
    val history: List<MrHistoryItem> = emptyList(),
) {
    fun answerOf(side: Side): String? = if (side == Side.One) answerOne else answerTwo
    fun lockedOf(side: Side): Boolean = if (side == Side.One) lockedOne else lockedTwo
}

object MindReaderEngine {

    const val RANDOM = "random"
    const val COUNTDOWN_MS = 3_000L
    const val ANSWER_SECONDS = 60
    const val BEST_OF = 10
    const val HISTORY_LIMIT = 60
    const val NO_ANSWER = "(no answer)"

    const val ACH_SOULMATES = "soulmates"
    const val ACH_STREAK5 = "streak5"
    const val ACH_TEN_ROUNDS = "ten_rounds"
    const val ACH_CATEGORY_MASTER = "category_master"
    const val ACH_FUNNY_BUNCH = "funny_bunch"

    val categories = listOf(
        "love", "relationship", "movies", "music", "travel", "dreams", "food", "future", "habits",
        "personality", "funny", "deep", "memories", "dating", "lifestyle", "family", "goals", "random",
    )

    fun points(match: MrMatch): Int = when (match) {
        MrMatch.Exact -> 10
        MrMatch.Near -> 5
        MrMatch.Funny -> 3
        MrMatch.Different -> 1
    }

    fun xp(match: MrMatch): Int = when (match) {
        MrMatch.Exact -> 12
        MrMatch.Near -> 8
        MrMatch.Funny -> 6
        MrMatch.Different -> 3
    }

    fun coins(match: MrMatch): Int = when (match) {
        MrMatch.Exact -> 6
        MrMatch.Near -> 4
        MrMatch.Funny -> 3
        MrMatch.Different -> 1
    }

    private val playful = Regex("[😂🤣😅😆!?]|lol|lmao|haha", RegexOption.IGNORE_CASE)

    /** exact / near / funny / different, like the web app. Choice questions only match exactly. */
    fun classify(a: String, b: String, isChoice: Boolean): MrMatch {
        val na = TextMatch.loose(a)
        val nb = TextMatch.loose(b)
        if (na.isEmpty() || nb.isEmpty()) return MrMatch.Different
        if (isChoice) return if (na == nb) MrMatch.Exact else MrMatch.Different
        if (na == nb) return MrMatch.Exact
        val maxLen = maxOf(na.length, nb.length)
        val similarity = 1.0 - TextMatch.levenshtein(na, nb).toDouble() / maxOf(1, maxLen)
        if (similarity >= 0.72) return MrMatch.Exact
        val wa = na.split(' ').filter { it.length > 2 }.toSet()
        val wb = nb.split(' ').filter { it.length > 2 }.toSet()
        val overlap = wa.count { it in wb }
        val overlapRatio = overlap.toDouble() / maxOf(1, minOf(wa.size, wb.size))
        if (similarity >= 0.42 || overlapRatio >= 0.5) return MrMatch.Near
        if ((playful.containsMatchIn(a) || playful.containsMatchIn(b)) && maxLen <= 40) return MrMatch.Funny
        return MrMatch.Different
    }

    /**
     * Starts a new match and its first round. Lifetime numbers (compatibility, streak record,
     * achievements, history) carry over, like the web app. Null when [pool] has no question.
     */
    fun start(
        state: MindReaderState,
        mode: MrMode,
        category: String,
        pool: List<MrRef>,
        now: Long,
        random: Random = Random.Default,
    ): MindReaderState? {
        val fresh = state.copy(
            phase = MrPhase.Lobby,
            mode = mode,
            category = category,
            roundIndex = 0,
            targetRounds = if (mode == MrMode.BestOfTen) BEST_OF else 0,
            question = -1,
            used = emptyList(),
            answerOne = null,
            answerTwo = null,
            lockedOne = false,
            lockedTwo = false,
            match = null,
            curStreak = 0,
        )
        return beginRound(fresh, pool, now, random)
    }

    private fun beginRound(state: MindReaderState, pool: List<MrRef>, now: Long, random: Random): MindReaderState? {
        if (pool.isEmpty()) return null
        var used = state.used
        var candidates = pool.filter { it.index !in used }
        if (candidates.isEmpty()) {
            used = emptyList()
            candidates = pool
        }
        val pick = candidates[random.nextInt(candidates.size)]
        return state.copy(
            phase = MrPhase.Countdown,
            question = pick.index,
            questionCat = pick.cat,
            used = used + pick.index,
            roundIndex = state.roundIndex + 1,
            answerOne = null,
            answerTwo = null,
            lockedOne = false,
            lockedTwo = false,
            match = null,
            countdownAt = now + COUNTDOWN_MS,
        )
    }

    /** The countdown is over: people can type now. */
    fun startAnswering(state: MindReaderState, now: Long): MindReaderState? {
        if (state.phase != MrPhase.Countdown) return null
        return state.copy(phase = MrPhase.Answering, roundStartedAt = now)
    }

    fun secondsLeft(state: MindReaderState, now: Long): Int =
        maxOf(0, ANSWER_SECONDS - ((now - state.roundStartedAt) / 1000).toInt())

    /**
     * [side] locks [answer]. When the second person locks, the answers are flipped and the round
     * is scored right here, so whichever phone locks last settles it.
     */
    fun lock(state: MindReaderState, side: Side, answer: String, isChoice: Boolean): MindReaderState? {
        if (state.phase != MrPhase.Answering || state.lockedOf(side)) return null
        val text = answer.trim()
        if (text.isEmpty()) return null
        val locked = if (side == Side.One) {
            state.copy(answerOne = text, lockedOne = true)
        } else {
            state.copy(answerTwo = text, lockedTwo = true)
        }
        return if (locked.lockedOne && locked.lockedTwo) reveal(locked, isChoice) else locked
    }

    private fun reveal(state: MindReaderState, isChoice: Boolean): MindReaderState {
        val a1 = state.answerOne.orEmpty()
        val a2 = state.answerTwo.orEmpty()
        val match = classify(a1, a2, isChoice)
        val totalPts = state.totalPts + points(match)
        val maxPts = state.maxPts + 10
        val matched = match == MrMatch.Exact || match == MrMatch.Near
        val curStreak = if (matched) state.curStreak + 1 else 0
        val cat = state.questionCat
        val previousStat = state.catStats[cat] ?: MrCatStat()
        val stats = state.catStats + (cat to MrCatStat(previousStat.total + 1, previousStat.matched + if (matched) 1 else 0))
        val item = MrHistoryItem(state.question, cat, a1, a2, match)
        var next = state.copy(
            phase = MrPhase.Reveal,
            match = match,
            totalPts = totalPts,
            maxPts = maxPts,
            compatPct = Math.round(totalPts * 100f / maxPts),
            totalRounds = state.totalRounds + 1,
            curStreak = curStreak,
            bestStreak = maxOf(state.bestStreak, curStreak),
            funnyMatches = state.funnyMatches + if (match == MrMatch.Funny) 1 else 0,
            catStats = stats,
            history = (listOf(item) + state.history).take(HISTORY_LIMIT),
        )
        next = next.copy(achievements = unlock(next))
        return next
    }

    private fun unlock(state: MindReaderState): List<String> {
        val have = state.achievements.toMutableList()
        fun grant(id: String, earned: Boolean) {
            if (earned && id !in have) have.add(id)
        }
        grant(ACH_SOULMATES, state.compatPct >= 90)
        grant(ACH_STREAK5, state.bestStreak >= 5)
        grant(ACH_TEN_ROUNDS, state.totalRounds >= 10)
        grant(ACH_CATEGORY_MASTER, state.catStats.values.any { it.total >= 5 && it.matched.toFloat() / it.total >= 0.8f })
        grant(ACH_FUNNY_BUNCH, state.funnyMatches >= 5)
        return have
    }

    /** "Next question", or the summary when the last round of a Best of 10 is done. */
    fun next(state: MindReaderState, pool: List<MrRef>, now: Long, random: Random = Random.Default): MindReaderState? {
        if (state.phase != MrPhase.Reveal) return null
        if (state.targetRounds > 0 && state.roundIndex >= state.targetRounds) {
            return state.copy(phase = MrPhase.Summary)
        }
        return beginRound(state, pool, now, random)
    }

    fun endMatch(state: MindReaderState): MindReaderState? =
        if (state.phase == MrPhase.Lobby || state.phase == MrPhase.Summary) null else state.copy(phase = MrPhase.Summary)

    fun backToLobby(state: MindReaderState): MindReaderState = state.copy(phase = MrPhase.Lobby)

    /** Stars or un-stars the question that was just revealed. */
    fun toggleFavorite(state: MindReaderState): MindReaderState? {
        val first = state.history.firstOrNull() ?: return null
        return state.copy(history = listOf(first.copy(fav = !first.fav)) + state.history.drop(1))
    }

    fun percentOf(stat: MrCatStat): Int = if (stat.total == 0) 0 else Math.round(stat.matched * 100f / stat.total)
}
