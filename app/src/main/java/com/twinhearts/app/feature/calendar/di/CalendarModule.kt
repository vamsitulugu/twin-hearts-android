package com.twinhearts.app.feature.calendar.di

import com.twinhearts.app.feature.calendar.data.CalendarRepository
import com.twinhearts.app.feature.calendar.data.CalendarRepositoryImpl
import com.twinhearts.app.feature.calendar.data.remote.CalendarApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CalendarModule {

    @Binds
    abstract fun bindCalendarRepository(impl: CalendarRepositoryImpl): CalendarRepository

    companion object {
        @Provides
        @Singleton
        fun provideCalendarApi(retrofit: Retrofit): CalendarApi = retrofit.create(CalendarApi::class.java)
    }
}
