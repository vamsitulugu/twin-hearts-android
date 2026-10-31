package com.twinhearts.app.feature.search.di

import com.twinhearts.app.feature.search.data.SearchRepository
import com.twinhearts.app.feature.search.data.SearchRepositoryImpl
import com.twinhearts.app.feature.search.data.remote.SearchApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Why this file exists: Hilt wiring for the Search feature (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SearchModule {

    @Binds
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository

    companion object {
        @Provides
        @Singleton
        fun provideSearchApi(retrofit: Retrofit): SearchApi = retrofit.create(SearchApi::class.java)
    }
}
