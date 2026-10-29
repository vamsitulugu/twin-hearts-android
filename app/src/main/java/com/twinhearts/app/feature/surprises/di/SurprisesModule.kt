package com.twinhearts.app.feature.surprises.di

import com.twinhearts.app.feature.surprises.data.SurprisesRepository
import com.twinhearts.app.feature.surprises.data.SurprisesRepositoryImpl
import com.twinhearts.app.feature.surprises.data.remote.SurprisesApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for Surprises (R-44). Auto-discovered; nothing to register elsewhere. */
@Module
@InstallIn(SingletonComponent::class)
abstract class SurprisesModule {

    @Binds
    abstract fun bindSurprisesRepository(impl: SurprisesRepositoryImpl): SurprisesRepository

    companion object {
        @Provides
        @Singleton
        fun provideSurprisesApi(retrofit: Retrofit): SurprisesApi = retrofit.create(SurprisesApi::class.java)
    }
}
