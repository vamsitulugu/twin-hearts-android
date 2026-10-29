package com.twinhearts.app.feature.globe.ui.globe

import android.graphics.Paint
import android.graphics.Typeface
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import com.twinhearts.app.R
import com.twinhearts.app.core.common.rememberReduceMotion
import com.twinhearts.app.feature.globe.data.Country
import com.twinhearts.app.feature.globe.data.WorldAtlas
import com.twinhearts.app.feature.globe.domain.Flight
import com.twinhearts.app.feature.globe.domain.GlobeCamera
import com.twinhearts.app.feature.globe.domain.GlobeView
import com.twinhearts.app.feature.globe.domain.PinCluster
import com.twinhearts.app.feature.globe.domain.PinPoint
import com.twinhearts.app.feature.globe.domain.RingClipper
import com.twinhearts.app.feature.globe.domain.Vec3
import com.twinhearts.app.feature.globe.domain.clusterPins
import com.twinhearts.app.feature.globe.domain.greatCircle
import com.twinhearts.app.feature.globe.domain.model.ArcLeg
import com.twinhearts.app.feature.globe.domain.model.CityGroup
import com.twinhearts.app.feature.globe.domain.model.CountryGlow
import com.twinhearts.app.feature.globe.domain.model.GeoPoint
import com.twinhearts.app.feature.globe.domain.wrapLongitude
import com.twinhearts.app.feature.globe.ui.CameraRequest
import com.twinhearts.app.feature.globe.ui.LiveUi
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/*
 * Why this file exists: the 3D globe. Cesium (used by the web app) does not exist on Android, so the
 * ball is drawn here: country shapes from the bundled map, a graticule, an atmosphere, glowing
 * visited countries, lifted trip arcs, heart pins that merge into number bubbles when they crowd,
 * and live-location dots. One finger turns it (with inertia), two fingers zoom, a tap on a pin opens it.
 */

private const val RAD_TO_DEG = 180.0 / PI
private const val AUTO_ROTATE_DEG_PER_SEC = 4.1
private const val FLY_MS = 1400L
private const val BALL_FRACTION = 0.78f
private const val CENTER_Y = 0.47f
private const val CLUSTER_DP = 60f
private const val PIN_DP = 52f
private const val PIN_SELECTED_DP = 66f
private const val CLUSTER_ICON_DP = 54f
private const val LABEL_MIN_ZOOM = 2.4
private const val MAX_FLING_DEG_PER_SEC = 240.0
private const val FRICTION = 3.2
private const val CLUSTER_ZOOM_STEP = 2.4

/** Things that change every frame but must not cause a new composition. */
private class GlobeAnim {
    var flight: Flight? = null
    var flightStartNanos = -1L
    var flightId = -1L
    var touching = false
    var vLon = 0.0
    var vLat = 0.0
    var hits: List<Hit> = emptyList()
}

private class Hit(val x: Float, val y: Float, val key: String?, val cluster: PinCluster?)

private class Star(val x: Float, val y: Float, val r: Float, val a: Float)

private class LegPath(val xs: FloatArray, val ys: FloatArray, val zs: FloatArray, val lift: Float, val color: Color)

private fun ballRadius(width: Float, height: Float, zoom: Double): Float =
    min(width, height) / 2f * BALL_FRACTION * zoom.toFloat()

