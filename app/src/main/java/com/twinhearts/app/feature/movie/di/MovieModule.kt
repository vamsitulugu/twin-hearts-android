package com.twinhearts.app.feature.movie.di

import com.twinhearts.app.feature.movie.data.MovieRepository
import com.twinhearts.app.feature.movie.data.MovieRepositoryImpl
import com.twinhearts.app.feature.movie.data.remote.MovieApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for Watch Together (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class MovieModule {

    @Binds
    abstract fun bindMovieRepository(impl: MovieRepositoryImpl): MovieRepository

    companion object {
        @Provides
        @Singleton
        fun provideMovieApi(retrofit: Retrofit): MovieApi = retrofit.create(MovieApi::class.java)
    }
}
