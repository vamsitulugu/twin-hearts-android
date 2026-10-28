package com.twinhearts.app.core.network

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** The old server answers every failure as `{ "error": "friendly text" }`. */
@Serializable
private data class ApiErrorBody(val error: String? = null)

private val errorJson = Json { ignoreUnknownKeys = true }

/**
 * Runs one Retrofit call and turns every failure into an [AppError] (R-46).
 * Repositories wrap each API call in this, so no exception ever reaches a ViewModel.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): AppResult<T> = try {
    AppResult.Success(block())
} catch (e: CancellationException) {
    throw e // never swallow coroutine cancellation
} catch (e: HttpException) {
    Log.w(TAG, "HTTP ${e.code()} from the server")
    val message = parseServerMessage(e)
    AppResult.Failure(
        if (e.code() == 401) AppError.Unauthorized(message) else AppError.Server(e.code(), message),
    )
} catch (e: IOException) {
    Log.w(TAG, "Network problem: ${e::class.java.simpleName}")
    AppResult.Failure(AppError.Network)
} catch (e: SerializationException) {
    // Only the exception type is logged (never the body: it holds personal data).
    Log.w(TAG, "Could not read the answer: ${e::class.java.simpleName}")
    AppResult.Failure(AppError.Unknown)
} catch (e: Exception) {
    Log.w(TAG, "Unexpected problem: ${e::class.java.simpleName}")
    AppResult.Failure(AppError.Unknown)
}

private const val TAG = "TwinApi"

private fun parseServerMessage(e: HttpException): String? = try {
    e.response()?.errorBody()?.string()?.let { errorJson.decodeFromString<ApiErrorBody>(it).error }
} catch (ignored: Exception) {
    null
}
