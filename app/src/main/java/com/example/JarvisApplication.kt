package com.example

import android.app.Application
import com.example.ai.CompositeAIProvider
import com.example.data.AppDatabase
import com.example.data.ConversationRepository
import com.example.data.MemoryRepository
import com.example.data.UserPreferencesRepository
import com.example.tools.ToolRegistry

class JarvisApplication : Application() {
    lateinit var database: AppDatabase
        private set
    lateinit var conversationRepository: ConversationRepository
        private set
    lateinit var memoryRepository: MemoryRepository
        private set
    lateinit var learningRepository: com.example.data.LearningRepository
        private set
    lateinit var userPreferencesRepository: UserPreferencesRepository
        private set
    lateinit var toolRegistry: ToolRegistry
        private set
    lateinit var compositeAIProvider: CompositeAIProvider
        private set

    override fun onCreate() {
        super.onCreate()
        database = AppDatabase.getInstance(this)
        conversationRepository = ConversationRepository(database.conversationDao())
        memoryRepository = MemoryRepository(database.memoryDao())
        learningRepository = com.example.data.LearningRepository(database.learningDao())
        userPreferencesRepository = UserPreferencesRepository(this)
        toolRegistry = ToolRegistry(this)
        compositeAIProvider = CompositeAIProvider()
    }
}
