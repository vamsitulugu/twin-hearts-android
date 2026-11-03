package com.twinhearts.app.feature.access.di

import com.twinhearts.app.feature.access.data.remote.AccessApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AccessModule {
    @Provides
    @Singleton
    fun provideAccessApi(retrofit: Retrofit): AccessApi = retrofit.create(AccessApi::class.java)
}
