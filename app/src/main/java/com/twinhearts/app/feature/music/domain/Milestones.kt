package com.twinhearts.app.feature.music.domain

import com.twinhearts.app.feature.music.domain.model.Recording
import com.twinhearts.app.feature.music.domain.model.RecordingKind
import com.twinhearts.app.feature.music.domain.model.Song

enum class Milestone(val emoji: String) {
    FirstNote("🎵"), GrowingCollection("🎶"), MusicLover("🎸"), Fan("❤️"), TrueFan("💕"),
    OurSoundtrack("👫"), SameTaste("💞"), Poet("📜"), OnAir("🎙"), StageStar("🎤"), OnRepeat("🔥"),
}

/**
 * Why this file exists: the achievements on the Music settings page. Same rules as `milestones()` in
 * the web app's `public/music/settings.js`, computed live from the data so nothing is stored.
 */
object Milestones {
    private const val GROWING = 5
    private const val LOVER = 10
    private const val TRUE_FAN = 5
    private const val ON_REPEAT_PLAYS = 50

    fun earned(songs: List<Song>, recordings: List<Recording>, myRole: String): Set<Milestone> {
        val full = songs.filterNot { it.isPreview }
        val mine = full.count { it.uploadedBy == myRole }
        val partner = full.size - mine
        val out = mutableSetOf<Milestone>()
        if (songs.isNotEmpty()) out += Milestone.FirstNote
        if (songs.size >= GROWING) out += Milestone.GrowingCollection
        if (songs.size >= LOVER) out += Milestone.MusicLover
        if (songs.any { it.liked }) out += Milestone.Fan
        if (songs.count { it.liked } >= TRUE_FAN) out += Milestone.TrueFan
        if (mine >= 1 && partner >= 1) out += Milestone.OurSoundtrack
        if (songs.any { it.liked && it.partnerLiked }) out += Milestone.SameTaste
        if (songs.any { it.hasLyrics }) out += Milestone.Poet
        if (recordings.any { it.userRole == myRole }) out += Milestone.OnAir
        if (recordings.any { it.kind == RecordingKind.Karaoke && it.userRole == myRole }) out += Milestone.StageStar
        if (songs.sumOf { it.myPlayCount } >= ON_REPEAT_PLAYS) out += Milestone.OnRepeat
        return out
    }
}
