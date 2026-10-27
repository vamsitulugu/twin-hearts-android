package com.twinhearts.app.feature.twinai.di

import com.twinhearts.app.feature.twinai.data.TwinAiRepository
import com.twinhearts.app.feature.twinai.data.TwinAiRepositoryImpl
import com.twinhearts.app.feature.twinai.data.remote.TwinAiApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Twin AI feature (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class TwinAiModule {

    @Binds
    abstract fun bindTwinAiRepository(impl: TwinAiRepositoryImpl): TwinAiRepository

    companion object {
        @Provides
        @Singleton
        fun provideTwinAiApi(retrofit: Retrofit): TwinAiApi = retrofit.create(TwinAiApi::class.java)
    }
}
