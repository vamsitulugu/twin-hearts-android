package com.twinhearts.app.core.telemetry

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object TelemetryModule {
    @Provides
    @Singleton
    fun provideTelemetryApi(retrofit: Retrofit): TelemetryApi = retrofit.create(TelemetryApi::class.java)
}
