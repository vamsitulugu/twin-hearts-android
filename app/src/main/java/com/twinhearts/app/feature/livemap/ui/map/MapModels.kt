package com.twinhearts.app.feature.livemap.ui.map

import androidx.compose.runtime.Immutable

/** A point on the map, (latitude, longitude). */
@Immutable
data class MapPoint(val lat: Double, val lng: Double)

/** A named dot, used for saved places. */
@Immutable
data class MapMarker(val point: MapPoint, val label: String)

/**
 * Everything drawn on top of the map tiles. The screens build one of these from their state and the
 * map view only draws it, so the map holds no business logic (R-42).
 */
@Immutable
data class MapOverlay(
    val me: MapPoint? = null,
    val partner: MapPoint? = null,
    val meAccuracyM: Double? = null,
    val partnerAccuracyM: Double? = null,
    /** Recent path of each person, oldest first. */
    val myTrail: List<MapPoint> = emptyList(),
    val partnerTrail: List<MapPoint> = emptyList(),
    /** Road route (directions) or the route of the selected day. */
    val route: List<MapPoint> = emptyList(),
    /** Other possible routes, drawn grey under the chosen one. */
    val altRoutes: List<List<MapPoint>> = emptyList(),
    val places: List<MapMarker> = emptyList(),
    val stops: List<MapPoint> = emptyList(),
    val destination: MapPoint? = null,
    val playback: MapPoint? = null,
    val loveNotes: List<MapPoint> = emptyList(),
)

/** Colours for the overlay, taken from the design tokens by the caller (R-50). Plain ARGB ints. */
@Immutable
data class MapColors(
    val me: Int,
    val partner: Int,
    val route: Int,
    val place: Int,
    val stop: Int,
    val destination: Int,
    val note: Int,
    val label: Int,
    val labelHalo: Int,
    val outline: Int,
    /** Colour of the grey alternative routes; 0 = use [stop]. */
    val alt: Int = 0,
)

/** "Move the camera" request. A new [id] means a new request, even to the same place. */
@Immutable
data class CameraRequest(
    val id: Long,
    val target: MapPoint? = null,
    val zoom: Double? = null,
    /** When set, the camera fits all these points instead of going to [target]. */
    val fit: List<MapPoint> = emptyList(),
    /** Turn the map so this compass direction points up (navigation). Null keeps the current rotation. */
    val bearing: Double? = null,
    /** Camera tilt in degrees. Null follows the 3D switch. */
    val tilt: Double? = null,
    /** Animation length in ms. Null uses the default. */
    val animMs: Int? = null,
)
