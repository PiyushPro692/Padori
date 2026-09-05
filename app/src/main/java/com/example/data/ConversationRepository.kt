package com.example.data

import kotlinx.coroutines.flow.Flow

class ConversationRepository(private val dao: ConversationDao) {
    val allMessages: Flow<List<ChatMessageEntity>> = dao.getAllMessages()

    suspend fun getAllMessagesList(): List<ChatMessageEntity> {
        return dao.getAllMessagesList()
    }

    suspend fun getRecentMessages(limit: Int = 10): List<ChatMessageEntity> {
        return dao.getRecentMessages(limit).reversed()
    }

    suspend fun addMessage(
        sender: String,
        message: String,
        toolName: String? = null,
        toolArgs: String? = null,
        toolResult: String? = null
    ): Long {
        return dao.insertMessage(
            ChatMessageEntity(
                sender = sender,
                message = message,
                toolName = toolName,
                toolArgs = toolArgs,
                toolResult = toolResult
            )
        )
    }

    suspend fun clearHistory() {
        dao.clearAllMessages()
    }
}
