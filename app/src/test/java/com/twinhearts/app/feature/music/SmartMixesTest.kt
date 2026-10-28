package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.domain.MixKind
import com.twinhearts.app.feature.music.domain.Milestone
import com.twinhearts.app.feature.music.domain.Milestones
import com.twinhearts.app.feature.music.domain.SmartMixes
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class SmartMixesTest {
    private val now = Instant.parse("2026-10-28T00:00:00Z")
    private val old = Instant.parse("2026-01-01T00:00:00Z")
    private val fresh = Instant.parse("2026-10-20T00:00:00Z")
    private val keep: (List<com.twinhearts.app.feature.music.domain.model.Song>) -> List<com.twinhearts.app.feature.music.domain.model.Song> = { it }

    @Test
    fun noSongs_noMixes() {
        assertTrue(SmartMixes.build(emptyList(), now, keep).isEmpty())
        // previews are not part of the library
        assertTrue(SmartMixes.build(listOf(song("p", url = "https://x.dzcdn.net/a.mp3")), now, keep).isEmpty())
    }

    @Test
    fun ourMix_isAlwaysFirst_andHoldsEverySong() {
        val mixes = SmartMixes.build(listOf(song("a", created = old), song("b", created = old)), now, keep)
        assertEquals(MixKind.Ours, mixes.first().kind)
        assertEquals(listOf("a", "b"), mixes.first().songs.map { it.id })
        assertEquals(1, mixes.size) // nothing else qualifies
    }

    @Test
    fun lovedByBoth_andPartnersFavourites_splitOnHearts() {
        val both = song("both", liked = true, created = old).copy(partnerLiked = true)
        val theirs = song("theirs", created = old).copy(partnerLiked = true)
        val mine = song("mine", liked = true, created = old)
        val mixes = SmartMixes.build(listOf(both, theirs, mine), now, keep)
        assertEquals(listOf("both"), mixes.first { it.kind == MixKind.Both }.songs.map { it.id })
        assertEquals(listOf("theirs"), mixes.first { it.kind == MixKind.Theirs }.songs.map { it.id })
    }

    @Test
    fun onRepeat_needsThreeSongsPlayedTwiceOrMore_mostPlayedFirst() {
        val two = listOf(song("a", plays = 5, created = old), song("b", plays = 2, created = old), song("c", plays = 1, created = old))
        assertTrue(SmartMixes.build(two, now, keep).none { it.kind == MixKind.Repeat })
        val three = two + song("d", plays = 9, created = old)
        val repeat = SmartMixes.build(three, now, keep).first { it.kind == MixKind.Repeat }
        assertEquals(listOf("d", "a", "b"), repeat.songs.map { it.id })
    }

    @Test
    fun newTogether_isLastTwoWeeks_newestFirst() {
        val a = song("a", created = Instant.parse("2026-10-15T00:00:00Z"))
        val b = song("b", created = Instant.parse("2026-10-25T00:00:00Z"))
        val c = song("c", created = old)
        val fresh = SmartMixes.build(listOf(a, b, c), now, keep).first { it.kind == MixKind.New }
        assertEquals(listOf("b", "a"), fresh.songs.map { it.id })
    }

    @Test
    fun singAlong_needsThreeSongsWithLyrics() {
        val two = listOf(song("a", hasLyrics = true, created = old), song("b", hasLyrics = true, created = old))
        assertTrue(SmartMixes.build(two, now, keep).none { it.kind == MixKind.Sing })
        assertTrue(SmartMixes.build(two + song("c", hasLyrics = true, created = old), now, keep).any { it.kind == MixKind.Sing })
    }

    @Test
    fun hero_usesOnlySongsWithArtwork_heartedFirst_maxTwelve() {
        val noArt = song("none", created = old)
        val plain = (1..15).map { song("s$it", created = old).copy(coverUrl = "https://img/$it.jpg") }
        val loved = song("loved", liked = true, created = old).copy(coverUrl = "https://img/l.jpg")
        val picks = SmartMixes.heroPicks(listOf(noArt) + plain + loved, keep)
        assertEquals(12, picks.size)
        assertEquals("loved", picks.first().id)
        assertFalse(picks.any { it.id == "none" })
    }

    @Test
    fun dailyOrder_isStableWithinADay_andKeepsEverySong() {
        val songs = (1..30).map { song("s$it", created = old) }
        val morning = SmartMixes.dailyOrder(songs, Instant.parse("2026-10-28T06:00:00Z"))
        val evening = SmartMixes.dailyOrder(songs, Instant.parse("2026-10-28T22:00:00Z"))
        val nextDay = SmartMixes.dailyOrder(songs, Instant.parse("2026-10-29T06:00:00Z"))
        assertEquals(morning.map { it.id }, evening.map { it.id })
        assertEquals(songs.map { it.id }.toSet(), morning.map { it.id }.toSet())
        assertFalse(morning.map { it.id } == nextDay.map { it.id })
        // the default "Our Mix" uses it, so it does not reshuffle on every refresh
        val first = SmartMixes.build(songs, now).first().songs.map { it.id }
        assertEquals(first, SmartMixes.build(songs, now).first().songs.map { it.id })
    }

    @Test
    fun milestones_areComputedFromData() {
        assertTrue(Milestones.earned(emptyList(), emptyList(), "user1").isEmpty())
        val songs = listOf(
            song("a", by = "user1", liked = true, hasLyrics = true, plays = 60).copy(partnerLiked = true),
            song("b", by = "user2"),
        )
        val karaoke = recording("r").copy(kind = RecordingKind.Karaoke)
        val got = Milestones.earned(songs, listOf(karaoke), "user1")
        assertTrue(got.containsAll(listOf(
            Milestone.FirstNote, Milestone.Fan, Milestone.OurSoundtrack, Milestone.SameTaste,
            Milestone.Poet, Milestone.OnAir, Milestone.StageStar, Milestone.OnRepeat,
        )))
        assertFalse(Milestone.GrowingCollection in got)
        assertFalse(Milestone.TrueFan in got)
        // a partner's recording is not "On Air" for me
        assertFalse(Milestone.OnAir in Milestones.earned(songs, listOf(recording("r", by = "user2")), "user1"))
    }
}
