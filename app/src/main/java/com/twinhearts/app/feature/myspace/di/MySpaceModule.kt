package com.twinhearts.app.feature.myspace.di

import com.twinhearts.app.feature.myspace.data.MySpaceRepository
import com.twinhearts.app.feature.myspace.data.MySpaceRepositoryImpl
import com.twinhearts.app.feature.myspace.data.remote.MySpaceApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class MySpaceModule {

    @Binds
    abstract fun bindMySpaceRepository(impl: MySpaceRepositoryImpl): MySpaceRepository

    companion object {
        @Provides
        @Singleton
        fun provideMySpaceApi(retrofit: Retrofit): MySpaceApi = retrofit.create(MySpaceApi::class.java)
    }
}
