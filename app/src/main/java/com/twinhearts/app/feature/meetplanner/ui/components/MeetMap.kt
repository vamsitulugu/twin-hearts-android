package com.twinhearts.app.feature.meetplanner.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.twinhearts.app.core.design.TwinTheme
import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import com.twinhearts.app.feature.livemap.domain.nav.sampled
import com.twinhearts.app.feature.livemap.ui.map.CameraRequest
import com.twinhearts.app.feature.livemap.ui.map.LiveMapView
import com.twinhearts.app.feature.livemap.ui.map.MapColors
import com.twinhearts.app.feature.livemap.ui.map.MapMarker
import com.twinhearts.app.feature.livemap.ui.map.MapOverlay
import com.twinhearts.app.feature.livemap.ui.map.MapPoint
import com.twinhearts.app.feature.livemap.ui.map.MapStyles
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.Place

/**
 * Why this file exists: the real map (MapLibre tiles, the same one Live Map uses) for Meet Planner.
 * It shows the numbered stops and the route line, and fits the camera around them. While
 * navigating, the caller passes [camera] and [me] so the map follows the car.
 */
@Composable
internal fun MeetMap(
    stops: List<Place>,
    route: List<GeoPoint>,
    modifier: Modifier = Modifier,
    fallbackCenter: GeoPoint? = null,
    me: MapPoint? = null,
    camera: CameraRequest? = null,
    navigating: Boolean = false,
    insideScroll: Boolean = false,
    onUserGesture: () -> Unit = {},
) {
    val colors = TwinTheme.colors
    val mapColors = remember(colors) {
        MapColors(
            me = Color(0xFF4DA3FF).toArgb(),
            partner = colors.accent.toArgb(),
            route = colors.brand.toArgb(),
            place = colors.success.toArgb(),
            stop = colors.inkMuted.toArgb(),
            destination = colors.error.toArgb(),
            note = colors.accent.toArgb(),
            label = colors.ink.toArgb(),
            labelHalo = colors.background.toArgb(),
            outline = Color.White.toArgb(),
            alt = colors.inkFaint.toArgb(),
            badge = colors.brand.toArgb(),
        )
    }
    // OpenFreeMap street style: free, no key. (The dark CARTO tiles of "Auto" at night now demand an API key.)
    val style = remember { MapStyles.sourceFor(MapStyleKind.Street, nightNow = false) }
    val overlay = remember(stops, route, me) {
        MapOverlay(
            me = me,
            route = route.map { MapPoint(it.lat, it.lng) },
            numbered = stops.mapIndexed { i, s -> MapMarker(MapPoint(s.lat, s.lng), "${i + 1}") },
        )
    }
    // Fit the whole trip into view whenever the stops or the route change (not while following the car).
    val fit = remember(stops, route) {
        val line = if (route.size >= 2) route.sampled(FIT_SAMPLES) else emptyList()
        (line + stops.map { GeoPoint(it.lat, it.lng) }).map { MapPoint(it.lat, it.lng) }
    }
    val autoCamera = remember(fit) {
        when {
            fit.size >= 2 -> CameraRequest(id = System.nanoTime(), fit = fit)
            fit.size == 1 -> CameraRequest(id = System.nanoTime(), target = fit.first(), zoom = SINGLE_ZOOM)
            else -> null
        }
    }
    LiveMapView(
        style = style,
        overlay = overlay,
        colors = mapColors,
        modifier = modifier.fillMaxSize(),
        camera = camera ?: autoCamera,
        initialCenter = fit.firstOrNull() ?: fallbackCenter?.let { MapPoint(it.lat, it.lng) } ?: MapPoint(INDIA_LAT, INDIA_LNG),
        initialZoom = if (fit.isNotEmpty() || fallbackCenter != null) SINGLE_ZOOM else INDIA_ZOOM,
        navigating = navigating,
        onUserGesture = onUserGesture,
        lockParentScroll = insideScroll,
        textureMode = !navigating,
    )
}

private const val FIT_SAMPLES = 200
private const val SINGLE_ZOOM = 13.0
private const val INDIA_LAT = 20.5937
private const val INDIA_LNG = 78.9629
private const val INDIA_ZOOM = 4.5
