package com.twinhearts.app.core.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Random
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin

/**
 * Small, soft sound effects for coins, scratch cards, Spin & Win, rewarded ads, unlocks and purchases.
 * The same sounds as the web app (twin-sounds.js). Made live from sine/triangle waves: no audio files,
 * works offline. Quiet by design (master volume 0.35). One mute switch for everything:
 * [setEnabled] (remembered on this phone). Every call is safe: it silently does nothing when muted,
 * not started, or the phone has no audio. Never throws, never blocks the screen.
 *
 * Start once in Application.onCreate:  TwinSound.init(this)
 */
object TwinSound {
    private const val RATE = 22050
    private const val MASTER = 0.35f
    private const val PREFS = "twin_sound"
    private const val KEY = "enabled"

    @Volatile private var app: Context? = null
    @Volatile private var on = true
    private val enabledState = MutableStateFlow(true)

    /** For the mute button on screen. */
    val enabledFlow: StateFlow<Boolean> = enabledState.asStateFlow()
    val isEnabled: Boolean get() = on

    private val pool = Executors.newScheduledThreadPool(1) { r -> Thread(r, "twin-sound").apply { isDaemon = true } }
    private val cache = ConcurrentHashMap<String, ShortArray>()
    private val lastCue = ConcurrentHashMap<String, Long>()
    private val rnd = Random()
    @Volatile private var lastTick = 0L
    @Volatile private var lastScratch = 0L

