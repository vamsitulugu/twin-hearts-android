package com.twinhearts.app.feature.meetplanner.data

import android.content.Context
import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.meetplanner.data.mapper.checklistBody
import com.twinhearts.app.feature.meetplanner.data.mapper.editBody
import com.twinhearts.app.feature.meetplanner.data.mapper.toCity
import com.twinhearts.app.feature.meetplanner.data.mapper.toCityFromReverse
import com.twinhearts.app.feature.meetplanner.data.mapper.toCreateBody
import com.twinhearts.app.feature.meetplanner.data.mapper.toMeetup
import com.twinhearts.app.feature.meetplanner.data.mapper.toPlace
import com.twinhearts.app.feature.meetplanner.data.mapper.toPlaces
import com.twinhearts.app.feature.meetplanner.data.mapper.toRoute
import com.twinhearts.app.feature.meetplanner.data.remote.CompleteBody
import com.twinhearts.app.feature.meetplanner.data.remote.CoupleBody
import com.twinhearts.app.feature.meetplanner.data.remote.GeoApi
import com.twinhearts.app.feature.meetplanner.data.remote.MeetPlannerApi
import com.twinhearts.app.feature.meetplanner.data.remote.OverpassBody
import com.twinhearts.app.feature.meetplanner.domain.PlanDraft
import com.twinhearts.app.feature.meetplanner.domain.buildOverpassQuery
import com.twinhearts.app.feature.meetplanner.domain.dedupePlaces
import com.twinhearts.app.feature.meetplanner.domain.model.ChecklistItem
import com.twinhearts.app.feature.meetplanner.domain.model.City
import com.twinhearts.app.feature.meetplanner.domain.model.GeoPoint
import com.twinhearts.app.feature.meetplanner.domain.model.MeetIdentity
import com.twinhearts.app.feature.meetplanner.domain.model.Meetup
import com.twinhearts.app.feature.meetplanner.domain.model.Place
import com.twinhearts.app.feature.meetplanner.domain.model.PlaceCategory
import com.twinhearts.app.feature.meetplanner.domain.model.RoutePlan
import com.twinhearts.app.feature.meetplanner.domain.model.TravelMode
import com.twinhearts.app.feature.meetplanner.domain.model.TripType
import com.twinhearts.app.feature.meetplanner.domain.rankPlaces
import com.twinhearts.app.feature.meetplanner.domain.straightLineRoute
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** How many plans were deleted and how many could not be (bulk delete shows both). */
data class DeleteSummary(val deleted: Int, val failed: Int)

/** Why this file exists: the one place Meet Planner screens get data from (R-40). */
interface MeetPlannerRepository {
    suspend fun identity(): MeetIdentity?
    suspend fun plans(who: MeetIdentity): AppResult<List<Meetup>>
    suspend fun searchCities(query: String): AppResult<List<City>>
    suspend fun cityAt(point: GeoPoint): AppResult<City>
    suspend fun nearby(category: PlaceCategory, city: City): AppResult<List<Place>>
    suspend fun searchText(query: String, city: City): AppResult<List<Place>>

    /** Never fails: when the route service is down it returns straight lines between the stops. */
    suspend fun buildRoute(stops: List<Place>, mode: TravelMode): RoutePlan
    suspend fun save(who: MeetIdentity, draft: PlanDraft): AppResult<Unit>
    suspend fun edit(
        who: MeetIdentity,
        id: String,
        title: String,
        from: LocalDate?,
        to: LocalDate?,
        budget: String,
        notes: String,
        tripType: TripType?,
    ): AppResult<Unit>
    suspend fun saveChecklist(who: MeetIdentity, id: String, items: List<ChecklistItem>): AppResult<Unit>
    suspend fun delete(who: MeetIdentity, ids: List<String>): DeleteSummary
    suspend fun complete(who: MeetIdentity, id: String, mood: String?, photoUrls: List<String>, extraNotes: String?): AppResult<Unit>
    suspend fun uploadPhoto(who: MeetIdentity, uri: String): AppResult<String>
    suspend fun photos(who: MeetIdentity, memoryId: String): AppResult<List<String>>
    suspend fun currentLocation(): GeoPoint?
    fun hasLocationPermission(): Boolean
}

