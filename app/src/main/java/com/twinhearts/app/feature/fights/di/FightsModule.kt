package com.twinhearts.app.feature.fights.di

import com.twinhearts.app.feature.fights.data.FightsRepository
import com.twinhearts.app.feature.fights.data.FightsRepositoryImpl
import com.twinhearts.app.feature.fights.data.remote.FightsApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Fight Log feature (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class FightsModule {

    @Binds
    abstract fun bindFightsRepository(impl: FightsRepositoryImpl): FightsRepository

    companion object {
        @Provides
        @Singleton
        fun provideFightsApi(retrofit: Retrofit): FightsApi = retrofit.create(FightsApi::class.java)
    }
}
