package com.twinhearts.app.feature.places.data

import android.content.Context
import android.net.Uri
import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import com.twinhearts.app.core.common.map
import com.twinhearts.app.core.data.SessionStore
import com.twinhearts.app.core.network.safeApiCall
import com.twinhearts.app.feature.places.data.mapper.toPlaces
import com.twinhearts.app.feature.places.data.mapper.toStateBody
import com.twinhearts.app.feature.places.data.remote.PlacesApi
import com.twinhearts.app.feature.places.data.remote.SaveStateBody
import com.twinhearts.app.feature.places.domain.model.GeoPoint
import com.twinhearts.app.feature.places.domain.model.PlaceMemory
import com.twinhearts.app.feature.places.domain.model.PlacesIdentity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Why this file exists: the only door between the Places screens and the server. Places are one
 * list inside the couple's shared state, so "save" always sends the whole list (like the web app).
 */
interface PlacesRepository {
    suspend fun identity(): PlacesIdentity?
    suspend fun load(who: PlacesIdentity): AppResult<List<PlaceMemory>>
    suspend fun save(who: PlacesIdentity, places: List<PlaceMemory>): AppResult<Unit>
    suspend fun uploadPhoto(who: PlacesIdentity, uri: String): AppResult<String>
    suspend fun currentLocation(): GeoPoint?
    fun hasLocationPermission(): Boolean
}

@Singleton
class PlacesRepositoryImpl @Inject constructor(
    private val api: PlacesApi,
    private val sessionStore: SessionStore,
    private val locationFinder: LocationFinder,
    @ApplicationContext private val context: Context,
) : PlacesRepository {

    override suspend fun identity(): PlacesIdentity? = sessionStore.session.first()?.let {
        PlacesIdentity(coupleId = it.coupleId, role = it.role, myName = it.myName)
    }

    override suspend fun load(who: PlacesIdentity): AppResult<List<PlaceMemory>> =
        safeApiCall { api.getState(who.coupleId) }.map { it.toPlaces() }

    override suspend fun save(who: PlacesIdentity, places: List<PlaceMemory>): AppResult<Unit> =
        safeApiCall {
            api.saveState(SaveStateBody(who.coupleId, places.toStateBody(), who.role, who.myName))
        }.map { }

    override suspend fun uploadPhoto(who: PlacesIdentity, uri: String): AppResult<String> = withContext(Dispatchers.IO) {
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
            val part = MultipartBody.Part.createFormData("file", "place_${System.currentTimeMillis()}.$ext", bytes.toRequestBody(type))
            val couple = who.coupleId.toRequestBody("text/plain".toMediaTypeOrNull())
            safeApiCall { api.upload(part, couple) }.map { it.url }
        }
    }

    override suspend fun currentLocation(): GeoPoint? = locationFinder.current()

    override fun hasLocationPermission(): Boolean = locationFinder.hasPermission()

    private companion object {
        const val DEFAULT_MIME = "image/jpeg"
    }
}
