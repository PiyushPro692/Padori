package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "learning_insights")
data class LearningInsightEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // e.g. "USER_PREFERENCE", "BEHAVIOR_PATTERN", "COMMUNICATION_STYLE", "TOOL_ADAPTATION"
    val insightKey: String,
    val description: String,
    val confidence: Float = 0.85f, // 0.0 to 1.0
    val usageCount: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)
