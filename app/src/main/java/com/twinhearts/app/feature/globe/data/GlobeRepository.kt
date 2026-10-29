package com.twinhearts.app.feature.globe.data

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.Session
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.globe.data.mapper.mediaWire
import com.twinhearts.app.feature.globe.data.mapper.toDomain
import com.twinhearts.app.feature.globe.data.mapper.toPerson
import com.twinhearts.app.feature.globe.data.mapper.toPlace
import com.twinhearts.app.feature.globe.data.mapper.toRoad
import com.twinhearts.app.feature.globe.data.mapper.toWeather
import com.twinhearts.app.feature.globe.data.mapper.toWire
import com.twinhearts.app.feature.globe.data.remote.AddMediaBody
import com.twinhearts.app.feature.globe.data.remote.GlobeApi
import com.twinhearts.app.feature.globe.data.remote.GlobeGeoApi
import com.twinhearts.app.feature.globe.data.remote.PingBody
import com.twinhearts.app.feature.globe.data.remote.SaveMemoryBody
import com.twinhearts.app.feature.globe.data.remote.StopBody
import com.twinhearts.app.feature.globe.domain.model.GeoPoint
import com.twinhearts.app.feature.globe.domain.model.LiveSnapshot
import com.twinhearts.app.feature.globe.domain.model.MediaKind
import com.twinhearts.app.feature.globe.domain.model.Memory
import com.twinhearts.app.feature.globe.domain.model.MemoryDraft
import com.twinhearts.app.feature.globe.domain.model.PlaceHit
import com.twinhearts.app.feature.globe.domain.model.StagedMedia
import com.twinhearts.app.feature.globe.domain.model.WeatherNow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.net.URLEncoder
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** Who is signed in, as far as the Globe needs to know. */
data class GlobeIdentity(val coupleId: String, val role: String, val myName: String, val partnerName: String)

/** What happened when a memory was saved: its id and how many attachments failed to upload. */
data class SaveOutcome(val memoryId: String, val failedMedia: Int)

/** Everything the Globe screens need from the network (R-40). */
interface GlobeRepository {
    suspend fun identity(): GlobeIdentity?
    suspend fun memories(): AppResult<List<Memory>>
    suspend fun isPro(): AppResult<Boolean>
    suspend fun saveMemory(editingId: String?, draft: MemoryDraft, media: List<StagedMedia>): AppResult<SaveOutcome>
    suspend fun deleteMemory(id: String): AppResult<Unit>
    suspend fun deleteMedia(id: String): AppResult<Unit>
    suspend fun uploadPicked(uri: String, kind: MediaKind): AppResult<String>
    suspend fun uploadVoice(file: File): AppResult<String>
    suspend fun addMedia(memoryId: String, kind: MediaKind, url: String?, duration: String?): AppResult<Unit>
    suspend fun addJournal(memoryId: String, text: String): AppResult<Unit>
    suspend fun searchPlaces(query: String): AppResult<List<PlaceHit>>
    suspend fun weatherAt(lat: Double, lng: Double): AppResult<WeatherNow?>
    suspend fun roadRoute(from: GeoPoint, to: GeoPoint): AppResult<List<GeoPoint>?>
    suspend fun liveLocations(): AppResult<LiveSnapshot>
    suspend fun ping(fix: DeviceFix): AppResult<Unit>
    suspend fun stopSharing(): AppResult<Unit>
    suspend fun trail(ofPartner: Boolean): AppResult<List<GeoPoint>>
}

