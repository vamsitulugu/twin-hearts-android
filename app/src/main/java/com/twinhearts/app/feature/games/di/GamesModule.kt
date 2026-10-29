package com.twinhearts.app.feature.games.di

import com.twinhearts.app.feature.games.data.GamesRepository
import com.twinhearts.app.feature.games.data.GamesRepositoryImpl
import com.twinhearts.app.feature.games.data.remote.GamesApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Wires the Games repository and its API (R-44: lives inside the feature). */
@Module
@InstallIn(SingletonComponent::class)
abstract class GamesModule {

    @Binds
    abstract fun bindGamesRepository(impl: GamesRepositoryImpl): GamesRepository

    companion object {
        @Provides
        @Singleton
        fun provideGamesApi(retrofit: Retrofit): GamesApi = retrofit.create(GamesApi::class.java)
    }
}
