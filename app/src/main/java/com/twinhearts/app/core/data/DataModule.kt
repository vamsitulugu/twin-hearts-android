package com.twinhearts.app.core.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

// One DataStore file for small settings and the session. Must be declared once, at top level.
private val Context.twinDataStore: DataStore<Preferences> by preferencesDataStore(name = "twin_prefs")

/** Provides the local storage objects: DataStore, Room database and its DAO. */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> = context.twinDataStore

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): TwinDatabase =
        Room.databaseBuilder(context, TwinDatabase::class.java, "twin.db").build()

    @Provides
    fun provideCacheDao(database: TwinDatabase): CacheDao = database.cacheDao()
}
