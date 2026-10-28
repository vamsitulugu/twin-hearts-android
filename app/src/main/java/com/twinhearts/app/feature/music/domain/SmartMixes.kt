package com.twinhearts.app.feature.music.domain

import com.twinhearts.app.feature.music.domain.model.Song
import java.time.Duration
import java.time.Instant

/** The "Made for Us" cards. Names are filled in by the screen (they use the partner's name). */
enum class MixKind { Ours, Both, Theirs, Repeat, New, Sing }

data class SmartMix(val kind: MixKind, val songs: List<Song>)

/**
 * Why this file exists: "Made for Us" smart mixes and the suggested-song hero. The rules are the
 * ones in the web app's `public/music/library.js` (`mixes()` and `heroSongs()`): computed from real
 * listening data, never invented. Pure Kotlin so it can be tested.
 */
object SmartMixes {
    private const val MOST_PLAYED_LIMIT = 25
    private const val MOST_PLAYED_MIN = 3
    private const val SING_MIN = 3
    private const val HERO_LIMIT = 12
    private const val SECONDS_PER_DAY = 86_400L
    private const val GOLDEN = -7046029254386353131L
    private const val MIX_1 = -4658895280553007687L
    private const val MIX_2 = -7723592293110705685L
    private val FRESH = Duration.ofDays(14)

    /** [shuffle] is a parameter so tests can make the "Our Mix" order predictable. */
    fun build(
        songs: List<Song>,
        now: Instant,
        shuffle: (List<Song>) -> List<Song> = { dailyOrder(it, now) },
    ): List<SmartMix> {
        val all = LibraryQueries.full(songs)
        if (all.isEmpty()) return emptyList()
        val out = mutableListOf(SmartMix(MixKind.Ours, shuffle(all)))

        all.filter { it.liked && it.partnerLiked }.takeIf { it.isNotEmpty() }
            ?.let { out += SmartMix(MixKind.Both, it) }
        all.filter { it.partnerLiked && !it.liked }.takeIf { it.isNotEmpty() }
            ?.let { out += SmartMix(MixKind.Theirs, it) }
        all.filter { it.myPlayCount > 1 }.sortedByDescending { it.myPlayCount }.take(MOST_PLAYED_LIMIT)
            .takeIf { it.size >= MOST_PLAYED_MIN }?.let { out += SmartMix(MixKind.Repeat, it) }
        all.filter { s -> s.createdAt?.let { Duration.between(it, now) < FRESH } == true }
            .takeIf { it.isNotEmpty() }
            ?.let { out += SmartMix(MixKind.New, it.sortedByDescending { s -> s.createdAt }) }
        all.filter { it.hasLyrics }.takeIf { it.size >= SING_MIN }?.let { out += SmartMix(MixKind.Sing, it) }
        return out
    }

    /**
     * A mixed-up order that stays the same all day, so "Our Mix" does not reshuffle every time the
     * library refreshes (for example after each song is counted as listened).
     */
    fun dailyOrder(songs: List<Song>, now: Instant): List<Song> {
        val day = now.epochSecond / SECONDS_PER_DAY
        return songs.sortedBy { mix(it.id.hashCode().toLong() xor (day * GOLDEN)) }
    }

    /** SplitMix64: turns a number into a well-spread one, so a new day gives a completely new order. */
    private fun mix(seed: Long): Long {
        var z = seed + GOLDEN
        z = (z xor (z ushr 30)) * MIX_1
        z = (z xor (z ushr 27)) * MIX_2
        return z xor (z ushr 31)
    }

    /** Up to 12 songs that have artwork, hearted ones first, in a random order otherwise. */
    fun heroPicks(songs: List<Song>, shuffle: (List<Song>) -> List<Song> = { it.shuffled() }): List<Song> {
        val pool = LibraryQueries.full(songs).filter { !it.coverUrl.isNullOrBlank() }
        return shuffle(pool).sortedByDescending { it.liked }.take(HERO_LIMIT)
    }
}
