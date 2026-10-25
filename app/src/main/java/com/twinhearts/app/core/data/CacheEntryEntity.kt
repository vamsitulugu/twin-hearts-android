package com.twinhearts.app.core.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Upsert

/**
 * A tiny key/value cache table. Room refuses to build a database with no tables, so the
 * foundation ships this one. Features that need real offline tables add their own entities (R-45).
 */
@Entity(tableName = "cache_entry")
data class CacheEntryEntity(
    @PrimaryKey val key: String,
    val json: String,
    val updatedAtMillis: Long,
)

@Dao
interface CacheDao {
    @Query("SELECT * FROM cache_entry WHERE `key` = :key LIMIT 1")
    suspend fun get(key: String): CacheEntryEntity?

    @Upsert
    suspend fun put(entry: CacheEntryEntity)

    @Query("DELETE FROM cache_entry")
    suspend fun clear()
}
