package com.twinhearts.app.feature.music.di

import com.twinhearts.app.feature.music.data.MusicRepository
import com.twinhearts.app.feature.music.data.MusicRepositoryImpl
import com.twinhearts.app.feature.music.data.remote.MusicApi
import com.twinhearts.app.feature.music.data.remote.MusicUploadApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/** Why this file exists: Hilt wiring for the Music feature (repository, API, upload API). */
@Module
@InstallIn(SingletonComponent::class)
abstract class MusicModule {

    @Binds
    abstract fun bindMusicRepository(impl: MusicRepositoryImpl): MusicRepository

    companion object {
        @Provides
        @Singleton
        fun provideMusicApi(retrofit: Retrofit): MusicApi = retrofit.create(MusicApi::class.java)

        /**
         * Uploads get their own HTTP client: a 40 MB song on a phone connection can take longer
         * than the app-wide 90 second limit. Everything else is shared with the main client.
         */
        @Provides
        @Singleton
        fun provideMusicUploadApi(retrofit: Retrofit, client: OkHttpClient): MusicUploadApi {
            val slowClient = client.newBuilder()
                .callTimeout(0, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.MINUTES)
                .readTimeout(5, TimeUnit.MINUTES)
                .build()
            return retrofit.newBuilder().callFactory(slowClient).build().create(MusicUploadApi::class.java)
        }
    }
}
