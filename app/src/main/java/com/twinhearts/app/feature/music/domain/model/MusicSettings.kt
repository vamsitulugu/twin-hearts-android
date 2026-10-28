package com.twinhearts.app.feature.music.domain.model

/** The four sliders of "Sound effects". Ranges match the web app: reverb/echo 0..1, bass/treble −10..10 dB. */
data class SoundFx(
    val reverb: Float = 0f,
    val echo: Float = 0f,
    val bass: Int = 0,
    val treble: Int = 0,
) {
    val isFlat: Boolean get() = reverb == 0f && echo == 0f && bass == 0 && treble == 0
}

/** The ready-made looks in "Sound effects" (same values as the web `PRESETS`). */
enum class FxPreset(val fx: SoundFx?) {
    Flat(SoundFx()),
    Studio(SoundFx(reverb = 0.18f, bass = 2, treble = 2)),
    Bathroom(SoundFx(reverb = 0.55f, echo = 0.12f, bass = 1, treble = 3)),
    Stadium(SoundFx(reverb = 0.8f, echo = 0.3f, bass = 3, treble = 1)),
    BassBoost(SoundFx(bass = 8, treble = -1)),
    Bright(SoundFx(reverb = 0.1f, bass = -2, treble = 6)),

    /** Set automatically when a slider is moved by hand. */
    Custom(null),
}

/** Phone-side Music settings. Defaults match `DEFAULTS` in the web app's `public/music/core.js`. */
data class MusicSettings(
    /** Seconds of fade between two songs. 0 = off. */
    val crossfadeSec: Int = 0,
    val autoLyrics: Boolean = true,
    val normalize: Boolean = false,
    val vocalReduce: Boolean = false,
    val preset: FxPreset = FxPreset.Flat,
    val fx: SoundFx = SoundFx(),
) {
    /** `true` when the player must run its sound chain at all. */
    val needsSoundChain: Boolean get() = normalize || vocalReduce || !fx.isFlat
}
