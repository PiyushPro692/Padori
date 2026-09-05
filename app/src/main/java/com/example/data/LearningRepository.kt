package com.example.data

import kotlinx.coroutines.flow.Flow

class LearningRepository(private val learningDao: LearningDao) {

    val allInsights: Flow<List<LearningInsightEntity>> = learningDao.getAllInsights()

    suspend fun getInsightsList(): List<LearningInsightEntity> = learningDao.getInsightsList()

    suspend fun learnOrReinforce(
        category: String,
        key: String,
        description: String,
        initialConfidence: Float = 0.85f
    ) {
        val existing = learningDao.findByKey(key)
        if (existing != null) {
            learningDao.reinforceInsight(key)
        } else {
            learningDao.insertOrUpdate(
                LearningInsightEntity(
                    category = category,
                    insightKey = key,
                    description = description,
                    confidence = initialConfidence,
                    usageCount = 1,
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun removeInsight(insight: LearningInsightEntity) {
        learningDao.delete(insight)
    }

    suspend fun clearInsights() {
        learningDao.clearAll()
    }
}
