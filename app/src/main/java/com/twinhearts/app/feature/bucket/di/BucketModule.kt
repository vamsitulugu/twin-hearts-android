package com.twinhearts.app.feature.bucket.di

import com.twinhearts.app.feature.bucket.data.BucketRepository
import com.twinhearts.app.feature.bucket.data.BucketRepositoryImpl
import com.twinhearts.app.feature.bucket.data.remote.BucketApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class BucketModule {

    @Binds
    abstract fun bindBucketRepository(impl: BucketRepositoryImpl): BucketRepository

    companion object {
        @Provides
        @Singleton
        fun provideBucketApi(retrofit: Retrofit): BucketApi = retrofit.create(BucketApi::class.java)
    }
}
