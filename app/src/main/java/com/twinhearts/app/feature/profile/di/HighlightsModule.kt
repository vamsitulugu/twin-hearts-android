package com.twinhearts.app.feature.profile.di

import com.twinhearts.app.feature.profile.highlights.data.HighlightsRepository
import com.twinhearts.app.feature.profile.highlights.data.HighlightsRepositoryImpl
import com.twinhearts.app.feature.profile.highlights.data.remote.HighlightsApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Profile Highlights row (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class HighlightsModule {

    @Binds
    abstract fun bindHighlightsRepository(impl: HighlightsRepositoryImpl): HighlightsRepository

    companion object {
        @Provides
        @Singleton
        fun provideHighlightsApi(retrofit: Retrofit): HighlightsApi = retrofit.create(HighlightsApi::class.java)
    }
}
