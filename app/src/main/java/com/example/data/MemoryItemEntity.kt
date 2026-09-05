package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "memory_items")
data class MemoryItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val category: String, // "preference", "fact", "custom_command", "general"
    val key: String,
    val value: String,
    val timestamp: Long = System.currentTimeMillis()
)
