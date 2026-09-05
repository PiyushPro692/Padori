package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MemoryDao {
    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    fun getAllMemories(): Flow<List<MemoryItemEntity>>

    @Query("SELECT * FROM memory_items ORDER BY timestamp DESC")
    suspend fun getMemoriesList(): List<MemoryItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMemory(memory: MemoryItemEntity): Long

    @Delete
    suspend fun deleteMemory(memory: MemoryItemEntity)

    @Query("DELETE FROM memory_items WHERE `key` = :key")
    suspend fun deleteByKey(key: String)

    @Query("DELETE FROM memory_items")
    suspend fun clearAllMemories()
}
