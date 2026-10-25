package com.twinhearts.app.core.data

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * The app's local database. Register new entities here only through a feature's SHARED_CHANGES.md,
 * and bump [version] with a migration each time (R-45).
 */
@Database(
    entities = [CacheEntryEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class TwinDatabase : RoomDatabase() {
    abstract fun cacheDao(): CacheDao
}
