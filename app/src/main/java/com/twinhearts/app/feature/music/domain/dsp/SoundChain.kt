package com.twinhearts.app.feature.music.domain.dsp

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/** What the sound chain should do right now. Values match the web app's `applyFx()`. */
data class SoundParams(
    val vocalReduce: Boolean = false,
    val normalize: Boolean = false,
    /** 0..1 */
    val reverb: Float = 0f,
    /** 0..1 */
    val echo: Float = 0f,
    /** −10..10 dB, low shelf at 200 Hz */
    val bassDb: Int = 0,
    /** −10..10 dB, high shelf at 4.5 kHz */
    val trebleDb: Int = 0,
) {
    val isIdle: Boolean
        get() = !vocalReduce && !normalize && reverb == 0f && echo == 0f && bassDb == 0 && trebleDb == 0
}

/**
 * Why this file exists: the phone version of the web app's "Sound chain" (public/music/player.js):
 * vocal reduction, bass and treble, volume normalising, reverb and echo, for the Sound effects sheet
 * and karaoke. It works on interleaved 16-bit stereo and has no Android classes, so it is unit tested.
 *
 * Signal flow (same order as the web graph):
 * input → [vocal reduction] → bass → treble → [compressor + make-up] → output, with reverb and echo
 * taken from that signal and added on top.
 */
class SoundChain(private val sampleRate: Int) {

    @Volatile private var params = SoundParams()
    private var applied: SoundParams? = null

    private val hpSide = Biquad()
    private val lpL = Biquad()
    private val lpR = Biquad()
    private val bassL = Biquad()
    private val bassR = Biquad()
    private val trebleL = Biquad()
    private val trebleR = Biquad()

    private val reverbTank = Reverb(sampleRate)
    private val delayL = FloatArray((sampleRate * ECHO_SECONDS).toInt().coerceAtLeast(1))
    private val delayR = FloatArray(delayL.size)
    private var delayPos = 0

    private var envDb = 0.0
    private val attack = exp(-1.0 / (sampleRate * ATTACK_S))
    private val release = exp(-1.0 / (sampleRate * RELEASE_S))

    fun setParams(p: SoundParams) {
        params = p
    }

    val isIdle: Boolean get() = params.isIdle

    /** Forgets reverb tails and filter memory (call after a seek or a new song). */
    fun reset() {
        listOf(hpSide, lpL, lpR, bassL, bassR, trebleL, trebleR).forEach { it.clear() }
        reverbTank.clear()
        delayL.fill(0f)
        delayR.fill(0f)
        delayPos = 0
        envDb = 0.0
    }

    /** Processes [frames] stereo frames of interleaved samples in place. */
    fun process(buf: ShortArray, frames: Int) {
        val p = params
        if (p.isIdle) return
        if (p != applied) {
            configure(p)
            applied = p
        }
        val sendReverb = p.reverb * REVERB_SEND
        val sendEcho = p.echo * ECHO_SEND
        for (i in 0 until frames) {
            var l = buf[2 * i] / SCALE
            var r = buf[2 * i + 1] / SCALE

            if (p.vocalReduce) {
                // (L − R) keeps what differs between the channels and drops the centred vocal;
                // the low end is kept as it was so the bass does not vanish.
                val side = hpSide.process(l - r) * VOCAL_GAIN
                val lowL = lpL.process(l)
                val lowR = lpR.process(r)
                l = side + lowL
                r = side + lowR
            }
            if (p.bassDb != 0) {
                l = bassL.process(l)
                r = bassR.process(r)
            }
            if (p.trebleDb != 0) {
                l = trebleL.process(l)
                r = trebleR.process(r)
            }
            if (p.normalize) {
                val gain = compressorGain(max(abs(l), abs(r)))
                l *= gain
                r *= gain
            }

            var outL = l
            var outR = r
            if (sendReverb > 0f) {
                val wet = reverbTank.process((l + r) * 0.5f) * sendReverb
                outL += wet
                outR += wet
            }
            if (sendEcho > 0f) {
                val dl = delayL[delayPos]
                val dr = delayR[delayPos]
                delayL[delayPos] = l * sendEcho + dl * ECHO_FEEDBACK
                delayR[delayPos] = r * sendEcho + dr * ECHO_FEEDBACK
                delayPos = (delayPos + 1) % delayL.size
                outL += dl
                outR += dr
            }
            buf[2 * i] = toShort(outL)
            buf[2 * i + 1] = toShort(outR)
        }
    }

    private fun configure(p: SoundParams) {
        hpSide.setHighPass(sampleRate, SPLIT_HZ, Q)
        lpL.setLowPass(sampleRate, SPLIT_HZ, Q)
        lpR.setLowPass(sampleRate, SPLIT_HZ, Q)
        bassL.setLowShelf(sampleRate, BASS_HZ, p.bassDb.toDouble())
        bassR.setLowShelf(sampleRate, BASS_HZ, p.bassDb.toDouble())
        trebleL.setHighShelf(sampleRate, TREBLE_HZ, p.trebleDb.toDouble())
        trebleR.setHighShelf(sampleRate, TREBLE_HZ, p.trebleDb.toDouble())
    }

