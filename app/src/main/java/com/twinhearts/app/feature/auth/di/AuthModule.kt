package com.twinhearts.app.feature.auth.di

import com.twinhearts.app.feature.auth.data.AuthRepository
import com.twinhearts.app.feature.auth.data.AuthRepositoryImpl
import com.twinhearts.app.feature.auth.data.remote.AuthApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the auth feature: the repository and its Retrofit API. */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl): AuthRepository

    companion object {
        @Provides
        @Singleton
        fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)
    }
}
