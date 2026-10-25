package com.twinhearts.app.core.network

import com.twinhearts.app.core.common.AppError
import com.twinhearts.app.core.common.AppResult
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response
import java.io.IOException

/** Checks that every kind of failure becomes the right [AppError] (R-46). */
class SafeApiCallTest {

    private fun httpError(code: Int, body: String) = HttpException(
        Response.error<Any>(code, body.toResponseBody("application/json".toMediaType())),
    )

    @Test
    fun success_isWrapped() = runTest {
        val result = safeApiCall { "ok" }
        assertEquals(AppResult.Success("ok"), result)
    }

    @Test
    fun http401_keepsServerMessage() = runTest {
        val result = safeApiCall<String> { throw httpError(401, """{"error":"Incorrect password."}""") }
        assertEquals(AppResult.Failure(AppError.Unauthorized("Incorrect password.")), result)
    }

    @Test
    fun http500_withoutBody_hasNoMessage() = runTest {
        val result = safeApiCall<String> { throw httpError(500, "oops") }
        assertEquals(AppResult.Failure(AppError.Server(500, null)), result)
    }

    @Test
    fun ioProblem_isNetworkError() = runTest {
        val result = safeApiCall<String> { throw IOException("no internet") }
        assertEquals(AppResult.Failure(AppError.Network), result)
    }
}