    fun init(context: Context) {
        val c = context.applicationContext
        app = c
        on = try { c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true) } catch (_: Exception) { true }
        enabledState.value = on
    }

    fun setEnabled(value: Boolean) {
        on = value
        enabledState.value = value
        try { app?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)?.edit()?.putBoolean(KEY, value)?.apply() } catch (_: Exception) {}
        if (value) tap()
    }

    fun toggle(): Boolean { setEnabled(!on); return on }

    // ── the sounds ───────────────────────────────────────────────

    /** One coin "ting". */
    fun coin() = play("coin") { m -> coinInto(m, 1f, 0f) }

    /** A handful of coins falling (n = how many, 1..10). */
    fun coins(n: Int = 4) {
        if (!ready()) return
        val count = max(1, min(10, n))
        pool.execute {
            try {
                val m = Mix(count * 0.065f + 0.6f)
                for (i in 0 until count) coinInto(m, 0.9f + rnd.nextFloat() * 0.35f, i * 0.065f + rnd.nextFloat() * 0.02f)
                emit(m.pcm())
            } catch (_: Throwable) {}
        }
    }

    /** Soft UI tap. */
    fun tap() = play("tap") { m -> m.tone(SINE, 520f, 380f, 0.07f, 0.18f, 0f, 0.003f) }

    /** Gentle "not now" thud. */
    fun error() = play("error") { m ->
        m.tone(SINE, 220f, 150f, 0.18f, 0.26f, 0f, 0.005f)
        m.tone(SINE, 196f, 130f, 0.22f, 0.20f, 0.09f, 0.005f)
    }

    /** Wheel whoosh. */
    fun spinStart() = play("spinStart") { m ->
        m.burst(0.55f, 0.32f, BAND, 400f, 2600f, 0.9f, 0f)
        m.tone(SINE, 180f, 420f, 0.3f, 0.12f, 0f, 0.02f)
    }

    /** One wheel-peg click. speed 0..1 = softer/slower ... sharper/faster. */
    fun tick(speed: Float = 0.5f) {
        val now = System.nanoTime() / 1_000_000
        if (now - lastTick < 28) return // never a machine-gun
        lastTick = now
        val s = max(0f, min(1f, speed))
        val bucket = (s * 3).toInt() // 0..3, cached
        val sp = bucket / 3f
        play("tick$bucket") { m ->
            m.tone(SQUARE, 1500f + sp * 700f, 900f + sp * 300f, 0.032f, 0.09f + sp * 0.05f, 0f, 0.001f)
            m.burst(0.02f, 0.1f, HIGH, 3000f, 3000f, 0.7f, 0f)
        }
    }

    /** Wheel settles. */
    fun spinEnd() = play("spinEnd") { m ->
        m.tone(SINE, 330f, 330f, 0.25f, 0.16f, 0f, 0.01f)
        m.tone(SINE, 247f, 247f, 0.4f, 0.14f, 0.04f, 0.01f)
    }

    /** One scratch grain (call from the finger-move; it throttles itself). power 0.2..1. */
    fun scratch(power: Float = 0.6f) {
        val now = System.nanoTime() / 1_000_000
        if (now - lastScratch < 55) return
        lastScratch = now
        val variant = rnd.nextInt(4) // 4 cached grains so it does not sound like a loop
        val p = max(0.2f, min(1f, power))
        play("scratch$variant") { m ->
            m.burst(0.07f + p * 0.04f, 0.16f + p * 0.16f, BAND, 2200f + variant * 450f, 1500f + variant * 220f, 1.6f, 0f)
        }
    }

    /** Scratch card revealed. */
    fun reveal() {
        if (!ready()) return
        pool.execute {
            try {
                val m = Mix(1.4f)
                m.burst(0.28f, 0.2f, BAND, 1500f, 6000f, 1.2f, 0f)
                floatArrayOf(784f, 988f, 1175f, 1568f).forEachIndexed { i, f -> m.tone(TRI, f, f, 0.5f, 0.2f, 0.05f + i * 0.07f, 0.006f) }
                for (i in 0 until 5) coinInto(m, 0.9f + rnd.nextFloat() * 0.35f, 0.3f + i * 0.065f)
                emit(m.pcm())
            } catch (_: Throwable) {}
        }
    }

    /** Happy arpeggio + coins. big = longer, for big prizes. */
    fun win(big: Boolean = false) {
        if (!ready()) return
        pool.execute {
            try {
                val notes = if (big) floatArrayOf(523f, 659f, 784f, 1047f, 1319f, 1568f) else floatArrayOf(523f, 659f, 784f, 1047f)
                val coinCount = if (big) 8 else 5
                val m = Mix(notes.size * 0.085f + 0.8f + coinCount * 0.065f)
                notes.forEachIndexed { i, f ->
                    m.tone(TRI, f, f, 0.45f, 0.24f, i * 0.085f, 0.006f)
                    m.tone(SINE, f * 2, f * 2, 0.35f, 0.06f, i * 0.085f, 0.006f)
                }
                for (i in 0 until coinCount) coinInto(m, 0.9f + rnd.nextFloat() * 0.35f, 0.15f + i * 0.065f)
                emit(m.pcm())
            } catch (_: Throwable) {}
        }
    }

    /** Rewarded-ad payout: rising chime, then coins. */
    fun ad() {
        if (!ready()) return
        pool.execute {
            try {
                val m = Mix(1.6f)
                floatArrayOf(659f, 880f, 1109f).forEachIndexed { i, f -> m.tone(TRI, f, f, 0.32f, 0.22f, i * 0.1f, 0.006f) }
                for (i in 0 until 6) coinInto(m, 0.9f + rnd.nextFloat() * 0.35f, 0.3f + i * 0.065f)
                emit(m.pcm())
            } catch (_: Throwable) {}
        }
    }

    /** A pass / item unlocked: soft click, then a bright major chord. */
    fun unlock() = play("unlock") { m ->
        m.burst(0.05f, 0.22f, BAND, 1800f, 900f, 2f, 0f)
        floatArrayOf(523f, 659f, 784f).forEach { f -> m.tone(TRI, f, f, 0.7f, 0.2f, 0.08f, 0.01f) }
        m.tone(SINE, 1568f, 1568f, 0.6f, 0.08f, 0.16f, 0.01f)
    }

    /** "Ka-ching". */
    fun purchase() = play("purchase") { m ->
        m.burst(0.06f, 0.25f, HIGH, 2500f, 2500f, 0.8f, 0f)
        m.tone(TRI, 1568f, 1568f, 0.45f, 0.28f, 0.06f, 0.003f)
        m.tone(TRI, 2093f, 2093f, 0.6f, 0.26f, 0.15f, 0.003f)
        m.tone(SINE, 4186f, 4186f, 0.4f, 0.06f, 0.15f, 0.003f)
    }


    // ── chat + call sounds (same cues as the web app's twin-sounds.js v2) ──────

    /** Message sent: soft rising pop. */
    fun msgSent() = cue("msgSent") { m -> m.tone(SINE, 520f, 760f, 0.09f, 0.20f, 0f, 0.004f) }

    /** Message received: two-note bloop. */
    fun msgReceived() = cue("msgReceived") { m ->
        m.tone(SINE, 880f, 880f, 0.12f, 0.18f, 0f, 0.004f)
        m.tone(SINE, 1175f, 1175f, 0.20f, 0.16f, 0.09f, 0.004f)
    }

    /** Photo / video / file sent: a swish, then the pop. */
    fun mediaSent() = cue("mediaSent") { m ->
        m.burst(0.12f, 0.13f, BAND, 900f, 3200f, 0.9f, 0f)
        m.tone(SINE, 520f, 760f, 0.09f, 0.18f, 0.07f, 0.004f)
    }

    /** Sticker / GIF sent: playful two-note boing. */
    fun funSent() = cue("funSent") { m ->
        m.tone(SINE, 600f, 900f, 0.09f, 0.20f, 0f, 0.004f)
        m.tone(SINE, 900f, 1300f, 0.11f, 0.18f, 0.08f, 0.004f)
    }

    /** Voice recording starts: rising blip. */
    fun voiceStart() = cue("voiceStart") { m -> m.tone(SINE, 440f, 660f, 0.10f, 0.20f, 0f, 0.004f) }

    /** Voice recording stops / is sent: falling blip. */
    fun voiceStop() = cue("voiceStop") { m -> m.tone(SINE, 660f, 440f, 0.10f, 0.20f, 0f, 0.004f) }

    /** Voice recording thrown away. */
    fun voiceCancel() = cue("voiceCancel") { m -> m.tone(SINE, 400f, 180f, 0.16f, 0.20f, 0f, 0.004f) }

    /** A reaction was added: bright sparkle. */
    fun react() = cue("react") { m ->
        m.tone(TRI, 1568f, 2093f, 0.12f, 0.18f, 0f, 0.004f)
        m.tone(SINE, 3136f, 3136f, 0.12f, 0.05f, 0.03f, 0.004f)
    }

    /** Gift / hug / kiss: warm, rounded heart chime. */
    fun heart() = cue("heart") { m ->
        floatArrayOf(523f, 659f, 784f).forEachIndexed { i, f -> m.tone(SINE, f, f, 0.5f, 0.20f, i * 0.11f, 0.01f) }
        m.tone(SINE, 1047f, 1047f, 0.55f, 0.07f, 0.24f, 0.01f)
    }

    /** Message starred: two-note ding. */
    fun star() = cue("star") { m ->
        m.tone(TRI, 1175f, 1175f, 0.14f, 0.20f, 0f, 0.004f)
        m.tone(TRI, 1568f, 1568f, 0.24f, 0.20f, 0.07f, 0.004f)
    }

    /** Message pinned: soft tock. */
    fun pin() = cue("pin") { m ->
        m.tone(SINE, 700f, 500f, 0.06f, 0.22f, 0f, 0.002f)
        m.tone(SINE, 500f, 350f, 0.09f, 0.18f, 0.05f, 0.002f)
    }

    /** Message deleted: downward whoosh. */
    fun deleted() = cue("deleted") { m -> m.burst(0.14f, 0.14f, BAND, 2400f, 500f, 0.9f, 0f) }

    /** Call connected: three rising notes. */
    fun callConnected() = cue("callConnected") { m ->
        floatArrayOf(659f, 880f, 1109f).forEachIndexed { i, f -> m.tone(SINE, f, f, 0.16f, 0.18f, i * 0.09f, 0.006f) }
    }

    /** Call ended: three falling notes. */
    fun callEnded() = cue("callEnded") { m ->
        floatArrayOf(880f, 659f, 440f).forEachIndexed { i, f -> m.tone(SINE, f, f, 0.18f, 0.18f, i * 0.10f, 0.006f) }
    }

    /** Mic / camera / speaker switched on. */
    fun switchOn() = cue("switchOn") { m -> m.tone(SINE, 600f, 850f, 0.07f, 0.18f, 0f, 0.004f) }

    /** Mic / camera / speaker switched off. */
    fun switchOff() = cue("switchOff") { m -> m.tone(SINE, 850f, 600f, 0.07f, 0.18f, 0f, 0.004f) }

    // ── game sounds (same five cues as the web app's twin-sounds.js v3; the bomb countdown reuses tick) ──

    /** Memory card turned over: tiny rising tick. */
    fun flip() = cue("flip") { m -> m.tone(TRI, 620f, 900f, 0.05f, 0.15f, 0f, 0.003f) }

    /** Pair found / mind match: two-note chime. */
    fun match() = cue("match") { m ->
        m.tone(TRI, 988f, 988f, 0.12f, 0.18f, 0f, 0.004f)
        m.tone(TRI, 1319f, 1319f, 0.20f, 0.18f, 0.09f, 0.004f)
    }

    /** Puzzle piece locks in: short tock plus a click. */
    fun snap() = cue("snap") { m ->
        m.tone(SINE, 880f, 560f, 0.05f, 0.20f, 0f, 0.002f)
        m.burst(0.03f, 0.08f, HIGH, 3000f, 3000f, 0.7f, 0f)
    }

    /** Bomb goes off: low thump and a short rumble. */
    fun boom() = cue("boom") { m ->
        m.tone(SINE, 150f, 45f, 0.45f, 0.30f, 0f, 0.004f)
        m.burst(0.30f, 0.20f, BAND, 900f, 150f, 0.8f, 0f)
    }

    /** Dice roll: quick rattle, then a soft thud. */
    fun dice() = cue("dice") { m ->
        floatArrayOf(0f, 0.06f, 0.12f, 0.19f).forEachIndexed { i, d ->
            m.tone(SQUARE, 1400f + i * 160f, 1400f + i * 160f, 0.03f, 0.07f, d, 0.001f)
        }
        m.tone(SINE, 220f, 150f, 0.09f, 0.18f, 0.26f, 0.004f)
    }

    // ── engine ───────────────────────────────────────────────────

    private fun ready() = on && app != null

    private fun coinInto(m: Mix, k: Float, d: Float) {
        m.tone(TRI, 1318f * k, 1318f * k, 0.16f, 0.34f, d, 0.004f)          // E6 "ting"
        m.tone(TRI, 1976f * k, 1976f * k, 0.34f, 0.30f, d + 0.075f, 0.004f) // B6 ring
        m.tone(SINE, 3951f * k, 3951f * k, 0.22f, 0.07f, d + 0.075f, 0.004f) // sparkle
    }

    /** Like [play], but the same cue never fires twice within 120 ms (bulk star / delete, double events). */
    private fun cue(key: String, build: (Mix) -> Unit) {
        val now = System.nanoTime() / 1_000_000
        val prev = lastCue[key]
        if (prev != null && now - prev < 120) return
        lastCue[key] = now
        play(key, build)
    }

    /** Builds (once, then cached) and plays a fixed sound on the sound thread. */
    private fun play(key: String, build: (Mix) -> Unit) {
        if (!ready()) return
        pool.execute {
            try {
                val pcm = cache.getOrPut(key) { val m = Mix(1.4f); build(m); m.pcm() }
                emit(pcm)
            } catch (_: Throwable) {}
        }
    }

    private fun emit(pcm: ShortArray) {
        if (!on || pcm.isEmpty()) return
        try {
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setSampleRate(RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setBufferSizeInBytes(pcm.size * 2)
                .setTransferMode(AudioTrack.MODE_STATIC)
                .build()
            track.write(pcm, 0, pcm.size)
            track.play()
            val ms = pcm.size * 1000L / RATE + 200
            pool.schedule(Runnable { try { track.release() } catch (_: Throwable) {} }, ms, TimeUnit.MILLISECONDS)
        } catch (_: Throwable) {
            // no audio on this phone: stay silent
        }
    }

    private const val SINE = 0
    private const val TRI = 1
    private const val SQUARE = 2
    private const val BAND = 0
    private const val HIGH = 1

    /** A little mixing buffer: tones and filtered-noise bursts are added in, then turned into 16-bit sound. */
    private class Mix(seconds: Float) {
        private val n = (seconds * RATE).toInt() + 1
        private val buf = FloatArray(n)
        private val noise = Random()

        fun tone(shape: Int, f0: Float, f1: Float, len: Float, vol: Float, delay: Float, attack: Float) {
            val start = (delay * RATE).toInt()
            val count = (len * RATE).toInt()
            var phase = 0.0
            for (i in 0 until count) {
                val idx = start + i
                if (idx >= n) break
                val t = i / RATE.toFloat()
                val p = i.toFloat() / count
                val f = if (f1 != f0) f0 * (f1 / f0).pow(p) else f0
                phase += 2.0 * PI * f / RATE
                val s = when (shape) {
                    TRI -> (2.0 / PI * asin(sin(phase))).toFloat()
                    SQUARE -> if (sin(phase) >= 0) 1f else -1f
                    else -> sin(phase).toFloat()
                }
                buf[idx] += s * envelope(t, len, vol, attack)
            }
        }

        /** Noise through a band-pass or high-pass filter whose centre glides f0 -> f1. */
        fun burst(len: Float, vol: Float, type: Int, f0: Float, f1: Float, q: Float, delay: Float) {
            val start = (delay * RATE).toInt()
            val count = (len * RATE).toInt()
            var low = 0.0
            var band = 0.0
            val damp = 1.0 / max(0.3f, q)
            val attack = min(0.012f, len / 3)
            for (i in 0 until count) {
                val idx = start + i
                if (idx >= n) break
                val p = i.toFloat() / count
                val fc = if (f1 != f0) f0 * (f1 / f0).pow(p) else f0
                val f = min(1.2, 2.0 * sin(PI * fc / RATE))
                val input = noise.nextFloat() * 2.0 - 1.0
                low += f * band
                val high = input - low - damp * band
                band += f * high
                val out = if (type == HIGH) high else band
                buf[idx] += (out * 0.6).toFloat() * envelope(i / RATE.toFloat(), len, vol, attack)
            }
        }

        /** Quick rise, then a smooth fall to silence (like the web's exponential ramps). */
        private fun envelope(t: Float, len: Float, vol: Float, attack: Float): Float {
            if (t < attack) return vol * (t / attack)
            val x = ((t - attack) / max(0.001f, len - attack)).coerceIn(0f, 1f)
            return vol * (0.0001f / vol).pow(x)
        }

        fun pcm(): ShortArray {
            val out = ShortArray(n)
            for (i in 0 until n) {
                val v = (buf[i] * MASTER).coerceIn(-1f, 1f) // master volume; hard limit so stacked coins never clip
                out[i] = (v * 32000f).toInt().toShort()
            }
            return out
        }
    }
}
