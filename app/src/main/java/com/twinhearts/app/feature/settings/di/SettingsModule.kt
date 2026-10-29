package com.twinhearts.app.feature.settings.di

import com.twinhearts.app.feature.settings.data.SettingsPrefs
import com.twinhearts.app.feature.settings.data.SettingsPrefsImpl
import com.twinhearts.app.feature.settings.data.SettingsRepository
import com.twinhearts.app.feature.settings.data.SettingsRepositoryImpl
import com.twinhearts.app.feature.settings.data.remote.SettingsApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Settings feature (repository, phone-only switches, API). */
@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsModule {

    @Binds
    abstract fun bindSettingsRepository(impl: SettingsRepositoryImpl): SettingsRepository

    @Binds
    abstract fun bindSettingsPrefs(impl: SettingsPrefsImpl): SettingsPrefs

    companion object {
        @Provides
        @Singleton
        fun provideSettingsApi(retrofit: Retrofit): SettingsApi = retrofit.create(SettingsApi::class.java)
    }
}
