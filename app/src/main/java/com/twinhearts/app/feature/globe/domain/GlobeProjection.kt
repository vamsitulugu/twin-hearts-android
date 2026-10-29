package com.twinhearts.app.feature.globe.domain

import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.acos

/*
 * Why this file exists: the maths of the 3D globe, with no Android code in it.
 * The globe is drawn as an orthographic ball: every point on Earth is turned into a unit vector,
 * turned to face the camera, and the part facing us is flattened onto the screen.
 * Keeping this pure makes it testable and keeps the drawing code short.
 */

/** Where the camera looks: the point in the middle of the ball, and how close it is (1 = whole ball). */
data class GlobeCamera(val lat: Double, val lon: Double, val zoom: Double) {
    fun normalized(): GlobeCamera =
        GlobeCamera(lat.coerceIn(-MAX_LAT, MAX_LAT), wrapLongitude(lon), zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))

    companion object {
        const val MIN_ZOOM = 0.85
        const val MAX_ZOOM = 9.0
        const val MAX_LAT = 85.0

        /** What "Reset view" returns to. */
        val Default = GlobeCamera(15.0, 0.0, 1.0)
    }
}

/** Longitude in -180..180. */
fun wrapLongitude(lon: Double): Double {
    var x = (lon + 180.0) % 360.0
    if (x < 0) x += 360.0
    return x - 180.0
}

/** A reusable 3-number holder, so drawing a frame does not create garbage. */
class Vec3(var x: Float = 0f, var y: Float = 0f, var z: Float = 0f)

/**
 * The camera turned into three axes. A point with unit vector (px,py,pz) lands at screen-plane
 * (x, y) in -1..1, and is on the visible side of the ball when z > 0.
 */
class GlobeView(latDeg: Double, lonDeg: Double) {
    private val rx: Float
    private val ry: Float
    private val ux: Float
    private val uy: Float
    private val uz: Float
    private val fx: Float
    private val fy: Float
    private val fz: Float

    init {
        val phi = Math.toRadians(latDeg)
        val lam = Math.toRadians(lonDeg)
        rx = (-sin(lam)).toFloat()
        ry = cos(lam).toFloat()
        ux = (-sin(phi) * cos(lam)).toFloat()
        uy = (-sin(phi) * sin(lam)).toFloat()
        uz = cos(phi).toFloat()
        fx = (cos(phi) * cos(lam)).toFloat()
        fy = (cos(phi) * sin(lam)).toFloat()
        fz = sin(phi).toFloat()
    }

    fun x(px: Float, py: Float) = px * rx + py * ry
    fun y(px: Float, py: Float, pz: Float) = px * ux + py * uy + pz * uz
    fun z(px: Float, py: Float, pz: Float) = px * fx + py * fy + pz * fz

    /** Projects a latitude / longitude into [out]. */
    fun project(latDeg: Double, lonDeg: Double, out: Vec3) {
        val phi = Math.toRadians(latDeg)
        val lam = Math.toRadians(lonDeg)
        val px = (cos(phi) * cos(lam)).toFloat()
        val py = (cos(phi) * sin(lam)).toFloat()
        val pz = sin(phi).toFloat()
        out.x = x(px, py)
        out.y = y(px, py, pz)
        out.z = z(px, py, pz)
    }
}

/** A closed outline (one piece of a country) stored as unit vectors, ready to be turned every frame. */
class GeoRing(val ux: FloatArray, val uy: FloatArray, val uz: FloatArray) {
    val size: Int get() = ux.size

    companion object {
        fun fromDegrees(lon: FloatArray, lat: FloatArray): GeoRing {
            val n = lon.size
            val x = FloatArray(n)
            val y = FloatArray(n)
            val z = FloatArray(n)
            for (i in 0 until n) {
                val phi = Math.toRadians(lat[i].toDouble())
                val lam = Math.toRadians(lon[i].toDouble())
                x[i] = (cos(phi) * cos(lam)).toFloat()
                y[i] = (cos(phi) * sin(lam)).toFloat()
                z[i] = sin(phi).toFloat()
            }
            return GeoRing(x, y, z)
        }
    }
}

/**
 * Cuts one outline at the horizon. The result is a flat polygon in -1..1 units: the part of the
 * outline facing us, closed along the edge of the ball. Buffers are reused between calls.
 */
class RingClipper {
    var xs = FloatArray(2048)
        private set
    var ys = FloatArray(2048)
        private set
    var count = 0
        private set

    private var bx = FloatArray(0)
    private var by = FloatArray(0)
    private var bz = FloatArray(0)

    private fun add(x: Float, y: Float) {
        if (count == xs.size) {
            xs = xs.copyOf(count * 2)
            ys = ys.copyOf(count * 2)
        }
        xs[count] = x
        ys[count] = y
        count++
    }

