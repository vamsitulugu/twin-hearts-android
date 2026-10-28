package com.twinhearts.app.feature.music.player

import com.twinhearts.app.feature.music.domain.model.Song

/** Why this file exists: what the UI may know about playback, with no Media3 types in it. */
enum class RepeatKind { Off, All, One }

data class PlayerState(
    /** The queue in the order it will play (after shuffle, if shuffle is on). */
    val queue: List<Song> = emptyList(),
    val current: Song? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffle: Boolean = false,
    val repeat: RepeatKind = RepeatKind.Off,
    /** Wall-clock time (ms) when the sleep timer will pause playback; null when it is off. */
    val sleepAtMs: Long? = null,
) {
    val hasSong: Boolean get() = current != null
}