    /** Gentle downward compressor (threshold −24 dB, ratio 4, soft knee) plus ×1.25 make-up, like the web. */
    private fun compressorGain(level: Float): Float {
        val x = 20.0 * log10(max(level.toDouble(), 1e-6))
        val over = x - THRESHOLD_DB
        val y = when {
            2 * over < -KNEE_DB -> x
            2 * abs(over) <= KNEE_DB -> x + (1.0 / RATIO - 1.0) * (over + KNEE_DB / 2).pow(2) / (2 * KNEE_DB)
            else -> THRESHOLD_DB + over / RATIO
        }
        val target = y - x
        val coef = if (target < envDb) attack else release
        envDb = coef * envDb + (1 - coef) * target
        return (10.0.pow(envDb / 20.0) * MAKEUP).toFloat()
    }

    private fun toShort(v: Float): Short = (v.coerceIn(-1f, 1f) * (SCALE - 1)).toInt().toShort()

    private companion object {
        const val SCALE = 32768f
        const val SPLIT_HZ = 150.0
        const val Q = 0.7071
        const val BASS_HZ = 200.0
        const val TREBLE_HZ = 4500.0
        const val VOCAL_GAIN = 1.25f
        const val THRESHOLD_DB = -24.0
        const val RATIO = 4.0
        const val KNEE_DB = 30.0
        const val MAKEUP = 1.25
        const val ATTACK_S = 0.003
        const val RELEASE_S = 0.25
        const val REVERB_SEND = 0.33f
        const val ECHO_SEND = 0.7f
        const val ECHO_FEEDBACK = 0.34f
        const val ECHO_SECONDS = 0.28f
    }
}

/** One biquad filter (RBJ cookbook formulas). */
internal class Biquad {
    private var b0 = 1.0
    private var b1 = 0.0
    private var b2 = 0.0
    private var a1 = 0.0
    private var a2 = 0.0
    private var z1 = 0.0
    private var z2 = 0.0

    fun clear() {
        z1 = 0.0
        z2 = 0.0
    }

    fun process(x: Float): Float {
        val xd = x.toDouble()
        val y = b0 * xd + z1
        z1 = b1 * xd - a1 * y + z2
        z2 = b2 * xd - a2 * y
        return y.toFloat()
    }

    private fun set(nb0: Double, nb1: Double, nb2: Double, a0: Double, na1: Double, na2: Double) {
        b0 = nb0 / a0
        b1 = nb1 / a0
        b2 = nb2 / a0
        a1 = na1 / a0
        a2 = na2 / a0
    }

    fun setLowPass(fs: Int, f0: Double, q: Double) {
        val w = 2 * PI * f0 / fs
        val c = cos(w)
        val alpha = sin(w) / (2 * q)
        set((1 - c) / 2, 1 - c, (1 - c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }

    fun setHighPass(fs: Int, f0: Double, q: Double) {
        val w = 2 * PI * f0 / fs
        val c = cos(w)
        val alpha = sin(w) / (2 * q)
        set((1 + c) / 2, -(1 + c), (1 + c) / 2, 1 + alpha, -2 * c, 1 - alpha)
    }

    fun setLowShelf(fs: Int, f0: Double, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * f0 / fs
        val c = cos(w)
        val beta = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        set(
            a * ((a + 1) - (a - 1) * c + beta),
            2 * a * ((a - 1) - (a + 1) * c),
            a * ((a + 1) - (a - 1) * c - beta),
            (a + 1) + (a - 1) * c + beta,
            -2 * ((a - 1) + (a + 1) * c),
            (a + 1) + (a - 1) * c - beta,
        )
    }

    fun setHighShelf(fs: Int, f0: Double, gainDb: Double) {
        val a = 10.0.pow(gainDb / 40.0)
        val w = 2 * PI * f0 / fs
        val c = cos(w)
        val beta = 2 * sqrt(a) * (sin(w) / 2 * sqrt(2.0))
        set(
            a * ((a + 1) + (a - 1) * c + beta),
            -2 * a * ((a - 1) + (a + 1) * c),
            a * ((a + 1) + (a - 1) * c - beta),
            (a + 1) - (a - 1) * c + beta,
            2 * ((a - 1) - (a + 1) * c),
            (a + 1) - (a - 1) * c - beta,
        )
    }
}

/** A small Schroeder reverb: four comb filters in parallel, then two all-pass filters. */
internal class Reverb(sampleRate: Int) {
    private class Comb(size: Int) {
        val buf = FloatArray(size)
        var pos = 0
        var store = 0f
        fun process(x: Float): Float {
            val out = buf[pos]
            store = out * (1 - DAMP) + store * DAMP
            buf[pos] = x + store * FEEDBACK
            pos = (pos + 1) % buf.size
            return out
        }
    }

    private class AllPass(size: Int) {
        val buf = FloatArray(size)
        var pos = 0
        fun process(x: Float): Float {
            val held = buf[pos]
            val out = -x + held
            buf[pos] = x + held * 0.5f
            pos = (pos + 1) % buf.size
            return out
        }
    }

    private val scale = sampleRate / 44100f
    private val combs = intArrayOf(1116, 1188, 1277, 1356).map { Comb((it * scale).toInt().coerceAtLeast(1)) }
    private val allPass = intArrayOf(556, 441).map { AllPass((it * scale).toInt().coerceAtLeast(1)) }

    fun process(x: Float): Float {
        var sum = 0f
        for (c in combs) sum += c.process(x * INPUT_GAIN)
        for (a in allPass) sum = a.process(sum)
        return sum
    }

    fun clear() {
        combs.forEach { it.buf.fill(0f); it.store = 0f }
        allPass.forEach { it.buf.fill(0f) }
    }

    private companion object {
        const val FEEDBACK = 0.84f
        const val DAMP = 0.2f
        const val INPUT_GAIN = 0.5f
    }
}
