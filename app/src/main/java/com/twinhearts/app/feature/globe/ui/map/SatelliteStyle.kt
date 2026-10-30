package com.twinhearts.app.feature.globe.ui.map

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/*
 * Why this file exists: the web app's street level is satellite imagery with place names, roads and 3D
 * buildings. This builds the same look for MapLibre: it downloads the free OpenFreeMap "liberty" style
 * (so fonts, icons and tile addresses are always the right ones), drops its flat painted land and water,
 * puts Esri satellite pictures under it, keeps roads / borders / names (white text), and keeps or adds
 * 3D buildings. If anything fails, the caller falls back to the plain liberty style.
 */

private const val LIBERTY_URL = "https://tiles.openfreemap.org/styles/liberty"
private const val SATELLITE_TILES = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
private const val SATELLITE_SOURCE = "gm-satellite"
private const val SATELLITE_ATTRIBUTION = "Imagery © Esri, Maxar, Earthstar Geographics"
private const val TIMEOUT_MS = 12_000
private const val BUILDING_COLOR = "#C9CCD2"
private val KEEP_LINES = setOf("transportation", "boundary")
private val KEEP_LABELS = setOf("place", "transportation_name", "water_name")

object SatelliteStyle {
    @Volatile private var cached: String? = null

    /**
     * The satellite style as JSON, or null when it could not be built (offline, server change).
     * Remembered after the first success, and fetched when the Globe page opens, so zooming in is instant.
     */
    suspend fun load(): String? {
        cached?.let { return it }
        return withContext(Dispatchers.IO) {
            try {
                transform(download()).also { cached = it }
            } catch (e: java.io.IOException) {
                android.util.Log.w("GlobeMap", "Satellite style download failed: ${e.message}")
                null
            } catch (e: org.json.JSONException) {
                android.util.Log.w("GlobeMap", "Satellite style could not be built: ${e.message}")
                null
            }
        }
    }

    private fun download(): String {
        val connection = URL(LIBERTY_URL).openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        try {
            return connection.inputStream.bufferedReader().use { it.readText() }
        } finally {
            connection.disconnect()
        }
    }

    internal fun transform(json: String): String {
        val root = JSONObject(json)
        val sources = root.getJSONObject("sources")
        var vectorId: String? = null
        val ids = sources.keys()
        while (ids.hasNext() && vectorId == null) {
            val id = ids.next()
            if (sources.getJSONObject(id).optString("type") == "vector") vectorId = id
        }
        sources.put(
            SATELLITE_SOURCE,
            JSONObject()
                .put("type", "raster")
                .put("tiles", JSONArray().put(SATELLITE_TILES))
                .put("tileSize", 256)
                .put("maxzoom", 19)
                .put("attribution", SATELLITE_ATTRIBUTION),
        )

        val out = JSONArray()
        out.put(JSONObject().put("id", "gm-satellite-layer").put("type", "raster").put("source", SATELLITE_SOURCE))
        var hasBuildings = false
        val layers = root.getJSONArray("layers")
        for (i in 0 until layers.length()) {
            val layer = layers.getJSONObject(i)
            val sourceLayer = layer.optString("source-layer")
            val paint = layer.optJSONObject("paint") ?: JSONObject().also { layer.put("paint", it) }
            when (layer.optString("type")) {
                "fill-extrusion" -> {
                    hasBuildings = true
                    layer.put("minzoom", 13)
                    paint.put("fill-extrusion-color", BUILDING_COLOR)
                    paint.put("fill-extrusion-opacity", 0.88)
                    out.put(layer)
                }
                "line" -> if (sourceLayer in KEEP_LINES) {
                    paint.put("line-opacity", if (sourceLayer == "boundary") 0.85 else 0.62)
                    if (sourceLayer == "boundary") paint.put("line-color", "#FFFFFF")
                    out.put(layer)
                }
                "symbol" -> if (sourceLayer in KEEP_LABELS) {
                    paint.put("text-color", "#FFFFFF")
                    paint.put("text-halo-color", "rgba(0,0,0,0.78)")
                    paint.put("text-halo-width", 1.6)
                    out.put(layer)
                }
                else -> Unit // painted land, water and flat buildings: the satellite picture replaces them
            }
        }
        if (!hasBuildings && vectorId != null) out.put(ownBuildings(vectorId))
        root.put("layers", out)
        root.put("name", "Twin Hearts satellite")
        return root.toString()
    }

    private fun ownBuildings(vectorId: String): JSONObject = JSONObject()
        .put("id", "gm-buildings-3d")
        .put("type", "fill-extrusion")
        .put("source", vectorId)
        .put("source-layer", "building")
        .put("minzoom", 13)
        .put(
            "paint",
            JSONObject()
                .put("fill-extrusion-color", BUILDING_COLOR)
                .put("fill-extrusion-opacity", 0.88)
                .put("fill-extrusion-height", JSONArray("[\"coalesce\",[\"get\",\"render_height\"],10]"))
                .put("fill-extrusion-base", JSONArray("[\"coalesce\",[\"get\",\"render_min_height\"],0]")),
        )
}
