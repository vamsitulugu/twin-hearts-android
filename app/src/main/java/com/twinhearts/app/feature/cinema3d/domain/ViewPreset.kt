package com.twinhearts.app.feature.cinema3d.domain

/** Ready-made camera positions for the spike. */
enum class ViewPreset(val label: String) {
    Seat("Seat E8"),
    HallBack("Hall back"),
    Lobby("Lobby"),
    Orbit("Orbit"),
}

/** Eye height above the floor for someone sitting down, in metres. */
const val SEAT_EYE_HEIGHT = 1.15

/** Eye height above the floor for someone standing, in metres. */
const val STANDING_EYE_HEIGHT = 1.65

/** Puts [rig] at the position for [preset]. Everything comes from the layout file, nothing is guessed per phone. */
fun applyPreset(rig: CameraRig, preset: ViewPreset, layout: TheatreLayout) {
    val screen = layout.screenCentre()
    when (preset) {
        ViewPreset.Seat -> {
            val seat = layout.seat("E8") ?: layout.seats.firstOrNull()
            val eye = if (seat != null) {
                Vec3(seat.x, seat.y + SEAT_EYE_HEIGHT, seat.z)
            } else {
                Vec3(0.0, 2.15, -0.4)
            }
            rig.lookFromAt(eye, screen)
        }
        ViewPreset.HallBack -> {
            // Just inside the door, a little above head height, looking at the screen.
            rig.lookFromAt(Vec3(0.0, 3.6, layout.hall.doorZ - 0.6), screen)
        }
        ViewPreset.Lobby -> {
            val floor = layout.lobby.floorY
            val eye = Vec3(0.0, floor + STANDING_EYE_HEIGHT, layout.lobby.originZ + layout.lobby.depth - 1.5)
            rig.lookFromAt(eye, Vec3(0.0, floor + 1.5, layout.hall.doorZ + 1.5))
        }
        ViewPreset.Orbit -> {
            // From outside the hall the roof and walls hide the inside. That is expected; it proves the shell is closed.
            val midZ = (layout.hall.frontZ + layout.hall.doorZ) / 2.0
            rig.orbitAround(0.0, 2.5, midZ, dist = 22.0, yawRad = 0.6, pitchRad = 0.5)
        }
    }
}
