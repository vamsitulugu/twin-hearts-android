package com.twinhearts.app.feature.livemap

import com.twinhearts.app.feature.livemap.data.mapper.parseIsoMs
import com.twinhearts.app.feature.livemap.data.mapper.toAlertPrefs
import com.twinhearts.app.feature.livemap.data.mapper.toCouple
import com.twinhearts.app.feature.livemap.data.mapper.toDrivingRoute
import com.twinhearts.app.feature.livemap.data.mapper.toPlaces
import com.twinhearts.app.feature.livemap.data.mapper.toPlacesJson
import com.twinhearts.app.feature.livemap.data.mapper.toRouteDay
import com.twinhearts.app.feature.livemap.data.remote.LocationRowDto
import com.twinhearts.app.feature.livemap.data.remote.LocationsResponse
import com.twinhearts.app.feature.livemap.data.remote.OsrmGeometryDto
import com.twinhearts.app.feature.livemap.data.remote.OsrmResponse
import com.twinhearts.app.feature.livemap.data.remote.OsrmRouteDto
import com.twinhearts.app.feature.livemap.data.remote.RouteDayResponse
import com.twinhearts.app.feature.livemap.data.remote.RoutePointDto
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveMapMapperTest {

    @Test
    fun parseIso_acceptsZAndOffset_andRejectsJunk() {
        assertEquals(parseIsoMs("2026-10-27T10:00:00Z"), parseIsoMs("2026-10-27T10:00:00+00:00"))
        assertNull(parseIsoMs("nonsense"))
        assertNull(parseIsoMs(null))
    }

    @Test
    fun locations_mapMyRoleToMe_andOtherToPartner() {
        val response = LocationsResponse(
            user1 = LocationRowDto(lat = 1.0, lng = 2.0, online = true, vehicleType = "car"),
            user2 = LocationRowDto(lat = 3.0, lng = 4.0, status = "paused", emergency = true),
        )
        val asUser2 = response.toCouple("user2")
        assertEquals(3.0, asUser2.me!!.lat, 0.0)
        assertTrue(asUser2.me!!.paused)
        assertTrue(asUser2.me!!.emergency)
        assertEquals(Vehicle.Car, asUser2.partner!!.vehicle)
        assertTrue(asUser2.partner!!.online)
    }

    @Test
    fun locationRowWithoutCoordinates_isNull() {
        assertNull(LocationsResponse(user1 = LocationRowDto(lat = null, lng = 5.0)).toCouple("user1").me)
    }

    @Test
    fun routeDay_dropsPointsWithoutCoordinates() {
        val day = RouteDayResponse(
            points = listOf(RoutePointDto(1.0, 2.0, 1.5, "2026-10-27T10:00:00Z"), RoutePointDto(null, 2.0)),
        ).toRouteDay("2026-10-27")
        assertEquals(1, day.points.size)
        assertEquals("2026-10-27", day.date)
        assertEquals(0.0, day.distanceKm, 0.0)
    }

    @Test
    fun osrm_swapsLngLatToLatLng_andHandlesNoRoute() {
        val route = OsrmResponse(listOf(OsrmRouteDto(1200.0, 300.0, OsrmGeometryDto(listOf(listOf(80.6, 16.5), listOf(80.7, 16.6)))))).toDrivingRoute()
        assertNotNull(route)
        assertEquals(16.5 to 80.6, route!!.line.first())
        assertNull(OsrmResponse().toDrivingRoute())
    }

    @Test
    fun places_roundTripThroughStateJson() {
        val json = Json.parseToJsonElement(
            """{"placesList":[{"id":"p1","owner":"user2","cat":"Home","name":"Home","lat":16.5,"lng":80.6,"address":"Vijayawada","ts":123},{"id":"bad"}]}""",
        ).jsonObject
        val places = json.toPlaces()
        assertEquals(1, places.size) // the row without coordinates is skipped
        assertEquals("user2", places[0].ownerRole)
        val back = JsonObject(mapOf("placesList" to places.toPlacesJson())).toPlaces()
        assertEquals(places, back)
    }

    @Test
    fun alertPrefs_missingKeyMeansOn_onlyFalseTurnsOff() {
        assertTrue(JsonObject(emptyMap()).toAlertPrefs().arrive)
        val prefs = Json.parseToJsonElement("""{"lmAlerts":{"arrive":false,"leave":true}}""").jsonObject.toAlertPrefs()
        assertFalse(prefs.arrive)
        assertTrue(prefs.leave)
        assertTrue(prefs.battery)
    }
}
