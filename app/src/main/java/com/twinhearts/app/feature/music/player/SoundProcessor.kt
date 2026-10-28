package com.twinhearts.app.feature.music.player

import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import com.twinhearts.app.feature.music.domain.dsp.SoundChain
import com.twinhearts.app.feature.music.domain.dsp.SoundParams
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Why this file exists: plugs the [SoundChain] (bass, treble, reverb, echo, vocal reduction, volume
 * normalising) into ExoPlayer's audio path. It only touches 16-bit stereo; any other format, and any
 * time all effects are off, passes straight through untouched.
 */
@UnstableApi
class SoundProcessor : BaseAudioProcessor() {

    @Volatile private var params = SoundParams()
    private var chain: SoundChain? = null
    private var scratch = ShortArray(0)

    fun setParams(p: SoundParams) {
        params = p
        chain?.setParams(p)
    }

    override fun onConfigure(inputAudioFormat: AudioProcessor.AudioFormat): AudioProcessor.AudioFormat {
        val stereo16 = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT && inputAudioFormat.channelCount == 2
        chain = if (stereo16) SoundChain(inputAudioFormat.sampleRate).also { it.setParams(params) } else null
        // Always "active" with the same format, so effects can be switched on and off while a song plays.
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val size = inputBuffer.remaining()
        if (size == 0) return
        val out = replaceOutputBuffer(size)
        val c = chain
        if (c == null || c.isIdle) {
            out.put(inputBuffer)
        } else {
            val shorts = size / 2
            if (scratch.size < shorts) scratch = ShortArray(shorts)
            val order = inputBuffer.order()
            inputBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(scratch, 0, shorts)
            inputBuffer.order(order)
            inputBuffer.position(inputBuffer.limit())
            c.process(scratch, shorts / 2)
            out.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().put(scratch, 0, shorts)
            out.position(out.position() + size)
        }
        out.flip()
    }

    override fun onFlush() {
        chain?.reset()
    }

    override fun onReset() {
        chain = null
    }
}
