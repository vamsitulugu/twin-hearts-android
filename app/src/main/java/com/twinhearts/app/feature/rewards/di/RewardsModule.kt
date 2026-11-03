package com.twinhearts.app.feature.rewards.di

import com.twinhearts.app.feature.rewards.data.RewardsRepository
import com.twinhearts.app.feature.rewards.data.RewardsRepositoryImpl
import com.twinhearts.app.feature.rewards.data.remote.RewardsApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RewardsModule {

    @Binds
    abstract fun bindRewardsRepository(impl: RewardsRepositoryImpl): RewardsRepository

    @Binds
    abstract fun bindCoinEarner(impl: com.twinhearts.app.feature.rewards.data.CoinEarnerImpl): com.twinhearts.app.feature.rewards.domain.CoinEarner

    companion object {
        @Provides
        @Singleton
        fun provideRewardsApi(retrofit: Retrofit): RewardsApi = retrofit.create(RewardsApi::class.java)
    }
}
