package com.twinhearts.app.feature.cinema3d.domain

import kotlinx.serialization.Serializable

/**
 * The parts of `theatre_layout.json` the spike needs. Unknown keys are ignored when decoding,
 * so the file can grow without breaking this class. Units are metres, Y up.
 */
@Serializable
data class TheatreLayout(
    val hall: HallLayout,
    val lobby: LobbyLayout,
    val screen: ScreenLayout,
    val seats: List<SeatLayout>,
)

@Serializable
data class HallLayout(val frontZ: Double, val doorZ: Double)

@Serializable
data class LobbyLayout(val floorY: Double, val originZ: Double, val depth: Double)

@Serializable
data class ScreenLayout(val center: List<Double>, val defaultSize: List<Double>)

@Serializable
data class SeatLayout(
    val id: String,
    val row: Int,
    val col: Int,
    val x: Double,
    val y: Double,
    val z: Double,
    val takenByAudience: Boolean = false,
)

/** Centre of the screen as a point. Falls back to the hall front if the file is malformed. */
fun TheatreLayout.screenCentre(): Vec3 {
    val c = screen.center
    return if (c.size >= 3) Vec3(c[0], c[1], c[2]) else Vec3(0.0, 3.45, hall.frontZ + 0.15)
}

fun TheatreLayout.seat(id: String): SeatLayout? = seats.firstOrNull { it.id == id }
