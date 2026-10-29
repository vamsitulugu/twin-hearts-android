package com.twinhearts.app.feature.places.di

import com.twinhearts.app.feature.places.data.PlacesRepository
import com.twinhearts.app.feature.places.data.PlacesRepositoryImpl
import com.twinhearts.app.feature.places.data.remote.PlacesApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for Places (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlacesModule {

    @Binds
    abstract fun bindPlacesRepository(impl: PlacesRepositoryImpl): PlacesRepository

    companion object {
        @Provides
        @Singleton
        fun providePlacesApi(retrofit: Retrofit): PlacesApi = retrofit.create(PlacesApi::class.java)
    }
}
