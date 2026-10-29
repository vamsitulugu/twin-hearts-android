package com.twinhearts.app.feature.level.domain

import kotlin.math.floor
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Why this file exists: the Couple Level rules, copied from the web page (index.html:
 * levelFromXP, xpForLevel, BADGES, XP_ACTIONS). Plain Kotlin, no Android, so it can be unit tested.
 */

/** Names of the line icons the page uses. The screen turns each into a drawn icon. */
enum class LevelIcon { Crown, Heart, Target, Chevron, Zap, Lock, Sprout, Handshake, Users, Gem, Award, Chat, Check, Music, Pulse, Capsule, Star, Gift }

/** The five badges, in the order they unlock. [xp] is the total XP needed. */
enum class Badge(val xp: Int, val emoji: String, val icon: LevelIcon) {
    SweetStart(50, "🌱", LevelIcon.Sprout),
    GettingClose(150, "💞", LevelIcon.Handshake),
    BestFriends(300, "🤝", LevelIcon.Users),
    Soulmates(600, "💍", LevelIcon.Gem),
    ForeverUs(1000, "👑", LevelIcon.Crown),
}

/** Colour of an XP action icon on the web page: the theme accent, green or gold. */
enum class XpTone { Accent, Green, Gold }

/** The ways to earn XP, in the order the web page lists them. */
enum class XpAction(val xp: Int, val icon: LevelIcon, val tone: XpTone) {
    Chatting(1, LevelIcon.Chat, XpTone.Accent),
    HabitDone(5, LevelIcon.Check, XpTone.Green),
    FightResolved(6, LevelIcon.Zap, XpTone.Gold),
    ShareSong(5, LevelIcon.Music, XpTone.Accent),
    Wellness(2, LevelIcon.Pulse, XpTone.Accent),
    LoveCapsule(15, LevelIcon.Capsule, XpTone.Accent),
    DreamCompleted(8, LevelIcon.Star, XpTone.Gold),
    Surprise(10, LevelIcon.Gift, XpTone.Accent),
}

fun levelFromXp(xp: Int): Int = floor(sqrt(xp / XP_STEP)).toInt() + 1

/** Total XP at which [level] starts. */
fun xpForLevel(level: Int): Int = (level - 1) * (level - 1) * XP_STEP.toInt()

/** Everything the screen shows, worked out from one XP number. */
data class LevelState(
    val xp: Int,
    val level: Int,
    /** XP where the next level starts. */
    val nextLevelXp: Int,
    /** 0–100. The web page shows XP divided by [nextLevelXp], not progress inside the level. */
    val levelPercent: Int,
    val xpToNext: Int,
    val earned: List<Badge>,
    val locked: List<Badge>,
) {
    /** The badge being worked on, or null when every badge is unlocked. */
    val nextBadge: Badge? get() = locked.firstOrNull()

    /** 0–100 progress towards [nextBadge]. */
    val nextBadgePercent: Int get() = nextBadge?.let { percent(xp, it.xp) } ?: 100
}

fun levelState(rawXp: Int): LevelState {
    val xp = rawXp.coerceAtLeast(0)
    val level = levelFromXp(xp)
    val nextBase = xpForLevel(level + 1)
    return LevelState(
        xp = xp,
        level = level,
        nextLevelXp = nextBase,
        levelPercent = percent(xp, nextBase),
        xpToNext = (nextBase - xp).coerceAtLeast(0),
        earned = Badge.entries.filter { xp >= it.xp },
        locked = Badge.entries.filter { xp < it.xp },
    )
}

private fun percent(part: Int, whole: Int): Int =
    if (whole <= 0) 100 else (part * PERCENT.toDouble() / whole).roundToInt().coerceIn(0, PERCENT)

private const val XP_STEP = 25.0
private const val PERCENT = 100
