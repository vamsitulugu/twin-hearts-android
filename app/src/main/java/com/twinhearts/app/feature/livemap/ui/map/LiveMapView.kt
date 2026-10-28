package com.twinhearts.app.feature.livemap.ui.map

import android.os.Bundle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.expressions.Expression
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

/**
 * The one place that knows MapLibre. It shows the tiles, draws a [MapOverlay] on top and moves the
 * camera when a new [CameraRequest] arrives. Everything else in the feature talks to it with plain data.
 */
@Composable
fun LiveMapView(
    style: MapStyles.Source,
    overlay: MapOverlay,
    colors: MapColors,
    modifier: Modifier = Modifier,
    camera: CameraRequest? = null,
    tilted: Boolean = false,
    initialCenter: MapPoint? = null,
    initialZoom: Double = DEFAULT_ZOOM,
    onLongPress: (MapPoint) -> Unit = {},
    onCameraIdle: (MapPoint, Double) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    // Counts up each time a style finishes loading, so the overlay is redrawn onto the new style.
    var styleVersion by remember { mutableStateOf(0) }

    val currentLongPress by rememberUpdatedState(onLongPress)
    val currentCameraIdle by rememberUpdatedState(onCameraIdle)

    // Forward the screen lifecycle to the MapView. Without this MapLibre leaks memory and stops drawing.
    DisposableEffect(lifecycle, mapView) {
        var stage = 0 // 0 nothing, 1 created, 2 started, 3 resumed
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> if (stage < 1) { mapView.onCreate(Bundle()); stage = 1 }
                Lifecycle.Event.ON_START -> if (stage < 2) { mapView.onStart(); stage = 2 }
                Lifecycle.Event.ON_RESUME -> if (stage < 3) { mapView.onResume(); stage = 3 }
                Lifecycle.Event.ON_PAUSE -> if (stage == 3) { mapView.onPause(); stage = 2 }
                Lifecycle.Event.ON_STOP -> if (stage == 2) { mapView.onStop(); stage = 1 }
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (stage == 3) { mapView.onPause(); stage = 2 }
            if (stage == 2) { mapView.onStop(); stage = 1 }
            if (stage == 1) mapView.onDestroy()
        }
    }

    // Ask for the map once, then set it up.
    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.isCompassEnabled = true
            initialCenter?.let { c ->
                m.cameraPosition = CameraPosition.Builder().target(LatLng(c.lat, c.lng)).zoom(initialZoom).build()
            }
            m.addOnMapLongClickListener { latLng ->
                currentLongPress(MapPoint(latLng.latitude, latLng.longitude))
                true
            }
            m.addOnCameraIdleListener {
                val target = m.cameraPosition.target
                if (target != null) currentCameraIdle(MapPoint(target.latitude, target.longitude), m.cameraPosition.zoom)
            }
        }
    }

    // Load (or reload) the tiles whenever the map type changes.
    LaunchedEffect(map, style) {
        val m = map ?: return@LaunchedEffect
        val builder = when (style) {
            is MapStyles.Source.Url -> Style.Builder().fromUri(style.value)
            is MapStyles.Source.Json -> Style.Builder().fromJson(style.value)
        }
        m.setStyle(builder) { loaded ->
            installLayers(loaded, colors)
            styleVersion += 1
        }
    }

    // Draw the overlay again whenever it, the colours or the style change.
    LaunchedEffect(map, overlay, styleVersion) {
        val loaded = map?.style ?: return@LaunchedEffect
        if (!loaded.isFullyLoaded) return@LaunchedEffect
        drawOverlay(loaded, overlay)
    }

    // Camera moves.
    LaunchedEffect(map, camera) {
        val m = map ?: return@LaunchedEffect
        val request = camera ?: return@LaunchedEffect
        moveCamera(m, request, tilted)
    }

    // 3D / eye-level view: tilt the camera, or flatten it again.
    LaunchedEffect(map, tilted) {
        val m = map ?: return@LaunchedEffect
        val current = m.cameraPosition
        val tilt = if (tilted) TILT_3D else 0.0
        if (current.tilt != tilt) {
            m.animateCamera(CameraUpdateFactory.newCameraPosition(CameraPosition.Builder(current).tilt(tilt).build()), ANIM_MS)
        }
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private const val DEFAULT_ZOOM = 14.0
private const val TILT_3D = 60.0
private const val ANIM_MS = 600
private const val FOCUS_ZOOM = 16.0
private const val FIT_PADDING_PX = 120

private fun moveCamera(map: MapLibreMap, request: CameraRequest, tilted: Boolean) {
    val fit = request.fit
    if (fit.size >= 2) {
        val bounds = LatLngBounds.Builder().includes(fit.map { LatLng(it.lat, it.lng) }).build()
        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, FIT_PADDING_PX), ANIM_MS)
        return
    }
    val target = request.target ?: fit.firstOrNull() ?: return
    val position = CameraPosition.Builder()
        .target(LatLng(target.lat, target.lng))
        .zoom(request.zoom ?: maxOf(map.cameraPosition.zoom, FOCUS_ZOOM))
        .tilt(if (tilted) TILT_3D else 0.0)
        .build()
    map.animateCamera(CameraUpdateFactory.newCameraPosition(position), ANIM_MS)
}

