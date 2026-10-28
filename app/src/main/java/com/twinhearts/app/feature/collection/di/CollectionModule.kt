package com.twinhearts.app.feature.collection.di

import com.twinhearts.app.feature.collection.data.CollectionRepository
import com.twinhearts.app.feature.collection.data.CollectionRepositoryImpl
import com.twinhearts.app.feature.collection.data.remote.CollectionApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CollectionModule {

    @Binds
    abstract fun bindCollectionRepository(impl: CollectionRepositoryImpl): CollectionRepository

    companion object {
        @Provides
        @Singleton
        fun provideCollectionApi(retrofit: Retrofit): CollectionApi = retrofit.create(CollectionApi::class.java)
    }
}
