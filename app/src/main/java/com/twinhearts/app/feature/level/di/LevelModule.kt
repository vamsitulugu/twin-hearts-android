package com.twinhearts.app.feature.level.di

import com.twinhearts.app.feature.level.data.LevelRepository
import com.twinhearts.app.feature.level.data.LevelRepositoryImpl
import com.twinhearts.app.feature.level.data.remote.LevelApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class LevelModule {

    @Binds
    abstract fun bindLevelRepository(impl: LevelRepositoryImpl): LevelRepository

    companion object {
        @Provides
        @Singleton
        fun provideLevelApi(retrofit: Retrofit): LevelApi = retrofit.create(LevelApi::class.java)
    }
}
