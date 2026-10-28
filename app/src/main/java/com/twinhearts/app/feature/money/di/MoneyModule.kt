package com.twinhearts.app.feature.money.di

import com.twinhearts.app.feature.money.data.MoneyRepository
import com.twinhearts.app.feature.money.data.MoneyRepositoryImpl
import com.twinhearts.app.feature.money.data.remote.MoneyApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MoneyModule {

    @Binds
    abstract fun bindMoneyRepository(impl: MoneyRepositoryImpl): MoneyRepository

    companion object {
        @Provides
        @Singleton
        fun provideMoneyApi(retrofit: Retrofit): MoneyApi = retrofit.create(MoneyApi::class.java)
    }
}
