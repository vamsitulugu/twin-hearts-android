package com.twinhearts.app.feature.chat.di

import com.twinhearts.app.feature.chat.data.ChatLocalStore
import com.twinhearts.app.feature.chat.data.ChatRepository
import com.twinhearts.app.feature.chat.data.ChatRepositoryImpl
import com.twinhearts.app.feature.chat.data.DataStoreChatLocalStore
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Why this file exists: Hilt wiring for the Chat feature (repository, phone-only settings, API). */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    @Binds
    abstract fun bindChatLocalStore(impl: DataStoreChatLocalStore): ChatLocalStore

    companion object {
        @Provides
        @Singleton
        fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)
    }
}
