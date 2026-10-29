package com.twinhearts.app.feature.vault.di

import com.twinhearts.app.feature.vault.data.VaultRepository
import com.twinhearts.app.feature.vault.data.VaultRepositoryImpl
import com.twinhearts.app.feature.vault.data.remote.VaultApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class VaultModule {

    @Binds
    abstract fun bindVaultRepository(impl: VaultRepositoryImpl): VaultRepository

    companion object {
        @Provides
        @Singleton
        fun provideVaultApi(retrofit: Retrofit): VaultApi = retrofit.create(VaultApi::class.java)
    }
}
