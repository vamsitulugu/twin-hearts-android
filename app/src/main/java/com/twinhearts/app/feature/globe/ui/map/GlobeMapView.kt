package com.twinhearts.app.feature.globe.ui.map

import android.os.Bundle
import android.graphics.PointF
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.twinhearts.app.feature.globe.domain.greatCircle
import com.twinhearts.app.feature.globe.domain.model.ArcLeg
import com.twinhearts.app.feature.globe.domain.model.CityGroup
import com.twinhearts.app.feature.globe.domain.model.GeoPoint
import com.twinhearts.app.feature.globe.ui.CameraRequest
import com.twinhearts.app.feature.globe.ui.LiveUi
import com.twinhearts.app.feature.globe.ui.globe.GlobePalette
import com.twinhearts.app.feature.globe.ui.globe.PinArt
import com.twinhearts.app.feature.globe.ui.globe.rememberGlobePalette
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

/*
 * Why this file exists: the flat "Map" view of the Globe, drawn with MapLibre (R-11) on the free
 * OpenFreeMap street style, so zooming in shows real roads and buildings (the web app's "street level").
 * It shows the same heart pins, trip lines (real road routes for short legs) and live dots.
 */

private const val STYLE_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val ANIM_MS = 1200
private const val FIT_PADDING_PX = 140
private const val WORLD_LAT = 20.0
private const val WORLD_LNG = 0.0
private const val WORLD_ZOOM = 1.8
private const val SINGLE_PIN_ZOOM = 5.0
private const val PIN_DP = 52f
private const val SRC_ARCS = "gm-arcs"
private const val SRC_PINS = "gm-pins"
private const val SRC_TRAILS = "gm-trails"
private const val SRC_LIVE = "gm-live"
private const val SRC_SEARCH = "gm-search"
private const val LAYER_PINS = "gm-pins-layer"

@Composable
fun GlobeMapView(
    groups: List<CityGroup>,
    legs: List<ArcLeg>,
    roads: Map<String, List<GeoPoint>>,
    selectedKey: String?,
    live: LiveUi,
    searchPin: GeoPoint?,
    camera: CameraRequest?,
    onPinTap: (String) -> Unit,
    onCameraArrived: (Long) -> Unit,
    onZoomChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val palette = rememberGlobePalette()
    val art = remember(palette) { PinArt(palette) }
    val density = LocalDensity.current.density
    val mapView = remember {
        MapLibre.getInstance(context)
        MapView(context)
    }
    var map by remember { mutableStateOf<MapLibreMap?>(null) }
    var styleVersion by remember { mutableStateOf(0) }
    var fitted by remember { mutableStateOf(false) }

    val currentPinTap by rememberUpdatedState(onPinTap)
    val currentZoom by rememberUpdatedState(onZoomChange)
    val currentArrived by rememberUpdatedState(onCameraArrived)

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

    LaunchedEffect(mapView) {
        mapView.getMapAsync { m ->
            map = m
            m.uiSettings.isCompassEnabled = true
            m.cameraPosition = CameraPosition.Builder().target(LatLng(WORLD_LAT, WORLD_LNG)).zoom(WORLD_ZOOM).build()
            m.addOnMapClickListener { latLng ->
                val screen: PointF = m.projection.toScreenLocation(latLng)
                val hit = m.queryRenderedFeatures(screen, LAYER_PINS).firstOrNull()
                val key = hit?.getStringProperty("key")
                if (key != null) {
                    currentPinTap(key)
                    true
                } else {
                    false
                }
            }
            m.addOnCameraIdleListener { currentZoom(m.cameraPosition.zoom) }
        }
    }

    LaunchedEffect(map) {
        val m = map ?: return@LaunchedEffect
        m.setStyle(Style.Builder().fromUri(STYLE_URL)) { loaded ->
            installLayers(loaded, palette, density)
            styleVersion += 1
        }
    }

    LaunchedEffect(map, styleVersion, groups, legs, roads, selectedKey, live, searchPin, palette) {
        val loaded = map?.style ?: return@LaunchedEffect
        if (!loaded.isFullyLoaded) return@LaunchedEffect
        drawOverlay(loaded, art, palette, groups, legs, roads, selectedKey, live, searchPin)
    }

    // First time with pins: show them all, like a good map should.
    LaunchedEffect(map, groups.isNotEmpty()) {
        val m = map ?: return@LaunchedEffect
        if (fitted || groups.isEmpty()) return@LaunchedEffect
        fitted = true
        if (groups.size == 1) {
            m.cameraPosition = CameraPosition.Builder().target(LatLng(groups[0].lat, groups[0].lng)).zoom(SINGLE_PIN_ZOOM).build()
        } else {
            val bounds = LatLngBounds.Builder().includes(groups.map { LatLng(it.lat, it.lng) }).build()
            m.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, FIT_PADDING_PX))
        }
    }

    LaunchedEffect(map, camera?.id) {
        val m = map ?: return@LaunchedEffect
        val request = camera ?: return@LaunchedEffect
        val position = CameraPosition.Builder().target(LatLng(request.lat, request.lng)).zoom(request.mapZoom).tilt(0.0).build()
        m.animateCamera(
            CameraUpdateFactory.newCameraPosition(position),
            ANIM_MS,
            object : MapLibreMap.CancelableCallback {
                override fun onFinish() = currentArrived(request.id)
                override fun onCancel() = Unit
            },
        )
    }

    AndroidView(factory = { mapView }, modifier = modifier)
}

