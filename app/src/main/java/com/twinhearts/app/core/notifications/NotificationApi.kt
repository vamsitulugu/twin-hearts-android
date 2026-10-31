package com.twinhearts.app.core.notifications

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Singleton

/**
 * The only door from the app to the server's push system. The server (routes/auth.js) already holds
 * the Firebase service account and sends every push itself (chat, calls, hug, touch, alerts …);
 * the app only tells it which phone belongs to which person.
 */
interface NotificationApi {

    /** Saves this phone's FCM token for the signed-in person (existing server route). */
    @POST("api/auth/register-fcm-token")
    suspend fun registerToken(@Body body: RegisterTokenBody): JsonElement

    /** Forgets this phone's token on log out (optional route, see backend-changes/notifications/README.md). */
    @POST("api/auth/unregister-fcm-token")
    suspend fun removeToken(@Body body: RemoveTokenBody): JsonElement
}

@Serializable
data class RegisterTokenBody(
    val userId: String,
    val coupleId: String,
    val role: String,
    val token: String,
)

@Serializable
data class RemoveTokenBody(val userId: String, val token: String)

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {
    @Provides
    @Singleton
    fun provideNotificationApi(retrofit: Retrofit): NotificationApi = retrofit.create(NotificationApi::class.java)
}