// ── layers ───────────────────────────────────────────────────────────────────────────────────────

private const val SRC_ROUTE = "lm-route"
private const val SRC_MY_TRAIL = "lm-my-trail"
private const val SRC_PT_TRAIL = "lm-pt-trail"
private const val SRC_PLACES = "lm-places"
private const val SRC_STOPS = "lm-stops"
private const val SRC_NOTES = "lm-notes"
private const val SRC_DEST = "lm-dest"
private const val SRC_PLAYBACK = "lm-playback"
private const val SRC_ME = "lm-me"
private const val SRC_PARTNER = "lm-partner"

private fun empty() = FeatureCollection.fromFeatures(emptyList<Feature>())

private fun installLayers(style: Style, c: MapColors) {
    listOf(SRC_ROUTE, SRC_MY_TRAIL, SRC_PT_TRAIL, SRC_PLACES, SRC_STOPS, SRC_NOTES, SRC_DEST, SRC_PLAYBACK, SRC_ME, SRC_PARTNER)
        .forEach { style.addSource(GeoJsonSource(it, empty())) }

    fun line(id: String, source: String, color: Int, width: Float, opacity: Float) = LineLayer(id, source).withProperties(
        PropertyFactory.lineColor(color),
        PropertyFactory.lineWidth(width),
        PropertyFactory.lineOpacity(opacity),
        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
    )

    fun dot(id: String, source: String, color: Int, radius: Float) = CircleLayer(id, source).withProperties(
        PropertyFactory.circleColor(color),
        PropertyFactory.circleRadius(radius),
        PropertyFactory.circleStrokeColor(c.outline),
        PropertyFactory.circleStrokeWidth(2.5f),
    )

    fun halo(id: String, source: String, color: Int) = CircleLayer(id, source).withProperties(
        PropertyFactory.circleColor(color),
        PropertyFactory.circleOpacity(0.22f),
        PropertyFactory.circleRadius(22f),
    )

    style.addLayer(line("lm-route-line", SRC_ROUTE, c.route, 6f, 0.9f))
    style.addLayer(line("lm-my-trail-line", SRC_MY_TRAIL, c.me, 4f, 0.6f))
    style.addLayer(line("lm-pt-trail-line", SRC_PT_TRAIL, c.partner, 4f, 0.6f))
    style.addLayer(dot("lm-places-dot", SRC_PLACES, c.place, 7f))
    style.addLayer(
        SymbolLayer("lm-places-label", SRC_PLACES).withProperties(
            PropertyFactory.textField(Expression.get("label")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textColor(c.label),
            PropertyFactory.textHaloColor(c.labelHalo),
            PropertyFactory.textHaloWidth(1.6f),
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textOffset(arrayOf(0f, 1.3f)),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.textOptional(true),
        ),
    )
    style.addLayer(dot("lm-stops-dot", SRC_STOPS, c.stop, 6f))
    style.addLayer(dot("lm-notes-dot", SRC_NOTES, c.note, 8f))
    style.addLayer(dot("lm-dest-dot", SRC_DEST, c.destination, 9f))
    style.addLayer(halo("lm-partner-halo", SRC_PARTNER, c.partner))
    style.addLayer(dot("lm-partner-dot", SRC_PARTNER, c.partner, 10f))
    style.addLayer(halo("lm-me-halo", SRC_ME, c.me))
    style.addLayer(dot("lm-me-dot", SRC_ME, c.me, 10f))
    style.addLayer(dot("lm-playback-dot", SRC_PLAYBACK, c.route, 9f))
}

private fun pointFeature(p: MapPoint, label: String? = null): Feature {
    val f = Feature.fromGeometry(Point.fromLngLat(p.lng, p.lat))
    if (label != null) f.addStringProperty("label", label)
    return f
}

private fun pointsOf(list: List<MapPoint>) = FeatureCollection.fromFeatures(list.map { pointFeature(it) })

private fun lineOf(list: List<MapPoint>): FeatureCollection =
    if (list.size < 2) {
        empty()
    } else {
        FeatureCollection.fromFeature(Feature.fromGeometry(LineString.fromLngLats(list.map { Point.fromLngLat(it.lng, it.lat) })))
    }

private fun drawOverlay(style: Style, o: MapOverlay) {
    fun set(id: String, data: FeatureCollection) {
        style.getSourceAs<GeoJsonSource>(id)?.setGeoJson(data)
    }
    set(SRC_ROUTE, lineOf(o.route))
    set(SRC_MY_TRAIL, lineOf(o.myTrail))
    set(SRC_PT_TRAIL, lineOf(o.partnerTrail))
    set(SRC_PLACES, FeatureCollection.fromFeatures(o.places.map { pointFeature(it.point, it.label) }))
    set(SRC_STOPS, pointsOf(o.stops))
    set(SRC_NOTES, pointsOf(o.loveNotes))
    set(SRC_DEST, pointsOf(listOfNotNull(o.destination)))
    set(SRC_PLAYBACK, pointsOf(listOfNotNull(o.playback)))
    set(SRC_ME, pointsOf(listOfNotNull(o.me)))
    set(SRC_PARTNER, pointsOf(listOfNotNull(o.partner)))
}