@Singleton
class GlobeRepositoryImpl @Inject constructor(
    private val api: GlobeApi,
    private val geo: GlobeGeoApi,
    private val files: GlobeFiles,
    private val sessionStore: SessionStore,
) : GlobeRepository {

    private suspend fun session(): Session? = sessionStore.session.first()

    private suspend fun <T> withSession(block: suspend (Session) -> AppResult<T>): AppResult<T> {
        val s = session() ?: return AppResult.Failure(AppError.Unauthorized())
        return block(s)
    }

    private fun Session.roleOf(partner: Boolean): String =
        if (partner) (if (role == "user1") "user2" else "user1") else role

    override suspend fun identity(): GlobeIdentity? =
        session()?.let { GlobeIdentity(it.coupleId, it.role, it.myName, it.partnerName) }

    override suspend fun memories() = withSession { s ->
        safeApiCall { api.memories(s.coupleId) }.map { list -> list.mapNotNull { it.toDomain() } }
    }

    override suspend fun isPro() = withSession { s -> safeApiCall { api.proStatus(s.coupleId) }.map { it.isPro } }

    override suspend fun saveMemory(editingId: String?, draft: MemoryDraft, media: List<StagedMedia>) = withSession { s ->
        val body = SaveMemoryBody(
            coupleId = s.coupleId,
            memory = draft.toWire(s.role),
            // The server sends the partner a push ("New Memory on the Globe") when it gets these two.
            senderRole = if (editingId == null) s.role else null,
            myName = if (editingId == null) s.myName else null,
        )
        val saved = when (
            val r = safeApiCall { if (editingId == null) api.create(body) else api.update(editingId, body) }
        ) {
            is AppResult.Success -> r.data
            is AppResult.Failure -> return@withSession r
        }
        val memoryId = saved.id ?: editingId ?: return@withSession AppResult.Failure(AppError.Unknown)
        var failed = 0
        // One request per item: it works with both versions of the server's media route.
        media.filter { it.url != null }.forEach { item ->
            val r = safeApiCall {
                api.addMedia(memoryId, AddMediaBody(s.coupleId, mediaWire(item.kind, item.url, item.duration.ifBlank { null }, null)))
            }
            if (r is AppResult.Failure) failed++
        }
        val journal = draft.journal.trim()
        if (journal.isNotEmpty()) {
            val r = safeApiCall {
                api.addMedia(memoryId, AddMediaBody(s.coupleId, mediaWire(MediaKind.Journal, null, null, journal)))
            }
            if (r is AppResult.Failure) failed++
        }
        AppResult.Success(SaveOutcome(memoryId, failed))
    }

    override suspend fun deleteMemory(id: String) = withSession { s ->
        safeApiCall { api.deleteMemory(id, s.coupleId) }.map { }
    }

    override suspend fun deleteMedia(id: String) = withSession { s ->
        safeApiCall { api.deleteMedia(id, s.coupleId) }.map { }
    }

    override suspend fun uploadPicked(uri: String, kind: MediaKind) = withSession { s ->
        val part = files.part(uri, kind) ?: return@withSession AppResult.Failure(AppError.Unknown)
        safeApiCall { api.upload(part, s.coupleId.toRequestBody(TEXT)) }.map { it.url }
    }

    override suspend fun uploadVoice(file: File) = withSession { s ->
        val bytes = withContext(Dispatchers.IO) { runCatching { file.readBytes() }.getOrNull() }
        if (bytes == null || bytes.isEmpty()) return@withSession AppResult.Failure(AppError.Unknown)
        val part = MultipartBody.Part.createFormData("file", "voice.m4a", bytes.toRequestBody("audio/mp4".toMediaType()))
        safeApiCall { api.uploadVoice(part, s.coupleId.toRequestBody(TEXT), s.role.toRequestBody(TEXT)) }.map { it.url }
    }

    override suspend fun addMedia(memoryId: String, kind: MediaKind, url: String?, duration: String?) = withSession { s ->
        safeApiCall { api.addMedia(memoryId, AddMediaBody(s.coupleId, mediaWire(kind, url, duration, null), s.role, s.myName)) }.map { }
    }

    override suspend fun addJournal(memoryId: String, text: String) = withSession { s ->
        safeApiCall { api.addMedia(memoryId, AddMediaBody(s.coupleId, mediaWire(MediaKind.Journal, null, null, text))) }.map { }
    }

    override suspend fun searchPlaces(query: String) = safeApiCall {
        geo.search("https://nominatim.openstreetmap.org/search?q=${enc(query)}&format=json&limit=6&addressdetails=1")
    }.map { hits -> hits.mapNotNull { it.toPlace(query) } }

    override suspend fun weatherAt(lat: Double, lng: Double) = safeApiCall {
        geo.weather("https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lng&current_weather=true")
    }.map { it.toWeather() }

    override suspend fun roadRoute(from: GeoPoint, to: GeoPoint) = safeApiCall {
        geo.route(
            "https://router.project-osrm.org/route/v1/driving/" +
                "${from.lng},${from.lat};${to.lng},${to.lat}?overview=full&geometries=geojson",
        )
    }.map { it.toRoad() }

    override suspend fun liveLocations() = withSession { s ->
        safeApiCall { api.locations(s.coupleId) }.map { dto ->
            val mine = if (s.role == "user1") dto.user1 else dto.user2
            val theirs = if (s.role == "user1") dto.user2 else dto.user1
            LiveSnapshot(me = mine?.toPerson(), partner = theirs?.toPerson())
        }
    }

    override suspend fun ping(fix: DeviceFix) = withSession { s ->
        safeApiCall {
            api.ping(
                PingBody(
                    coupleId = s.coupleId,
                    role = s.role,
                    lat = fix.lat,
                    lng = fix.lng,
                    accuracy = fix.accuracyM,
                    heading = fix.headingDeg,
                    speed = fix.speedMps,
                    moving = (fix.speedMps ?: 0.0) > MOVING_MPS,
                ),
            )
        }.map { }
    }

    override suspend fun stopSharing() = withSession { s ->
        safeApiCall { api.stop(StopBody(s.coupleId, s.role)) }.map { }
    }

    override suspend fun trail(ofPartner: Boolean) = withSession { s ->
        safeApiCall { api.trail(s.coupleId, s.roleOf(ofPartner)) }.map { list ->
            list.mapNotNull { p -> if (p.lat != null && p.lng != null) GeoPoint(p.lat, p.lng) else null }
        }
    }

    private fun enc(text: String): String = URLEncoder.encode(text, "UTF-8")

    private companion object {
        val TEXT = "text/plain".toMediaType()
        const val MOVING_MPS = 0.5
    }
}
