package com.twinhearts.app.feature.music.singtogether.di

import com.twinhearts.app.feature.music.singtogether.data.SingApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Why this file exists: Hilt wiring for Sing Together (R-44). */
@Module
@InstallIn(SingletonComponent::class)
object SingTogetherModule {
    @Provides
    @Singleton
    fun provideSingApi(retrofit: Retrofit): SingApi = retrofit.create(SingApi::class.java)
}
