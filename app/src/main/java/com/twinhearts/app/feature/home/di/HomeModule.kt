package com.twinhearts.app.feature.home.di

import com.twinhearts.app.feature.home.data.HomeRepository
import com.twinhearts.app.feature.home.data.HomeRepositoryImpl
import com.twinhearts.app.feature.home.data.remote.HomeApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Home feature. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HomeModule {

    @Binds
    abstract fun bindHomeRepository(impl: HomeRepositoryImpl): HomeRepository

    companion object {
        @Provides
        @Singleton
        fun provideHomeApi(retrofit: Retrofit): HomeApi = retrofit.create(HomeApi::class.java)
    }
}