@Composable
fun GlobeCanvas(
    groups: List<CityGroup>,
    legs: List<ArcLeg>,
    glows: List<CountryGlow>,
    atlas: WorldAtlas?,
    selectedKey: String?,
    live: LiveUi,
    searchPin: GeoPoint?,
    camera: CameraRequest?,
    onPinTap: (String) -> Unit,
    onCameraArrived: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = rememberGlobePalette()
    val art = remember(palette) { PinArt(palette) }
    val density = LocalDensity.current.density
    val reduceMotion = rememberReduceMotion()
    val describe = stringResource(R.string.globe_globe_cd)

    var cam by remember { mutableStateOf(GlobeCamera.Default) }
    var autoRotate by remember { mutableStateOf(true) }
    val anim = remember { GlobeAnim() }
    val currentPinTap by rememberUpdatedState(onPinTap)
    val currentArrived by rememberUpdatedState(onCameraArrived)

    val stars = remember {
        val rnd = kotlin.random.Random(STAR_SEED)
        List(STAR_COUNT) { Star(rnd.nextFloat(), rnd.nextFloat(), 0.4f + rnd.nextFloat() * 1.1f, 0.25f + rnd.nextFloat() * 0.6f) }
    }
    val graticule = remember { buildGraticule() }
    val visited: List<Country> = remember(glows, atlas) { glows.mapNotNull { atlas?.find(it.country) } }
    val legPaths = remember(legs, palette) { legs.map { buildLegPath(it, palette.arcs[it.index % palette.arcs.size]) } }
    val clipper = remember { RingClipper() }
    val landPath = remember { Path() }
    val visitedPath = remember { Path() }
    val linePath = remember { Path() }
    val labelFill = remember(palette) { Paint(Paint.ANTI_ALIAS_FLAG).apply { color = palette.label; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD } }
    val labelHalo = remember(palette) {
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = palette.labelHalo
            textAlign = Paint.Align.CENTER
            typeface = Typeface.DEFAULT_BOLD
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
        }
    }

    // A request to fly somewhere (a tapped pin, a search result, "Reset view").
    LaunchedEffect(camera?.id) {
        val request = camera ?: return@LaunchedEffect
        if (request.resetSpin) {
            autoRotate = true
            anim.vLon = 0.0
            anim.vLat = 0.0
        }
        anim.flight = Flight(cam, GlobeCamera(request.lat, request.lng, request.globeZoom).normalized(), FLY_MS)
        anim.flightStartNanos = -1L
        anim.flightId = request.id
    }

    // One loop drives flights, inertia and the slow idle spin.
    LaunchedEffect(Unit) {
        var last = 0L
        while (true) {
            androidx.compose.runtime.withFrameNanos { now ->
                val dt = if (last == 0L) 0.0 else ((now - last) / NANOS).coerceAtMost(MAX_DT)
                last = now
                val flight = anim.flight
                if (flight != null) {
                    if (anim.flightStartNanos < 0) anim.flightStartNanos = now
                    val elapsedMs = (now - anim.flightStartNanos) / 1_000_000L
                    cam = flight.at(elapsedMs)
                    if (elapsedMs >= flight.durationMs) {
                        anim.flight = null
                        if (anim.flightId >= 0) currentArrived(anim.flightId)
                        anim.flightId = -1L
                    }
                } else if (!anim.touching) {
                    if (abs(anim.vLon) > MIN_FLING || abs(anim.vLat) > MIN_FLING) {
                        cam = GlobeCamera(cam.lat + anim.vLat * dt, cam.lon + anim.vLon * dt, cam.zoom).normalized()
                        val keep = exp(-FRICTION * dt)
                        anim.vLon *= keep
                        anim.vLat *= keep
                    } else if (autoRotate && !reduceMotion) {
                        cam = GlobeCamera(cam.lat, wrapLongitude(cam.lon + AUTO_ROTATE_DEG_PER_SEC * dt), cam.zoom)
                    }
                }
            }
        }
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .semantics { contentDescription = describe }
            .pointerInput(groups) {
                val tracker = VelocityTracker()
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    anim.touching = true
                    anim.flight = null
                    anim.flightId = -1L
                    anim.vLon = 0.0
                    anim.vLat = 0.0
                    autoRotate = false
                    tracker.resetTracking()
                    tracker.addPosition(down.uptimeMillis, down.position)
                    var moved = 0f
                    var dragging = false
                    do {
                        val event = awaitPointerEvent()
                        val pan = event.calculatePan()
                        val zoomChange = event.calculateZoom()
                        moved += pan.getDistance()
                        if (!dragging && (moved > viewConfiguration.touchSlop || zoomChange != 1f)) dragging = true
                        if (dragging) {
                            val r = ballRadius(size.width.toFloat(), size.height.toFloat(), cam.zoom).toDouble()
                            val cosLat = max(cos(Math.toRadians(cam.lat)), MIN_COS_LAT)
                            cam = GlobeCamera(
                                cam.lat + pan.y / r * RAD_TO_DEG,
                                cam.lon - pan.x / r * RAD_TO_DEG / cosLat,
                                cam.zoom * zoomChange,
                            ).normalized()
                            event.changes.forEach { it.consume() }
                        }
                        event.changes.firstOrNull()?.let { tracker.addPosition(it.uptimeMillis, it.position) }
                    } while (event.changes.any { it.pressed })
                    anim.touching = false
                    if (!dragging) {
                        val hit = anim.hits.minByOrNull { hypot(it.x - down.position.x, it.y - down.position.y) }
                        val reach = HIT_DP * density
                        if (hit != null && hypot(hit.x - down.position.x, hit.y - down.position.y) <= reach) {
                            val key = hit.key
                            val cluster = hit.cluster
                            if (key != null) {
                                currentPinTap(key)
                            } else if (cluster != null) {
                                val members = cluster.members.mapNotNull { m -> groups.firstOrNull { it.key == m.key } }
                                if (members.isNotEmpty()) {
                                    val target = GlobeCamera(
                                        members.map { it.lat }.average(),
                                        members.map { it.lng }.average(),
                                        min(GlobeCamera.MAX_ZOOM, cam.zoom * CLUSTER_ZOOM_STEP),
                                    )
                                    anim.flight = Flight(cam, target.normalized(), FLY_MS)
                                    anim.flightStartNanos = -1L
                                }
                            }
                        }
                    } else {
                        val v = tracker.calculateVelocity()
                        val r = ballRadius(size.width.toFloat(), size.height.toFloat(), cam.zoom).toDouble()
                        val cosLat = max(cos(Math.toRadians(cam.lat)), MIN_COS_LAT)
                        anim.vLon = (-v.x / r * RAD_TO_DEG / cosLat).coerceIn(-MAX_FLING_DEG_PER_SEC, MAX_FLING_DEG_PER_SEC)
                        anim.vLat = (v.y / r * RAD_TO_DEG).coerceIn(-MAX_FLING_DEG_PER_SEC, MAX_FLING_DEG_PER_SEC)
                    }
                }
            },
    ) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h * CENTER_Y)
        val current = cam
        val radius = ballRadius(w, h, current.zoom)
        val view = GlobeView(current.lat, current.lon)

        // Stars
        stars.forEach { s -> drawCircle(palette.star.copy(alpha = s.a * 0.5f), s.r * density, Offset(s.x * w, s.y * h)) }

        // Atmosphere glow
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0.0f to Color.Transparent, 0.79f to Color.Transparent, 0.82f to palette.atmosphere.copy(alpha = 0.5f), 1f to Color.Transparent),
                center = center,
                radius = radius * 1.25f,
            ),
            radius = radius * 1.25f,
            center = center,
        )

        // Ocean
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(palette.oceanLight, palette.oceanDeep),
                center = Offset(center.x - radius * 0.3f, center.y - radius * 0.35f),
                radius = radius * 1.5f,
            ),
            radius = radius,
            center = center,
        )

        drawGraticule(graticule, view, center, radius, linePath, palette.graticule, density)
        if (atlas != null) drawLand(atlas, visited, view, center, radius, clipper, landPath, visitedPath, palette, density)

        // Round shading and rim light
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0.0f to Color.Transparent, 0.6f to Color.Transparent, 1f to palette.limbShade),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
        drawCircle(palette.atmosphere.copy(alpha = 0.35f), radius, center, style = Stroke(1.5f * density))

        drawGlows(glows, view, center, radius, palette, density)
        drawLegs(legPaths, view, center, radius, linePath, density)
        drawLive(live, view, center, radius, linePath, palette, density)

        val projected = Vec3()
        if (searchPin != null) {
            view.project(searchPin.lat, searchPin.lng, projected)
            if (projected.z > MIN_Z) {
                val p = Offset(center.x + projected.x * radius, center.y - projected.y * radius)
                drawCircle(palette.search.copy(alpha = 0.3f), 14f * density, p)
                drawCircle(palette.search, 6f * density, p)
                drawCircle(palette.outline, 6f * density, p, style = Stroke(2f * density))
            }
        }

        anim.hits = drawPins(groups, selectedKey, current, view, center, radius, art, palette, labelFill, labelHalo, density)
    }
}

