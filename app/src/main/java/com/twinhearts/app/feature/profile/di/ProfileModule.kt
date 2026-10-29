package com.twinhearts.app.feature.profile.di

import com.twinhearts.app.feature.profile.data.AndroidAvatarEncoder
import com.twinhearts.app.feature.profile.data.AvatarEncoder
import com.twinhearts.app.feature.profile.data.ProfileRepository
import com.twinhearts.app.feature.profile.data.ProfileRepositoryImpl
import com.twinhearts.app.feature.profile.data.remote.ProfileApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Profile feature (R-44). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ProfileModule {

    @Binds
    abstract fun bindProfileRepository(impl: ProfileRepositoryImpl): ProfileRepository

    @Binds
    abstract fun bindAvatarEncoder(impl: AndroidAvatarEncoder): AvatarEncoder

    companion object {
        @Provides
        @Singleton
        fun provideProfileApi(retrofit: Retrofit): ProfileApi = retrofit.create(ProfileApi::class.java)
    }
}
