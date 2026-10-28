package com.twinhearts.app.feature.livemap.di

import com.twinhearts.app.feature.livemap.data.LiveMapRepository
import com.twinhearts.app.feature.livemap.data.LiveMapRepositoryImpl
import com.twinhearts.app.feature.livemap.data.remote.GeoServicesApi
import com.twinhearts.app.feature.livemap.data.remote.LiveMapApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Live Map feature (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class LiveMapModule {

    @Binds
    abstract fun bindLiveMapRepository(impl: LiveMapRepositoryImpl): LiveMapRepository

    companion object {
        @Provides
        @Singleton
        fun provideLiveMapApi(retrofit: Retrofit): LiveMapApi = retrofit.create(LiveMapApi::class.java)

        /** Same Retrofit: every call to this API passes a full https URL with @Url. */
        @Provides
        @Singleton
        fun provideGeoServicesApi(retrofit: Retrofit): GeoServicesApi = retrofit.create(GeoServicesApi::class.java)
    }
}
