package com.twinhearts.app.feature.movie.cinema.di

import com.twinhearts.app.feature.movie.cinema.data.CinemaApi
import com.twinhearts.app.feature.movie.cinema.data.CinemaRepository
import com.twinhearts.app.feature.movie.cinema.data.CinemaRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Twin Cinema 3D entry inside Watch Together (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class CinemaModule {

    @Binds
    abstract fun bindCinemaRepository(impl: CinemaRepositoryImpl): CinemaRepository

    companion object {
        @Provides
        @Singleton
        fun provideCinemaApi(retrofit: Retrofit): CinemaApi = retrofit.create(CinemaApi::class.java)
    }
}
