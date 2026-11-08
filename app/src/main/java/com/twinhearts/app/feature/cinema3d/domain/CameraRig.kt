package com.twinhearts.app.feature.cinema3d.domain

import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/** A point or direction in the theatre's world (metres, Y up, screen at -Z, hall door at +Z). */
data class Vec3(val x: Double, val y: Double, val z: Double)

/**
 * Walk: the eye stands still (or dollies along the floor) and the finger turns the head.
 * Orbit: the eye circles a target point.
 */
enum class CameraMode { Walk, Orbit }

/**
 * Pure camera maths for the 3D theatre spike. No Android or Filament types, so it can be unit-tested on the JVM.
 *
 * Angles are radians. With yaw = 0 and pitch = 0 the camera looks straight down -Z, toward the screen.
 * Positive pitch looks DOWN (forward.y = -sin(pitch)).
 */
class CameraRig {

    var mode: CameraMode = CameraMode.Walk
        private set

    /** Vertical field of view in degrees. */
    var fovDegrees: Double = 60.0
        private set

    // Walk: this is the eye. Orbit: this is the point being circled.
    private var px = 0.0
    private var py = 1.7
    private var pz = 0.0
    private var yaw = 0.0
    private var pitch = 0.0
    private var distance = 10.0

    /** Walk mode: stand at (x, y, z) and look in the given direction. */
    fun lookFrom(x: Double, y: Double, z: Double, yawRad: Double = 0.0, pitchRad: Double = 0.0) {
        mode = CameraMode.Walk
        px = clampX(x)
        py = y
        pz = clampZ(z)
        yaw = yawRad
        pitch = pitchRad.coerceIn(-MAX_WALK_PITCH, MAX_WALK_PITCH)
    }

    /** Walk mode: stand at [eye] and face [target]. */
    fun lookFromAt(eye: Vec3, target: Vec3) {
        val dx = target.x - eye.x
        val dy = target.y - eye.y
        val dz = target.z - eye.z
        val len = sqrt(dx * dx + dy * dy + dz * dz)
        if (len < 1e-6) {
            lookFrom(eye.x, eye.y, eye.z)
            return
        }
        val fx = dx / len
        val fy = dy / len
        val fz = dz / len
        lookFrom(eye.x, eye.y, eye.z, yawRad = atan2(-fx, -fz), pitchRad = asin((-fy).coerceIn(-1.0, 1.0)))
    }

    /** Orbit mode: circle (x, y, z) from [dist] metres away. */
    fun orbitAround(x: Double, y: Double, z: Double, dist: Double, yawRad: Double, pitchRad: Double) {
        mode = CameraMode.Orbit
        px = x
        py = y
        pz = z
        distance = dist.coerceIn(MIN_ORBIT_DISTANCE, MAX_ORBIT_DISTANCE)
        yaw = yawRad
        pitch = pitchRad.coerceIn(MIN_ORBIT_PITCH, MAX_ORBIT_PITCH)
    }

    /** One-finger drag. Dragging moves the world with the finger. */
    fun drag(dxPixels: Float, dyPixels: Float) {
        val dx = dxPixels * RADIANS_PER_PIXEL
        val dy = dyPixels * RADIANS_PER_PIXEL
        when (mode) {
            CameraMode.Walk -> {
                yaw += dx
                pitch = (pitch - dy).coerceIn(-MAX_WALK_PITCH, MAX_WALK_PITCH)
            }
            CameraMode.Orbit -> {
                yaw -= dx
                pitch = (pitch + dy).coerceIn(MIN_ORBIT_PITCH, MAX_ORBIT_PITCH)
            }
        }
    }

    /** Pinch. [scale] > 1 means fingers moved apart: walk forward, or move closer in orbit. */
    fun pinch(scale: Float) {
        if (scale <= 0f) return
        when (mode) {
            CameraMode.Walk -> {
                val step = (scale - 1.0) * DOLLY_METRES_PER_FULL_PINCH
                px = clampX(px + (-sin(yaw)) * step)
                pz = clampZ(pz + (-cos(yaw)) * step)
            }
            CameraMode.Orbit -> {
                distance = (distance / scale).coerceIn(MIN_ORBIT_DISTANCE, MAX_ORBIT_DISTANCE)
            }
        }
    }

    /** Where the camera is. */
    fun eye(): Vec3 = when (mode) {
        CameraMode.Walk -> Vec3(px, py, pz)
        CameraMode.Orbit -> {
            val f = forward()
            Vec3(px - f.x * distance, py - f.y * distance, pz - f.z * distance)
        }
    }

    /** The point the camera looks at. */
    fun target(): Vec3 = when (mode) {
        CameraMode.Walk -> {
            val f = forward()
            Vec3(px + f.x, py + f.y, pz + f.z)
        }
        CameraMode.Orbit -> Vec3(px, py, pz)
    }

    private fun forward(): Vec3 =
        Vec3(-sin(yaw) * cos(pitch), -sin(pitch), -cos(yaw) * cos(pitch))

    private fun clampX(x: Double) = min(max(x, WALK_MIN_X), WALK_MAX_X)
    private fun clampZ(z: Double) = min(max(z, WALK_MIN_Z), WALK_MAX_Z)

    companion object {
        const val RADIANS_PER_PIXEL = 0.004
        const val DOLLY_METRES_PER_FULL_PINCH = 6.0
        const val MAX_WALK_PITCH = 1.2
        const val MIN_ORBIT_PITCH = 0.05
        const val MAX_ORBIT_PITCH = 1.45
        const val MIN_ORBIT_DISTANCE = 3.0
        const val MAX_ORBIT_DISTANCE = 40.0

        // Walking stays inside the hall + lobby footprint (hall half width 6.3, lobby half width 9).
        const val WALK_MIN_X = -8.5
        const val WALK_MAX_X = 8.5
        const val WALK_MIN_Z = -10.5
        const val WALK_MAX_Z = 21.0
    }
}