    /** Returns how many points were produced (0 when the whole outline is on the far side). */
    fun clip(ring: GeoRing, view: GlobeView): Int {
        val n = ring.size
        count = 0
        if (n < 3) return 0
        if (bx.size < n) {
            bx = FloatArray(n)
            by = FloatArray(n)
            bz = FloatArray(n)
        }
        var front = 0
        for (i in 0 until n) {
            val px = ring.ux[i]
            val py = ring.uy[i]
            val pz = ring.uz[i]
            bx[i] = view.x(px, py)
            by[i] = view.y(px, py, pz)
            bz[i] = view.z(px, py, pz)
            if (bz[i] > 0f) front++
        }
        if (front == 0) return 0
        if (front == n) {
            for (i in 0 until n) add(bx[i], by[i])
            return count
        }
        var start = -1
        for (i in 0 until n) {
            if (bz[i] > 0f && bz[(i + n - 1) % n] <= 0f) {
                start = i
                break
            }
        }
        if (start < 0) return 0
        val firstPrev = (start + n - 1) % n
        val enter = crossing(firstPrev, start)
        add(enter[0], enter[1])
        val firstAngle = atan2(enter[1], enter[0])
        add(bx[start], by[start])
        var prevFront = true
        var prevIdx = start
        var exitAngle = 0f
        for (k in 1 until n) {
            val i = (start + k) % n
            val isFront = bz[i] > 0f
            if (isFront) {
                if (!prevFront) {
                    val c = crossing(prevIdx, i)
                    limbArc(exitAngle, atan2(c[1], c[0]))
                    add(c[0], c[1])
                }
                add(bx[i], by[i])
            } else if (prevFront) {
                val c = crossing(prevIdx, i)
                add(c[0], c[1])
                exitAngle = atan2(c[1], c[0])
            }
            prevFront = isFront
            prevIdx = i
        }
        limbArc(exitAngle, firstAngle)
        return count
    }

    private val tmp = FloatArray(2)

    /** The point on the horizon between a front and a back vertex. */
    private fun crossing(a: Int, b: Int): FloatArray {
        val za = bz[a]
        val zb = bz[b]
        val denom = za - zb
        val t = if (denom == 0f) 0.5f else za / denom
        val x = bx[a] + t * (bx[b] - bx[a])
        val y = by[a] + t * (by[b] - by[a])
        val len = hypot(x, y)
        if (len < 1e-6f) {
            tmp[0] = 1f
            tmp[1] = 0f
        } else {
            tmp[0] = x / len
            tmp[1] = y / len
        }
        return tmp
    }

    /** Follows the edge of the ball the short way from one angle to another. */
    private fun limbArc(from: Float, to: Float) {
        var delta = to - from
        while (delta > PI) delta -= (2 * PI).toFloat()
        while (delta < -PI) delta += (2 * PI).toFloat()
        val steps = max(1, ceil(abs(delta) / (PI / 18)).toInt())
        for (i in 1 until steps) {
            val a = from + delta * i / steps
            add(cos(a), sin(a))
        }
    }
}

// ── camera flights ───────────────────────────────────────────────────────────────────────────

/** A smooth move of the camera from one place to another. */
data class Flight(val from: GlobeCamera, val to: GlobeCamera, val durationMs: Long) {
    /** The camera [elapsedMs] after the start. */
    fun at(elapsedMs: Long): GlobeCamera {
        val t = (elapsedMs.toDouble() / max(durationMs, 1L)).coerceIn(0.0, 1.0)
        if (t >= 1.0) return to.normalized()
        val e = if (t < 0.5) 2 * t * t else 1 - (-2 * t + 2).let { it * it } / 2
        var dLon = to.lon - from.lon
        while (dLon > 180) dLon -= 360
        while (dLon < -180) dLon += 360
        val distance = angularDistance(from, to)
        // Long trips pull back a little in the middle, like the web app's flyTo, so you see where you are going.
        val dip = 1.0 + 0.8 * sin(PI * t) * min(1.0, distance / (PI / 2))
        val zoom = (from.zoom + (to.zoom - from.zoom) * e) / dip
        return GlobeCamera(from.lat + (to.lat - from.lat) * e, from.lon + dLon * e, zoom).normalized()
    }
}

/** Angle in radians between two camera centres. */
fun angularDistance(a: GlobeCamera, b: GlobeCamera): Double {
    val p1 = Math.toRadians(a.lat)
    val p2 = Math.toRadians(b.lat)
    val dl = Math.toRadians(b.lon - a.lon)
    return acos((sin(p1) * sin(p2) + cos(p1) * cos(p2) * cos(dl)).coerceIn(-1.0, 1.0))
}

// ── pin clusters ─────────────────────────────────────────────────────────────────────────────

/** A pin as seen on screen. */
data class PinPoint(val key: String, val x: Float, val y: Float, val weight: Int)

/** Pins that are too close on screen are shown as one bubble with a number. */
data class PinCluster(val x: Float, val y: Float, val members: List<PinPoint>)

/** Greedy grouping: the busiest pin takes every free pin within [radiusPx]. */
fun clusterPins(pins: List<PinPoint>, radiusPx: Float): List<PinCluster> {
    val order = pins.indices.sortedByDescending { pins[it].weight }
    val taken = BooleanArray(pins.size)
    val result = mutableListOf<PinCluster>()
    for (i in order) {
        if (taken[i]) continue
        taken[i] = true
        val seed = pins[i]
        val members = mutableListOf(seed)
        for (j in order) {
            if (taken[j]) continue
            val other = pins[j]
            if (hypot(other.x - seed.x, other.y - seed.y) <= radiusPx) {
                taken[j] = true
                members += other
            }
        }
        val cx = members.map { it.x }.average().toFloat()
        val cy = members.map { it.y }.average().toFloat()
        result += PinCluster(cx, cy, members)
    }
    return result
}
