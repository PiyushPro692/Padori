package com.example.data

import kotlinx.coroutines.flow.Flow

class MemoryRepository(private val dao: MemoryDao) {
    val allMemories: Flow<List<MemoryItemEntity>> = dao.getAllMemories()

    suspend fun getMemoriesList(): List<MemoryItemEntity> {
        return dao.getMemoriesList()
    }

    suspend fun remember(category: String = "general", key: String, value: String): Long {
        return dao.insertMemory(
            MemoryItemEntity(
                category = category,
                key = key,
                value = value
            )
        )
    }

    suspend fun forget(memory: MemoryItemEntity) {
        dao.deleteMemory(memory)
    }

    suspend fun forgetByKey(key: String) {
        dao.deleteByKey(key)
    }

    suspend fun clearAll() {
        dao.clearAllMemories()
    }
}