private const val NANOS = 1_000_000_000.0
private const val MAX_DT = 0.05
private const val MIN_FLING = 2.0
private const val MIN_COS_LAT = 0.25
private const val MIN_Z = 0.04f
private const val HIT_DP = 34f
private const val STAR_SEED = 7
private const val STAR_COUNT = 110

// ── drawing helpers ──────────────────────────────────────────────────────────────────────────

private fun buildGraticule(): List<FloatArray> {
    val lines = mutableListOf<FloatArray>()
    fun line(points: List<Pair<Double, Double>>) {
        val a = FloatArray(points.size * 3)
        points.forEachIndexed { i, (lat, lon) ->
            val phi = Math.toRadians(lat)
            val lam = Math.toRadians(lon)
            a[i * 3] = (cos(phi) * cos(lam)).toFloat()
            a[i * 3 + 1] = (cos(phi) * sin(lam)).toFloat()
            a[i * 3 + 2] = sin(phi).toFloat()
        }
        lines += a
    }
    for (lat in -60..60 step 30) line((0..72).map { lat.toDouble() to it * 5.0 })
    for (lon in 0 until 360 step 30) line((-18..18).map { it * 5.0 to lon.toDouble() })
    return lines
}

private fun DrawScope.drawGraticule(lines: List<FloatArray>, view: GlobeView, center: Offset, radius: Float, path: Path, color: Color, density: Float) {
    path.rewind()
    for (a in lines) {
        var prevFront = false
        for (i in 0 until a.size / 3) {
            val px = a[i * 3]
            val py = a[i * 3 + 1]
            val pz = a[i * 3 + 2]
            val front = view.z(px, py, pz) > 0f
            if (front) {
                val x = center.x + view.x(px, py) * radius
                val y = center.y - view.y(px, py, pz) * radius
                if (prevFront) path.lineTo(x, y) else path.moveTo(x, y)
            }
            prevFront = front
        }
    }
    drawPath(path, color, style = Stroke(1f * density))
}

