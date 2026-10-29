package com.twinhearts.app.feature.dailyroutine.di

import com.twinhearts.app.feature.dailyroutine.data.RoutineRepository
import com.twinhearts.app.feature.dailyroutine.data.RoutineRepositoryImpl
import com.twinhearts.app.feature.dailyroutine.data.remote.RoutineApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import java.time.Clock
import javax.inject.Singleton

/** Hilt wiring for the Daily Routine feature. */
@Module
@InstallIn(SingletonComponent::class)
abstract class DailyRoutineModule {

    @Binds
    abstract fun bindRoutineRepository(impl: RoutineRepositoryImpl): RoutineRepository

    companion object {
        @Provides
        @Singleton
        fun provideRoutineApi(retrofit: Retrofit): RoutineApi = retrofit.create(RoutineApi::class.java)

        /** The clock Daily Routine reads "now" from, so tests can freeze time. */
        @Provides
        @Singleton
        fun provideRoutineClock(): Clock = Clock.systemDefaultZone()
    }
}