private fun empty() = FeatureCollection.fromFeatures(emptyList<Feature>())

private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

private fun installLayers(style: Style, palette: GlobePalette, density: Float) {
    listOf(SRC_ARCS, SRC_PINS, SRC_TRAILS, SRC_LIVE, SRC_SEARCH).forEach { style.addSource(GeoJsonSource(it, empty())) }

    style.addLayer(
        LineLayer("gm-arcs-glow", SRC_ARCS).withProperties(
            PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
            PropertyFactory.lineWidth(8f),
            PropertyFactory.lineOpacity(0.25f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        LineLayer("gm-arcs-line", SRC_ARCS).withProperties(
            PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
            PropertyFactory.lineWidth(3f),
            PropertyFactory.lineOpacity(0.95f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
    style.addLayer(
        LineLayer("gm-trails-line", SRC_TRAILS).withProperties(
            PropertyFactory.lineColor(Expression.toColor(Expression.get("color"))),
            PropertyFactory.lineWidth(3f),
            PropertyFactory.lineOpacity(0.8f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
        ),
    )
    style.addLayer(
        CircleLayer("gm-live-dots", SRC_LIVE).withProperties(
            PropertyFactory.circleColor(Expression.toColor(Expression.get("color"))),
            PropertyFactory.circleRadius(8f),
            PropertyFactory.circleStrokeColor(palette.outline.toArgb()),
            PropertyFactory.circleStrokeWidth(2.5f),
        ),
    )
    style.addLayer(
        CircleLayer("gm-search-dot", SRC_SEARCH).withProperties(
            PropertyFactory.circleColor(palette.search.toArgb()),
            PropertyFactory.circleRadius(7f),
            PropertyFactory.circleStrokeColor(palette.outline.toArgb()),
            PropertyFactory.circleStrokeWidth(2f),
        ),
    )
    style.addLayer(
        SymbolLayer(LAYER_PINS, SRC_PINS).withProperties(
            PropertyFactory.iconImage(Expression.get("icon")),
            PropertyFactory.iconAnchor(Property.ICON_ANCHOR_BOTTOM),
            PropertyFactory.iconAllowOverlap(true),
            PropertyFactory.iconIgnorePlacement(true),
            PropertyFactory.iconSize(PIN_DP * density / PinArt.PIN_PX),
            PropertyFactory.textField(Expression.get("label")),
            PropertyFactory.textFont(arrayOf("Noto Sans Regular")),
            PropertyFactory.textSize(12f),
            PropertyFactory.textAnchor(Property.TEXT_ANCHOR_TOP),
            PropertyFactory.textOffset(arrayOf(0f, 0.2f)),
            PropertyFactory.textOptional(true),
            PropertyFactory.textColor(palette.pinOutline),
            PropertyFactory.textHaloColor(palette.labelHalo),
            PropertyFactory.textHaloWidth(1.6f),
        ),
    )
}

private fun lineOf(points: List<GeoPoint>): LineString {
    // Keep the longitude continuous so a line over the date line does not jump across the whole map.
    var previous = points.first().lng
    val coords = points.map { p ->
        var lng = p.lng
        while (lng - previous > 180) lng -= 360
        while (lng - previous < -180) lng += 360
        previous = lng
        Point.fromLngLat(lng, p.lat)
    }
    return LineString.fromLngLats(coords)
}

private fun drawOverlay(
    style: Style,
    art: PinArt,
    palette: GlobePalette,
    groups: List<CityGroup>,
    legs: List<ArcLeg>,
    roads: Map<String, List<GeoPoint>>,
    selectedKey: String?,
    live: LiveUi,
    searchPin: GeoPoint?,
) {
    // Trip lines: a real road for short legs when it was found, otherwise the shortest path over the globe.
    val arcs = legs.map { leg ->
        val road = if (leg.driveable) roads[leg.routeKey] else null
        val points = road ?: greatCircle(leg.from, leg.to, (leg.distanceKm / 150.0).toInt().coerceIn(8, 64))
        Feature.fromGeometry(lineOf(points)).also { it.addStringProperty("color", hex(palette.arcs[leg.index % palette.arcs.size])) }
    }
    style.getSourceAs<GeoJsonSource>(SRC_ARCS)?.setGeoJson(FeatureCollection.fromFeatures(arcs))

    val pins = groups.map { g ->
        val selected = g.key == selectedKey
        val icon = "pin-${g.emoji}-${g.count}-$selected"
        if (style.getImage(icon) == null) style.addImage(icon, art.pin(g.emoji, g.count, selected))
        Feature.fromGeometry(Point.fromLngLat(g.lng, g.lat)).also {
            it.addStringProperty("key", g.key)
            it.addStringProperty("icon", icon)
            it.addStringProperty("label", g.city)
        }
    }
    style.getSourceAs<GeoJsonSource>(SRC_PINS)?.setGeoJson(FeatureCollection.fromFeatures(pins))

    val trails = buildList {
        if (live.enabled && live.myTrail.size >= 2) add(Feature.fromGeometry(lineOf(live.myTrail)).also { it.addStringProperty("color", hex(palette.me)) })
        if (live.enabled && live.partnerTrail.size >= 2) add(Feature.fromGeometry(lineOf(live.partnerTrail)).also { it.addStringProperty("color", hex(palette.partner)) })
    }
    style.getSourceAs<GeoJsonSource>(SRC_TRAILS)?.setGeoJson(FeatureCollection.fromFeatures(trails))

    val dots = buildList {
        if (live.enabled) {
            live.me?.takeIf { it.online }?.let { add(Feature.fromGeometry(Point.fromLngLat(it.point.lng, it.point.lat)).also { f -> f.addStringProperty("color", hex(palette.me)) }) }
            live.partner?.takeIf { it.online }?.let { add(Feature.fromGeometry(Point.fromLngLat(it.point.lng, it.point.lat)).also { f -> f.addStringProperty("color", hex(palette.partner)) }) }
        }
    }
    style.getSourceAs<GeoJsonSource>(SRC_LIVE)?.setGeoJson(FeatureCollection.fromFeatures(dots))

    val search = if (searchPin != null) listOf(Feature.fromGeometry(Point.fromLngLat(searchPin.lng, searchPin.lat))) else emptyList()
    style.getSourceAs<GeoJsonSource>(SRC_SEARCH)?.setGeoJson(FeatureCollection.fromFeatures(search))
}

/** Zoom at which the "Street level" badge appears (the web app shows it at street level too). */
const val STREET_LEVEL_ZOOM = 15.0
