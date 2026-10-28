package com.twinhearts.app.feature.meetplanner.data.remote

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * Why this file exists: answers of the free public map services the web app also uses
 * (Nominatim for city/place names, Photon for typing suggestions, OSRM for driving routes).
 */

@Serializable
data class NominatimDto(
    @SerialName("place_id") val placeId: Long? = null,
    @SerialName("display_name") val displayName: String? = null,
    val lat: String? = null,
    val lon: String? = null,
    val address: Map<String, String>? = null,
)

@Serializable
data class PhotonDto(val features: List<PhotonFeatureDto> = emptyList())

@Serializable
data class PhotonFeatureDto(
    val geometry: PhotonGeometryDto? = null,
    val properties: PhotonPropsDto? = null,
)

@Serializable
data class PhotonGeometryDto(val coordinates: List<Double> = emptyList())

@Serializable
data class PhotonPropsDto(
    val name: String? = null,
    val street: String? = null,
    val city: String? = null,
    val state: String? = null,
    val country: String? = null,
    @SerialName("osm_type") val osmType: String? = null,
    @SerialName("osm_id") val osmId: Long? = null,
)

@Serializable
data class OsrmDto(val routes: List<OsrmRouteDto>? = null)

@Serializable
data class OsrmRouteDto(
    val distance: Double = 0.0,
    val duration: Double = 0.0,
    val geometry: RouteGeometryDto? = null,
    val legs: List<OsrmLegDto> = emptyList(),
)

@Serializable
data class OsrmLegDto(val distance: Double = 0.0, val duration: Double = 0.0)