private fun DrawScope.drawLand(
    atlas: WorldAtlas,
    visited: List<Country>,
    view: GlobeView,
    center: Offset,
    radius: Float,
    clipper: RingClipper,
    landPath: Path,
    visitedPath: Path,
    palette: GlobePalette,
    density: Float,
) {
    landPath.rewind()
    visitedPath.rewind()
    for (country in atlas.countries) {
        val target = if (visited.any { it === country }) visitedPath else landPath
        for (ring in country.rings) {
            val n = clipper.clip(ring, view)
            if (n < 3) continue
            target.moveTo(center.x + clipper.xs[0] * radius, center.y - clipper.ys[0] * radius)
            for (i in 1 until n) target.lineTo(center.x + clipper.xs[i] * radius, center.y - clipper.ys[i] * radius)
            target.close()
        }
    }
    drawPath(landPath, palette.land)
    drawPath(visitedPath, palette.landVisited)
    drawPath(landPath, palette.border, style = Stroke(0.7f * density, join = StrokeJoin.Round))
    drawPath(visitedPath, palette.border.copy(alpha = 0.45f), style = Stroke(0.9f * density, join = StrokeJoin.Round))
}

private fun DrawScope.drawGlows(glows: List<CountryGlow>, view: GlobeView, center: Offset, radius: Float, palette: GlobePalette, density: Float) {
    val v = Vec3()
    for (g in glows) {
        view.project(g.center.lat, g.center.lng, v)
        if (v.z <= MIN_Z) continue
        val p = Offset(center.x + v.x * radius, center.y - v.y * radius)
        val r = 64f * density
        drawCircle(
            brush = Brush.radialGradient(
                colorStops = arrayOf(0f to palette.glow.copy(alpha = 0.42f), 0.55f to palette.glow.copy(alpha = 0.16f), 1f to Color.Transparent),
                center = p,
                radius = r,
            ),
            radius = r,
            center = p,
        )
    }
}

private fun buildLegPath(leg: ArcLeg, color: Color): LegPath {
    val steps = (leg.distanceKm / 120.0).toInt().coerceIn(10, 72)
    val points = greatCircle(leg.from, leg.to, steps)
    val n = points.size
    val xs = FloatArray(n)
    val ys = FloatArray(n)
    val zs = FloatArray(n)
    points.forEachIndexed { i, p ->
        val phi = Math.toRadians(p.lat)
        val lam = Math.toRadians(p.lng)
        xs[i] = (cos(phi) * cos(lam)).toFloat()
        ys[i] = (cos(phi) * sin(lam)).toFloat()
        zs[i] = sin(phi).toFloat()
    }
    val lift = min(0.22, leg.distanceKm / 6371.0 * 0.3).toFloat()
    return LegPath(xs, ys, zs, lift, color)
}

