package com.twinhearts.app.feature.cinema3d.domain

/** Frame timing over the last window (about one second). */
data class FrameStats(
    val fps: Float = 0f,
    val averageMs: Float = 0f,
    val worstMs: Float = 0f,
)

/**
 * Turns frame timestamps (nanoseconds, from Choreographer) into [FrameStats].
 * Call [onFrame] once per rendered frame; read [latest] whenever you like.
 */
class FrameStatsTracker(private val windowNanos: Long = 1_000_000_000L) {

    var latest: FrameStats = FrameStats()
        private set

    private var lastNanos = 0L
    private var windowStartNanos = 0L
    private var frames = 0
    private var worstNanos = 0L

    fun onFrame(nowNanos: Long) {
        if (lastNanos == 0L) {
            windowStartNanos = nowNanos
        } else {
            val delta = nowNanos - lastNanos
            if (delta > worstNanos) worstNanos = delta
            frames++
        }
        lastNanos = nowNanos

        val elapsed = nowNanos - windowStartNanos
        if (frames > 0 && elapsed >= windowNanos) {
            latest = FrameStats(
                fps = (frames * 1_000_000_000.0 / elapsed).toFloat(),
                averageMs = (elapsed / 1_000_000.0 / frames).toFloat(),
                worstMs = (worstNanos / 1_000_000.0).toFloat(),
            )
            windowStartNanos = nowNanos
            frames = 0
            worstNanos = 0L
        }
    }

    /** Forget everything, e.g. after the app was paused (the gap would show up as one huge frame). */
    fun reset() {
        lastNanos = 0L
        windowStartNanos = 0L
        frames = 0
        worstNanos = 0L
    }
}
