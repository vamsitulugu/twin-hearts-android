package com.twinhearts.app.feature.globe

import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.model.MemoryMedia

/** Small builders so each test only states what it cares about. */
fun memory(
    id: String,
    city: String,
    country: String,
    lat: Double,
    lng: Double,
    from: String? = null,
    mood: String = "🥰 Romantic",
    cost: Double? = null,
    media: List<MemoryMedia> = emptyList(),
) = Memory(
    id = id, city = city, country = country, state = "", lat = lat, lng = lng, tripName = "", dateFrom = from, dateTo = null,
    mood = mood, weather = "", temperature = "", tripCost = cost, currency = "INR", notes = "", favoriteMoment = "",
    restaurants = emptyList(), hotels = emptyList(), gifts = emptyList(), media = media,
)

fun media(id: String, kind: MediaKind, url: String = "https://x/$id") = MemoryMedia(id, kind, url, "", "")
