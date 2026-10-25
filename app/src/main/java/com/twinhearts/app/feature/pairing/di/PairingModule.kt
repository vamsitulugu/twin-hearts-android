package com.twinhearts.app.feature.pairing.di

import com.twinhearts.app.feature.pairing.data.PairingRepository
import com.twinhearts.app.feature.pairing.data.PairingRepositoryImpl
import com.twinhearts.app.feature.pairing.data.remote.PairingApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the pairing feature. */
@Module
@InstallIn(SingletonComponent::class)
abstract class PairingModule {

    @Binds
    abstract fun bindPairingRepository(impl: PairingRepositoryImpl): PairingRepository

    companion object {
        @Provides
        @Singleton
        fun providePairingApi(retrofit: Retrofit): PairingApi = retrofit.create(PairingApi::class.java)
    }
}
