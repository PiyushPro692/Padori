package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LearningDao {
    @Query("SELECT * FROM learning_insights ORDER BY timestamp DESC")
    fun getAllInsights(): Flow<List<LearningInsightEntity>>

    @Query("SELECT * FROM learning_insights ORDER BY timestamp DESC")
    suspend fun getInsightsList(): List<LearningInsightEntity>

    @Query("SELECT * FROM learning_insights WHERE insightKey = :key LIMIT 1")
    suspend fun findByKey(key: String): LearningInsightEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(insight: LearningInsightEntity): Long

    @Query("UPDATE learning_insights SET usageCount = usageCount + 1, confidence = MIN(1.0, confidence + 0.05), timestamp = :time WHERE insightKey = :key")
    suspend fun reinforceInsight(key: String, time: Long = System.currentTimeMillis())

    @Delete
    suspend fun delete(insight: LearningInsightEntity)

    @Query("DELETE FROM learning_insights")
    suspend fun clearAll()
}