private fun DrawScope.drawLegs(legs: List<LegPath>, view: GlobeView, center: Offset, radius: Float, path: Path, density: Float) {
    for (leg in legs) {
        path.rewind()
        val n = leg.xs.size
        var prevFront = false
        for (i in 0 until n) {
            val z = view.z(leg.xs[i], leg.ys[i], leg.zs[i])
            val front = z > 0f
            if (front) {
                val t = i.toFloat() / (n - 1)
                val scale = 1f + leg.lift * sin(PI.toFloat() * t)
                val x = center.x + view.x(leg.xs[i], leg.ys[i]) * radius * scale
                val y = center.y - view.y(leg.xs[i], leg.ys[i], leg.zs[i]) * radius * scale
                if (prevFront) path.lineTo(x, y) else path.moveTo(x, y)
            }
            prevFront = front
        }
        drawPath(path, leg.color.copy(alpha = 0.28f), style = Stroke(7f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
        drawPath(path, leg.color.copy(alpha = 0.95f), style = Stroke(2.4f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

private fun DrawScope.drawLive(live: LiveUi, view: GlobeView, center: Offset, radius: Float, path: Path, palette: GlobePalette, density: Float) {
    if (!live.enabled) return
    fun trail(points: List<GeoPoint>, color: Color) {
        if (points.size < 2) return
        path.rewind()
        val v = Vec3()
        var prevFront = false
        for (p in points) {
            view.project(p.lat, p.lng, v)
            val front = v.z > 0f
            if (front) {
                val x = center.x + v.x * radius
                val y = center.y - v.y * radius
                if (prevFront) path.lineTo(x, y) else path.moveTo(x, y)
            }
            prevFront = front
        }
        drawPath(path, color.copy(alpha = 0.8f), style = Stroke(3f * density, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
    fun dot(person: com.twinhearts.app.feature.globe.domain.model.LivePerson?, color: Color) {
        if (person == null || !person.online) return
        val v = Vec3()
        view.project(person.point.lat, person.point.lng, v)
        if (v.z <= MIN_Z) return
        val p = Offset(center.x + v.x * radius, center.y - v.y * radius)
        drawCircle(color.copy(alpha = 0.3f), 15f * density, p)
        drawCircle(color, 8f * density, p)
        drawCircle(palette.outline, 8f * density, p, style = Stroke(2.5f * density))
    }
    trail(live.myTrail, palette.me)
    trail(live.partnerTrail, palette.partner)
    dot(live.me, palette.me)
    dot(live.partner, palette.partner)
}

private fun DrawScope.drawPins(
    groups: List<CityGroup>,
    selectedKey: String?,
    cam: GlobeCamera,
    view: GlobeView,
    center: Offset,
    radius: Float,
    art: PinArt,
    palette: GlobePalette,
    labelFill: Paint,
    labelHalo: Paint,
    density: Float,
): List<Hit> {
    val v = Vec3()
    val visible = ArrayList<PinPoint>(groups.size)
    for (g in groups) {
        view.project(g.lat, g.lng, v)
        if (v.z > MIN_Z) visible += PinPoint(g.key, center.x + v.x * radius, center.y - v.y * radius, g.count)
    }
    val clusters = clusterPins(visible, CLUSTER_DP * density)
    val hits = ArrayList<Hit>(clusters.size)
    val byKey = groups.associateBy { it.key }
    val textSize = 12.5f * density
    labelFill.textSize = textSize
    labelHalo.textSize = textSize
    labelHalo.strokeWidth = 3.5f * density

    // Lower pins first so higher ones are not covered by them; the selected pin always last.
    val ordered = clusters.sortedBy { c -> if (c.members.size == 1 && c.members[0].key == selectedKey) Float.MAX_VALUE else c.y }
    for (c in ordered) {
        if (c.members.size > 1) {
            val size = CLUSTER_ICON_DP * density
            val img = art.clusterImage(c.members.size)
            drawImage(
                image = img,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset((c.x - size / 2).toInt(), (c.y - size / 2).toInt()),
                dstSize = IntSize(size.toInt(), size.toInt()),
            )
            hits += Hit(c.x, c.y, null, c)
        } else {
            val m = c.members[0]
            val g = byKey[m.key] ?: continue
            val selected = m.key == selectedKey
            val size = (if (selected) PIN_SELECTED_DP else PIN_DP) * density
            val img = art.pinImage(g.emoji, g.count, selected)
            val top = c.y - size * PinArt.TIP_FRACTION
            drawImage(
                image = img,
                srcOffset = IntOffset.Zero,
                srcSize = IntSize(img.width, img.height),
                dstOffset = IntOffset((c.x - size / 2).toInt(), top.toInt()),
                dstSize = IntSize(size.toInt(), size.toInt()),
            )
            if (cam.zoom >= LABEL_MIN_ZOOM) {
                val y = c.y + textSize + 4f * density
                drawContext.canvas.nativeCanvas.drawText(g.city, c.x, y, labelHalo)
                drawContext.canvas.nativeCanvas.drawText(g.city, c.x, y, labelFill)
            }
            hits += Hit(c.x, c.y - size * 0.45f, m.key, null)
        }
    }
    return hits
}
