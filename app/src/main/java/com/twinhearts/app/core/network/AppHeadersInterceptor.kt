package com.twinhearts.app.core.network

import com.twinhearts.app.BuildConfig
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Tells our server which app is calling, so the admin dashboard can separate Android from web
 * and show app versions. Added only to calls to our own server (never to other hosts).
 */
class AppHeadersInterceptor : Interceptor {
    private val apiHost: String? = BuildConfig.API_BASE_URL.toHttpUrlOrNull()?.host

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (apiHost == null || request.url.host != apiHost) return chain.proceed(request)
        return chain.proceed(
            request.newBuilder()
                .header("X-App-Platform", "android_native")
                .header("X-App-Version", BuildConfig.VERSION_NAME)
                .build(),
        )
    }
}
