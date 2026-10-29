package com.twinhearts.app.feature.period.di

import com.twinhearts.app.feature.period.data.PeriodRepository
import com.twinhearts.app.feature.period.data.PeriodRepositoryImpl
import com.twinhearts.app.feature.period.data.remote.PeriodApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class PeriodModule {

    @Binds
    abstract fun bindPeriodRepository(impl: PeriodRepositoryImpl): PeriodRepository

    companion object {
        @Provides
        @Singleton
        fun providePeriodApi(retrofit: Retrofit): PeriodApi = retrofit.create(PeriodApi::class.java)
    }
}
