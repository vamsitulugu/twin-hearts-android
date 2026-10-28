package com.twinhearts.app.feature.livemap.ui.map

import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * The five map types of the web app (Map, Dark, Satellite, Hybrid, Auto), as free MapLibre styles (R-11, R-20).
 * Street is the OpenFreeMap vector style (OpenStreetMap data, no key). The others are raster tiles,
 * exactly the providers the web app already uses (CARTO dark, Esri imagery).
 */
object MapStyles {

    private const val STREET_URL = "https://tiles.openfreemap.org/styles/liberty"

    /** Glyph files for the place-name labels on the raster styles (they carry no fonts of their own). */
    private const val GLYPHS = "https://tiles.openfreemap.org/fonts/{fontstack}/{range}.pbf"

    private const val DARK_TILES = "https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"
    private const val SAT_TILES = "https://server.arcgisonline.com/ArcGIS/rest/services/World_Imagery/MapServer/tile/{z}/{y}/{x}"
    private const val LABEL_TILES =
        "https://server.arcgisonline.com/ArcGIS/rest/services/Reference/World_Boundaries_and_Places/MapServer/tile/{z}/{y}/{x}"

    /** What to load: either a style address or a ready JSON string. */
    sealed interface Source {
        data class Url(val value: String) : Source
        data class Json(val value: String) : Source
    }

    /** [nightNow] decides what "Auto" means: dark map in the evening and at night. */
    fun sourceFor(kind: MapStyleKind, nightNow: Boolean): Source = when (kind) {
        MapStyleKind.Street -> Source.Url(STREET_URL)
        MapStyleKind.Dark -> Source.Json(rasterStyle(DARK_TILES, "© OpenStreetMap, © CARTO"))
        MapStyleKind.Satellite -> Source.Json(rasterStyle(SAT_TILES, "Tiles © Esri"))
        MapStyleKind.Hybrid -> Source.Json(rasterStyle(SAT_TILES, "Tiles © Esri", LABEL_TILES))
        MapStyleKind.Auto -> sourceFor(if (nightNow) MapStyleKind.Dark else MapStyleKind.Street, nightNow)
    }

    /** True from 19:00 to 05:59. */
    fun isNight(hourOfDay: Int): Boolean = hourOfDay >= 19 || hourOfDay < 6

    /** Builds the style document with a JSON builder, so no quote can ever be mistyped. */
    private fun rasterStyle(baseTiles: String, attribution: String, labelTiles: String? = null): String {
        fun rasterSource(tiles: String, withAttribution: Boolean) = buildJsonObject {
            put("type", "raster")
            putJsonArray("tiles") { add(tiles) }
            put("tileSize", 256)
            put("maxzoom", 19)
            if (withAttribution) put("attribution", attribution)
        }
        return buildJsonObject {
            put("version", 8)
            put("glyphs", GLYPHS)
            putJsonObject("sources") {
                put("base", rasterSource(baseTiles, true))
                if (labelTiles != null) put("labels", rasterSource(labelTiles, false))
            }
            putJsonArray("layers") {
                add(
                    buildJsonObject {
                        put("id", "base-layer")
                        put("type", "raster")
                        put("source", "base")
                    },
                )
                if (labelTiles != null) {
                    add(
                        buildJsonObject {
                            put("id", "labels-layer")
                            put("type", "raster")
                            put("source", "labels")
                        },
                    )
                }
            }
        }.toString()
    }
}
