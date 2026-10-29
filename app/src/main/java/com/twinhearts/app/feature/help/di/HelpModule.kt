package com.twinhearts.app.feature.help.di

import com.twinhearts.app.feature.help.data.HelpRepository
import com.twinhearts.app.feature.help.data.HelpRepositoryImpl
import com.twinhearts.app.feature.help.data.remote.HelpApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Wires the Help repository and its Retrofit API (R-44). Found by Hilt automatically. */
@Module
@InstallIn(SingletonComponent::class)
abstract class HelpModule {

    @Binds
    abstract fun bindHelpRepository(impl: HelpRepositoryImpl): HelpRepository

    companion object {
        @Provides
        @Singleton
        fun provideHelpApi(retrofit: Retrofit): HelpApi = retrofit.create(HelpApi::class.java)
    }
}
