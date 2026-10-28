package com.twinhearts.app.feature.calls.di

import com.twinhearts.app.feature.calls.data.CallRepository
import com.twinhearts.app.feature.calls.data.CallRepositoryImpl
import com.twinhearts.app.feature.calls.data.remote.CallApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Why this file exists: Hilt wiring for Calls (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class CallsBindings {
    @Binds
    abstract fun bindRepository(impl: CallRepositoryImpl): CallRepository
}

@Module
@InstallIn(SingletonComponent::class)
object CallsApiModule {
    @Provides
    @Singleton
    fun provideCallApi(retrofit: Retrofit): CallApi = retrofit.create(CallApi::class.java)
}
