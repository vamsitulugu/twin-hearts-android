package com.twinhearts.app.feature.globe.di

import com.twinhearts.app.feature.globe.data.GlobeRepository
import com.twinhearts.app.feature.globe.data.GlobeRepositoryImpl
import com.twinhearts.app.feature.globe.data.remote.GlobeApi
import com.twinhearts.app.feature.globe.data.remote.GlobeGeoApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Globe feature (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class GlobeModule {

    @Binds
    abstract fun bindGlobeRepository(impl: GlobeRepositoryImpl): GlobeRepository

    companion object {
        @Provides
        @Singleton
        fun provideGlobeApi(retrofit: Retrofit): GlobeApi = retrofit.create(GlobeApi::class.java)

        /** Same Retrofit: every call passes a full https URL with @Url. */
        @Provides
        @Singleton
        fun provideGlobeGeoApi(retrofit: Retrofit): GlobeGeoApi = retrofit.create(GlobeGeoApi::class.java)
    }
}
