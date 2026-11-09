package com.twinhearts.app.core.network

import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Why this file exists: on mobile data a request often fails once (weak signal, a switch between
 * Wi-Fi and data, a stale connection, the free Render server still waking up) and works a second
 * later. Instead of showing "Something went wrong", we try again a few times, quietly.
 *
 * What is repeated, and when:
 * - Reads (GET) are always safe to repeat.
 * - Saving the shared state and sending a chat message are safe too: they MERGE / upsert by
 *   `clientId`, so doing the same save twice gives the same result.
 * - Everything else (payments, coins, gifts ...) is repeated only when the request provably never
 *   reached the server (could not connect), so nothing is ever done twice.
 * - A 502 / 503 / 504 answer comes from Render's front door while the server wakes up.
 */
class RetryInterceptor(private val maxAttempts: Int = 4) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val repeatable = request.body?.isOneShot() != true
        val safeToRepeat = request.method == "GET" || request.method == "HEAD" || isIdempotentSave(request.method, request.url.encodedPath)
        var attempt = 0
        while (true) {
            attempt++
            val last = attempt >= maxAttempts
            try {
                val response = chain.proceed(request)
                val retryCode = response.code == 502 || response.code == 503 ||
                    (response.code == 504 && safeToRepeat)
                if (!retryCode || last || !repeatable || chain.call().isCanceled()) return response
                response.close()
            } catch (e: IOException) {
                val neverSent = e is UnknownHostException || e is ConnectException ||
                    (e is SocketTimeoutException && e.message?.contains("connect", ignoreCase = true) == true)
                if (last || !repeatable || chain.call().isCanceled() || !(safeToRepeat || neverSent)) throw e
            }
            pause(attempt)
        }
    }

    /** 1 s, 2 s, 4 s ... so a waking server gets time, but the person is not left waiting for ever. */
    private fun pause(attempt: Int) {
        try {
            Thread.sleep(1000L shl (attempt - 1).coerceAtMost(3))
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Canceled")
        }
    }

    private fun isIdempotentSave(method: String, path: String): Boolean =
        method == "POST" && (path == "/api/data/state" || path == "/api/chat")
}
