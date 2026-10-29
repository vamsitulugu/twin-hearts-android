package com.twinhearts.app.feature.memories.di

import com.twinhearts.app.feature.memories.data.MemoriesRepository
import com.twinhearts.app.feature.memories.data.MemoriesRepositoryImpl
import com.twinhearts.app.feature.memories.data.remote.MemoriesApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MemoriesModule {

    @Binds
    abstract fun bindMemoriesRepository(impl: MemoriesRepositoryImpl): MemoriesRepository

    companion object {
        @Provides
        @Singleton
        fun provideMemoriesApi(retrofit: Retrofit): MemoriesApi = retrofit.create(MemoriesApi::class.java)
    }
}
