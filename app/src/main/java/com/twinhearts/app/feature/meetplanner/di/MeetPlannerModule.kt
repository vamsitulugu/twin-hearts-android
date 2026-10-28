package com.twinhearts.app.feature.meetplanner.di

import com.twinhearts.app.feature.meetplanner.data.MeetPlannerRepository
import com.twinhearts.app.feature.meetplanner.data.MeetPlannerRepositoryImpl
import com.twinhearts.app.feature.meetplanner.data.remote.GeoApi
import com.twinhearts.app.feature.meetplanner.data.remote.MeetPlannerApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Hilt wiring for Meet Planner (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class MeetPlannerModule {

    @Binds
    abstract fun bindMeetPlannerRepository(impl: MeetPlannerRepositoryImpl): MeetPlannerRepository

    companion object {
        private const val NOMINATIM_BASE_URL = "https://nominatim.openstreetmap.org/"

        // The free map services ask apps to say who they are.
        private const val USER_AGENT = "TwinHeartsAndroid/1.0 (couples app)"

        @Provides
        @Singleton
        fun provideMeetPlannerApi(retrofit: Retrofit): MeetPlannerApi = retrofit.create(MeetPlannerApi::class.java)

        /**
         * A separate client for the public map services. It starts from the app client but only adds
         * a User-Agent and a language, and uses shorter waits: these services answer fast or not at all.
         */
        @Provides
        @Singleton
        fun provideGeoApi(client: OkHttpClient, json: Json): GeoApi {
            val geoClient = client.newBuilder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .callTimeout(30, TimeUnit.SECONDS)
                .addInterceptor { chain ->
                    chain.proceed(
                        chain.request().newBuilder()
                            .header("User-Agent", USER_AGENT)
                            .header("Accept-Language", "en")
                            .build(),
                    )
                }
                .build()
            return Retrofit.Builder()
                .baseUrl(NOMINATIM_BASE_URL)
                .client(geoClient)
                .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
                .build()
                .create(GeoApi::class.java)
        }
    }
}
