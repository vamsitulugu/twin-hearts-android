package com.twinhearts.app.feature.us.di

import com.twinhearts.app.feature.us.data.UsRepository
import com.twinhearts.app.feature.us.data.UsRepositoryImpl
import com.twinhearts.app.feature.us.data.remote.UsApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class UsModule {

    @Binds
    abstract fun bindUsRepository(impl: UsRepositoryImpl): UsRepository

    companion object {
        @Provides
        @Singleton
        fun provideUsApi(retrofit: Retrofit): UsApi = retrofit.create(UsApi::class.java)
    }
}
