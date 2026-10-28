package com.twinhearts.app.feature.music

import com.twinhearts.app.feature.music.domain.dsp.SoundChain
import com.twinhearts.app.feature.music.domain.dsp.SoundParams
import com.twinhearts.app.feature.music.domain.model.FxPreset
import com.twinhearts.app.feature.music.domain.model.MusicSettings
import com.twinhearts.app.feature.music.domain.model.SoundFx
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

class SoundChainTest {
    private val rate = 44100

    private fun tone(hz: Double, leftGain: Double, rightGain: Double, seconds: Double = 1.0, amp: Double = 8000.0): ShortArray {
        val frames = (rate * seconds).toInt()
        val out = ShortArray(frames * 2)
        for (i in 0 until frames) {
            val v = sin(2 * PI * hz * i / rate) * amp
            out[2 * i] = (v * leftGain).toInt().toShort()
            out[2 * i + 1] = (v * rightGain).toInt().toShort()
        }
        return out
    }

    private fun rms(buf: ShortArray, channel: Int = 0, from: Int = rate / 2): Double {
        var sum = 0.0
        var n = 0
        var i = from
        while (i < buf.size / 2) {
            val v = buf[2 * i + channel].toDouble()
            sum += v * v
            n++
            i++
        }
        return sqrt(sum / n)
    }

    private fun run(params: SoundParams, input: ShortArray): ShortArray {
        val chain = SoundChain(rate)
        chain.setParams(params)
        val copy = input.copyOf()
        chain.process(copy, copy.size / 2)
        return copy
    }

    @Test
    fun flat_changesNothing() {
        val input = tone(440.0, 1.0, 1.0)
        assertTrue(SoundChain(rate).isIdle)
        assertArrayEquals(input, run(SoundParams(), input))
    }

    @Test
    fun vocalReduction_removesCentredSound_keepsSideSound() {
        val centred = tone(1000.0, 1.0, 1.0)
        val sided = tone(1000.0, 1.0, -1.0)
        val cutCentre = rms(run(SoundParams(vocalReduce = true), centred))
        val keepSide = rms(run(SoundParams(vocalReduce = true), sided))
        assertTrue("centre should vanish, was $cutCentre", cutCentre < rms(centred) * 0.05)
        assertTrue("side should stay loud, was $keepSide", keepSide > rms(sided) * 0.8)
    }

    @Test
    fun vocalReduction_keepsTheBass() {
        val bass = tone(60.0, 1.0, 1.0)
        assertTrue(rms(run(SoundParams(vocalReduce = true), bass)) > rms(bass) * 0.6)
    }

    @Test
    fun bassBoost_raisesLowTones_notHighTones() {
        val low = tone(60.0, 1.0, 1.0)
        val high = tone(8000.0, 1.0, 1.0)
        val boosted = SoundParams(bassDb = 8)
        assertTrue(rms(run(boosted, low)) > rms(low) * 1.8)
        assertEquals(rms(high), rms(run(boosted, high)), rms(high) * 0.05)
    }

    @Test
    fun trebleCut_lowersHighTones() {
        val high = tone(10000.0, 1.0, 1.0)
        assertTrue(rms(run(SoundParams(trebleDb = -8), high)) < rms(high) * 0.6)
    }

    @Test
    fun normalise_lowersLoudSounds_relativeToQuietOnes() {
        val loud = tone(440.0, 1.0, 1.0, amp = 24000.0)
        val quiet = tone(440.0, 1.0, 1.0, amp = 1500.0)
        val p = SoundParams(normalize = true)
        val loudGain = rms(run(p, loud)) / rms(loud)
        val quietGain = rms(run(p, quiet)) / rms(quiet)
        assertTrue("loud $loudGain should be reduced more than quiet $quietGain", loudGain < quietGain)
    }

    @Test
    fun echo_putsEnergyAfterTheSoundEnds() {
        val burst = ShortArray(rate * 2)
        for (i in 0 until 2000) { burst[2 * i] = 12000; burst[2 * i + 1] = 12000 }
        val out = run(SoundParams(echo = 1f), burst)
        // the repeat arrives 0.28 s later, well after the 2000-frame burst
        val start = (rate * 0.28).toInt()
        var peak = 0
        for (i in start until start + 2000) peak = maxOf(peak, abs(out[2 * i].toInt()))
        assertTrue("echo peak $peak", peak > 2000)
        assertEquals(0, abs(out[2 * 5000].toInt()))
    }

    @Test
    fun reverb_leavesATailAfterTheSoundEnds() {
        val burst = ShortArray(rate * 2)
        for (i in 0 until 2000) { burst[2 * i] = 12000; burst[2 * i + 1] = 12000 }
        val out = run(SoundParams(reverb = 1f), burst)
        var tail = 0.0
        for (i in 4000 until 20000) tail += abs(out[2 * i].toDouble())
        assertTrue("tail $tail", tail > 1000.0)
    }

    @Test
    fun neverClipsOrBlowsUp_withEverythingOn() {
        val loud = tone(200.0, 1.0, 0.5, seconds = 2.0, amp = 32000.0)
        val out = run(SoundParams(vocalReduce = true, normalize = true, reverb = 1f, echo = 1f, bassDb = 10, trebleDb = 10), loud)
        assertTrue(out.all { it in Short.MIN_VALUE..Short.MAX_VALUE })
        assertTrue(rms(out) < 33000)
    }

    @Test
    fun settings_decideWhenTheChainRuns() {
        assertFalse(MusicSettings().needsSoundChain)
        assertTrue(MusicSettings(normalize = true).needsSoundChain)
        assertTrue(MusicSettings(vocalReduce = true).needsSoundChain)
        assertTrue(MusicSettings(fx = SoundFx(bass = 3)).needsSoundChain)
        assertTrue(FxPreset.Flat.fx!!.isFlat)
        assertFalse(FxPreset.Stadium.fx!!.isFlat)
        assertEquals(null, FxPreset.Custom.fx)
    }
}
