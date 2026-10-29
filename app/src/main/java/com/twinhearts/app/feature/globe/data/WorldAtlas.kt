package com.twinhearts.app.feature.globe.data

import com.twinhearts.app.feature.globe.domain.GeoRing
import java.io.DataInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale

/*
 * Why this file exists: reads the world map that ships inside the app (assets/globe_countries.bin).
 * The file holds the country outlines (Natural Earth, public domain, simplified) plus every
 * country's two-letter code and its common names, so memories saved as "UAE" or "United States"
 * can be matched to a shape and a flag. No network is needed to show the globe.
 */

/** One country: its code, names and outlines (small islands and city-states have none). */
class Country(val name: String, val iso2: String, val aliases: List<String>, val rings: List<GeoRing>)

class WorldAtlas(val countries: List<Country>) {
    private val byName: Map<String, Country> = buildMap {
        countries.forEach { c ->
            put(normalize(c.name), c)
            c.aliases.forEach { putIfAbsent(normalize(it), c) }
        }
    }

    /** The country a memory's "country" text means, or null when unknown. */
    fun find(name: String): Country? {
        val key = normalize(name)
        if (key.isEmpty()) return null
        return byName[key] ?: byName[key.removePrefix("the ")]
    }

    companion object {
        private const val MAGIC = 0x54474C42 // "TGLB"
        private const val DEGREES_SCALE = 100f

        fun normalize(text: String): String = text.trim().lowercase(Locale.ROOT)

        /** Reads the binary file. Throws [IOException] when it is damaged. */
        fun parse(input: InputStream): WorldAtlas {
            val data = DataInputStream(input.buffered())
            if (data.readInt() != MAGIC) throw IOException("Not a globe map file")
            data.readUnsignedShort() // format version
            val total = data.readUnsignedShort()
            val countries = ArrayList<Country>(total)
            repeat(total) {
                val name = readText(data)
                val iso = ByteArray(2).also { data.readFully(it) }.toString(Charsets.US_ASCII).trim()
                val aliases = List(data.readUnsignedByte()) { readText(data) }
                val rings = List(data.readUnsignedShort()) {
                    val points = data.readUnsignedShort()
                    val lon = FloatArray(points)
                    val lat = FloatArray(points)
                    for (i in 0 until points) {
                        lon[i] = data.readShort() / DEGREES_SCALE
                        lat[i] = data.readShort() / DEGREES_SCALE
                    }
                    GeoRing.fromDegrees(lon, lat)
                }
                countries += Country(name, iso, aliases, rings)
            }
            return WorldAtlas(countries)
        }

        private fun readText(data: DataInputStream): String {
            val bytes = ByteArray(data.readUnsignedByte())
            data.readFully(bytes)
            return String(bytes, Charsets.UTF_8)
        }
    }
}
