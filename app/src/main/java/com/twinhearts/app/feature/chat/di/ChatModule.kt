package com.twinhearts.app.feature.chat.di

import com.twinhearts.app.feature.chat.data.ChatRepository
import com.twinhearts.app.feature.chat.data.ChatRepositoryImpl
import com.twinhearts.app.feature.chat.data.remote.ChatApi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import retrofit2.Retrofit
import javax.inject.Singleton

/** Hilt wiring for the Chat feature. */
@Module
@InstallIn(SingletonComponent::class)
abstract class ChatModule {

    @Binds
    abstract fun bindChatRepository(impl: ChatRepositoryImpl): ChatRepository

    companion object {
        @Provides
        @Singleton
        fun provideChatApi(retrofit: Retrofit): ChatApi = retrofit.create(ChatApi::class.java)
    }
}
