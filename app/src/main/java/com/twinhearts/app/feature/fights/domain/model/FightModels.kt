package com.twinhearts.app.feature.fights.domain.model

import java.time.LocalDate

/** Why this file exists: the plain shapes the Fight Log screens use (no network types, R-40). */

enum class FightStatus { Open, Resolved }

/** The eight categories from the web page. [tone] is the tile colour as 0xRRGGBB. */
enum class FightCategory(val label: String, val tone: Long) {
    Communication("Communication", 0xF472B6),
    Time("Time", 0xF87171),
    Plans("Plans", 0xA78BFA),
    Jealousy("Jealousy", 0xF472B6),
    Family("Family", 0x60A5FA),
    Money("Money", 0xFBBF24),
    Habits("Habits", 0x2DD4BF),
    Other("Other", 0xA1A1AA);

    companion object {
        /** Unknown or missing names show as "Other", exactly like the web. */
        fun from(raw: String?): FightCategory = entries.firstOrNull { it.label == raw } ?: Other
    }
}

/** "Who started it?" choices. */
enum class FightStarter(val key: String) { Me("me"), Partner("partner"), Both("both"), Unsure("unsure") }

/** The nine feelings. [value] is what is stored in the shared data (same words as the web). */
enum class FightFeeling(val value: String) {
    Angry("Angry"), Hurt("Hurt"), Sad("Sad"), Frustrated("Frustrated"), Anxious("Anxious"),
    Misunderstood("Misunderstood"), Ignored("Ignored"), Tired("Tired"), Confused("Confused"),
}

/** Resolution approaches. Each has a "done" wording (resolved fights) and a "next" wording (open ones). */
enum class FightApproach(val key: String) {
    Talk("talk"), Apology("apology"), Space("space"), Compromise("compromise"), Activity("activity"), Time("time");

    companion object {
        fun from(key: String): FightApproach? = entries.firstOrNull { it.key == key }
    }
}

enum class ActivityFilter(val label: String) {
    All("All"), Communication("Communication"), Fun("Fun"), Calm("Calm"), Growth("Growth"),
}

/** The eight reconnect activities. Text lives in strings (fights_act_<key>_*). */
enum class ReconnectActivity(val key: String, val category: ActivityFilter, val minutes: Int, val tone: Long) {
    Mirror("mirror", ActivityFilter.Communication, 10, 0xF472B6),
    Letter("letter", ActivityFilter.Communication, 15, 0xFBBF24),
    Walk("walk", ActivityFilter.Calm, 15, 0x60A5FA),
    Appreciation("appreciation", ActivityFilter.Growth, 10, 0xF87171),
    Hug("hug", ActivityFilter.Calm, 5, 0x60A5FA),
    Play("play", ActivityFilter.Fun, 20, 0xF472B6),
    Cook("cook", ActivityFilter.Fun, 30, 0xFBBF24),
    WhyDive("whydive", ActivityFilter.Growth, 15, 0xF472B6),
}

/** Time range of the Conflict Resolution Rate card. */
enum class FightRange(val days: Int?) { Days30(30), Days90(90), Days180(180), All(null) }

/** One logged fight, as stored in the couple's shared data. Dates are `yyyy-MM-dd` text like the web. */
data class Fight(
    val id: Long,
    val title: String,
    val reason: String = "",
    val category: String = FightCategory.Other.label,
    val intensity: Int = 5,
    /** `user1`, `user2`, `both` or `unsure`. */
    val startedBy: String = "",
    val feelings: List<String> = emptyList(),
    val need: String = "",
    val approach: List<String> = emptyList(),
    val notes: String = "",
    val status: FightStatus = FightStatus.Open,
    val date: String = "",
    val resolvedDate: String? = null,
) {
    val localDate: LocalDate? get() = runCatching { LocalDate.parse(date) }.getOrNull()
    val isOpen: Boolean get() = status == FightStatus.Open
}

/** What is typed in the three-step Log Fight wizard. */
data class FightForm(
    val title: String = "",
    val reason: String = "",
    val category: String = FightCategory.Communication.label,
    val intensity: Int = 5,
    val started: FightStarter = FightStarter.Both,
    val status: FightStatus = FightStatus.Open,
    val feelings: List<String> = emptyList(),
    val need: String = "",
    val approach: List<String> = emptyList(),
    val notes: String = "",
)

/** A wizard kept on this phone only (never sent to the partner). */
data class FightDraft(val form: FightForm, val step: Int)

/** Who is using the page. */
data class FightIdentity(val coupleId: String, val role: String, val myName: String, val partnerName: String)

/** Numbers shown on the dashboard. */
data class FightStats(val total: Int, val resolved: Int, val open: Int, val intensity: Double?, val days: Double?)

/** Which sentence the insight strip shows under the ring. */
enum class FightInsight { None, Intense, AllResolved, Great, StillOpen }
