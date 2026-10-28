package com.twinhearts.app.feature.livemap.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.twinhearts.app.feature.livemap.domain.model.MapStyleKind
import com.twinhearts.app.feature.livemap.domain.model.Vehicle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Settings of this phone only (R-07): sharing switch, map look and the "tell my partner" alert switches. */
data class LiveMapSettings(
    /** Null until the person has decided once; permission granted for the first time turns it on. */
    val sharing: Boolean?,
    val invisible: Boolean,
    /** Share a position rounded to about 1 km instead of the exact one. */
    val approximate: Boolean,
    /** Epoch millis until which sharing is paused. 0 = not paused, [MANUAL_PAUSE] = until resumed by hand. */
    val pausedUntilMs: Long,
    val vehicle: Vehicle,
    val mapStyle: MapStyleKind,
    val threeD: Boolean,
    val followPartner: Boolean,
    val showRouteLine: Boolean,
    val showPlaceLabels: Boolean,
    val showWeather: Boolean,
    /** Mirrors of the three "Let partner know when I…" switches, so the background service can read them. */
    val tellBattery: Boolean,
    val tellNetwork: Boolean,
    val tellGps: Boolean,
) {
    val isPaused: Boolean get() = pausedUntilMs != 0L && System.currentTimeMillis() < pausedUntilMs

    companion object {
        const val MANUAL_PAUSE = Long.MAX_VALUE
    }
}

@Singleton
class LiveMapPrefs @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) {
    val settings: Flow<LiveMapSettings> = dataStore.data.map { p ->
        LiveMapSettings(
            sharing = p[SHARING],
            invisible = p[INVISIBLE] ?: false,
            approximate = p[APPROXIMATE] ?: false,
            pausedUntilMs = p[PAUSED_UNTIL] ?: 0L,
            vehicle = Vehicle.fromWire(p[VEHICLE]),
            mapStyle = p[MAP_STYLE]?.let { name -> MapStyleKind.entries.firstOrNull { it.name == name } } ?: MapStyleKind.Street,
            threeD = p[THREE_D] ?: false,
            followPartner = p[FOLLOW] ?: false,
            showRouteLine = p[ROUTE_LINE] ?: true,
            showPlaceLabels = p[PLACE_LABELS] ?: true,
            showWeather = p[WEATHER] ?: false,
            tellBattery = p[TELL_BATTERY] ?: true,
            tellNetwork = p[TELL_NETWORK] ?: true,
            tellGps = p[TELL_GPS] ?: true,
        )
    }

    suspend fun snapshot(): LiveMapSettings = settings.first()

    suspend fun setSharing(on: Boolean) = dataStore.edit { it[SHARING] = on }
    suspend fun setInvisible(on: Boolean) = dataStore.edit { it[INVISIBLE] = on }
    suspend fun setApproximate(on: Boolean) = dataStore.edit { it[APPROXIMATE] = on }
    suspend fun setPausedUntil(ms: Long) = dataStore.edit { it[PAUSED_UNTIL] = ms }
    suspend fun setVehicle(v: Vehicle) = dataStore.edit { it[VEHICLE] = v.wire }
    suspend fun setMapStyle(s: MapStyleKind) = dataStore.edit { it[MAP_STYLE] = s.name }
    suspend fun setThreeD(on: Boolean) = dataStore.edit { it[THREE_D] = on }
    suspend fun setFollowPartner(on: Boolean) = dataStore.edit { it[FOLLOW] = on }
    suspend fun setShowRouteLine(on: Boolean) = dataStore.edit { it[ROUTE_LINE] = on }
    suspend fun setShowPlaceLabels(on: Boolean) = dataStore.edit { it[PLACE_LABELS] = on }
    suspend fun setShowWeather(on: Boolean) = dataStore.edit { it[WEATHER] = on }

    suspend fun setTellAlerts(battery: Boolean, network: Boolean, gps: Boolean) = dataStore.edit {
        it[TELL_BATTERY] = battery
        it[TELL_NETWORK] = network
        it[TELL_GPS] = gps
    }

    private companion object {
        val SHARING = booleanPreferencesKey("livemap_sharing")
        val INVISIBLE = booleanPreferencesKey("livemap_invisible")
        val APPROXIMATE = booleanPreferencesKey("livemap_approximate")
        val PAUSED_UNTIL = longPreferencesKey("livemap_paused_until")
        val VEHICLE = stringPreferencesKey("livemap_vehicle")
        val MAP_STYLE = stringPreferencesKey("livemap_map_style")
        val THREE_D = booleanPreferencesKey("livemap_3d")
        val FOLLOW = booleanPreferencesKey("livemap_follow")
        val ROUTE_LINE = booleanPreferencesKey("livemap_route_line")
        val PLACE_LABELS = booleanPreferencesKey("livemap_place_labels")
        val WEATHER = booleanPreferencesKey("livemap_weather")
        val TELL_BATTERY = booleanPreferencesKey("livemap_tell_battery")
        val TELL_NETWORK = booleanPreferencesKey("livemap_tell_network")
        val TELL_GPS = booleanPreferencesKey("livemap_tell_gps")
    }
}