@Singleton
class MeetPlannerRepositoryImpl @Inject constructor(
    private val api: MeetPlannerApi,
    private val geo: GeoApi,
    private val sessionStore: SessionStore,
    private val locationFinder: LocationFinder,
    @ApplicationContext private val context: Context,
) : MeetPlannerRepository {

    // Same 5-minute memory cache as the web app, so flipping between chips does not re-ask the map services.
    private val cache = HashMap<String, Pair<Long, List<Place>>>()

    override suspend fun identity(): MeetIdentity? = sessionStore.session.first()?.let {
        MeetIdentity(coupleId = it.coupleId, role = it.role, myName = it.myName, partnerName = it.partnerName)
    }

    override suspend fun plans(who: MeetIdentity): AppResult<List<Meetup>> =
        safeApiCall { api.plans(who.coupleId) }.map { list -> list.mapNotNull { it.toMeetup() } }

    override suspend fun searchCities(query: String): AppResult<List<City>> =
        safeApiCall {
            geo.nominatimSearch(JSON, 1, CITY_LIMIT, "city", null, null, query)
        }.map { list -> list.mapNotNull { it.toCity() } }

    override suspend fun cityAt(point: GeoPoint): AppResult<City> =
        safeApiCall { geo.nominatimReverse(JSON, point.lat, point.lng) }.map { it.toCityFromReverse(point) }

    override suspend fun nearby(category: PlaceCategory, city: City): AppResult<List<Place>> {
        val key = "cat:${category.key}:${"%.3f".format(city.lat)}:${"%.3f".format(city.lng)}"
        cached(key)?.let { return AppResult.Success(it) }
        val origin = GeoPoint(city.lat, city.lng)
        return safeApiCall {
            coroutineScope {
                category.osmKinds.map { kind ->
                    async {
                        val query = buildOverpassQuery(kind.filters, city.lat, city.lng, RADIUS_M, NEARBY_LIMIT)
                        if (query == null) emptyList() else api.overpass(OverpassBody(query)).toPlaces(kind, category, origin)
                    }
                }.awaitAll().flatten()
            }.sortedBy { it.distKm ?: Double.MAX_VALUE }.take(NEARBY_LIMIT)
        }.also { if (it is AppResult.Success) remember(key, it.data) }
    }

    override suspend fun searchText(query: String, city: City): AppResult<List<Place>> {
        val full = "$query ${city.name}"
        val key = "text:${full.lowercase()}"
        cached(key)?.let { return AppResult.Success(it) }
        val origin = GeoPoint(city.lat, city.lng)
        val viewbox = "${city.lng - BIAS_DEG},${city.lat + BIAS_DEG},${city.lng + BIAS_DEG},${city.lat - BIAS_DEG}"
        return safeApiCall {
            coroutineScope {
                val nominatim = async { attempt { geo.nominatimSearch(JSON, 1, TEXT_LIMIT, null, viewbox, 0, full) } }
                val photon = async { attempt { geo.photon(full, TEXT_LIMIT, "en", city.lat, city.lng) } }
                val a = nominatim.await()
                val b = photon.await()
                // One service failing is fine; both failing is a real error the person should see.
                if (a.isFailure && b.isFailure) throw (a.exceptionOrNull() ?: IOException("search failed"))
                val merged = a.getOrNull().orEmpty().mapNotNull { it.toPlace(origin) } +
                    b.getOrNull()?.toPlaces(origin).orEmpty()
                rankPlaces(dedupePlaces(merged), query, TEXT_LIMIT)
            }
        }.also { if (it is AppResult.Success) remember(key, it.data) }
    }

    override suspend fun buildRoute(stops: List<Place>, mode: TravelMode): RoutePlan {
        val coordinates = stops.joinToString(";") { "${it.lng},${it.lat}" }
        return try {
            geo.osrmRoute(coordinates, "full", "geojson", "false").toRoute(mode) ?: straightLineRoute(stops, mode)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            straightLineRoute(stops, mode)
        }
    }

    override suspend fun save(who: MeetIdentity, draft: PlanDraft): AppResult<Unit> {
        val body = draft.toCreateBody(who) ?: return AppResult.Failure(AppError.Unknown)
        return safeApiCall { api.create(body) }.map { }
    }

    override suspend fun edit(
        who: MeetIdentity,
        id: String,
        title: String,
        from: LocalDate?,
        to: LocalDate?,
        budget: String,
        notes: String,
        tripType: TripType?,
    ): AppResult<Unit> {
        val body = editBody(who, title, from, to, budget, notes, tripType)
        return safeApiCall { api.update(id, body) }.map { }
    }

    override suspend fun saveChecklist(who: MeetIdentity, id: String, items: List<ChecklistItem>): AppResult<Unit> =
        safeApiCall { api.update(id, checklistBody(who, items)) }.map { }

    override suspend fun delete(who: MeetIdentity, ids: List<String>): DeleteSummary {
        var deleted = 0
        var failed = 0
        for (id in ids) {
            when (safeApiCall { api.delete(id, CoupleBody(who.coupleId)) }) {
                is AppResult.Success -> deleted++
                is AppResult.Failure -> failed++
            }
        }
        return DeleteSummary(deleted, failed)
    }

    override suspend fun complete(
        who: MeetIdentity,
        id: String,
        mood: String?,
        photoUrls: List<String>,
        extraNotes: String?,
    ): AppResult<Unit> {
        val body = CompleteBody(
            coupleId = who.coupleId, mood = mood, photos = photoUrls, extraNotes = extraNotes,
            senderRole = who.role, myName = who.myName,
        )
        return safeApiCall { api.complete(id, body) }.map { }
    }

    override suspend fun uploadPhoto(who: MeetIdentity, uri: String): AppResult<String> = withContext(Dispatchers.IO) {
        val parsed = Uri.parse(uri)
        val resolver = context.contentResolver
        val mime = resolver.getType(parsed) ?: DEFAULT_MIME
        val bytes = try {
            resolver.openInputStream(parsed)?.use { it.readBytes() }
        } catch (e: IOException) {
            null
        } catch (e: SecurityException) {
            null
        }
        val type = mime.toMediaTypeOrNull()
        if (bytes == null || type == null) {
            AppResult.Failure(AppError.Unknown)
        } else {
            val ext = mime.substringAfter('/').let { if (it == "jpeg") "jpg" else it }
            val part = MultipartBody.Part.createFormData("file", "meetup_${System.currentTimeMillis()}.$ext", bytes.toRequestBody(type))
            val couple = who.coupleId.toRequestBody("text/plain".toMediaTypeOrNull())
            safeApiCall { api.upload(part, couple) }.map { it.url }
        }
    }

    override suspend fun photos(who: MeetIdentity, memoryId: String): AppResult<List<String>> =
        safeApiCall { api.globeMemories(who.coupleId) }.map { memories ->
            memories.firstOrNull { it.id?.contentOrNull == memoryId }
                ?.media.orEmpty()
                .filter { it.type == "photo" }
                .mapNotNull { it.url }
        }

    override suspend fun currentLocation(): GeoPoint? = locationFinder.current()

    override fun hasLocationPermission(): Boolean = locationFinder.hasPermission()

    // ── small helpers ──
    private suspend fun <T> attempt(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.failure(e)
    }

    @Synchronized
    private fun cached(key: String): List<Place>? {
        val hit = cache[key] ?: return null
        return if (System.currentTimeMillis() - hit.first < CACHE_TTL_MS) hit.second else null.also { cache.remove(key) }
    }

    @Synchronized
    private fun remember(key: String, places: List<Place>) {
        if (cache.size >= CACHE_MAX) cache.remove(cache.keys.first())
        cache[key] = System.currentTimeMillis() to places
    }

    private companion object {
        const val JSON = "json"
        const val CITY_LIMIT = 6
        const val TEXT_LIMIT = 15
        const val NEARBY_LIMIT = 30
        const val RADIUS_M = 12_000
        const val BIAS_DEG = 1.5
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val CACHE_MAX = 100
        const val DEFAULT_MIME = "image/jpeg"
    }
}
