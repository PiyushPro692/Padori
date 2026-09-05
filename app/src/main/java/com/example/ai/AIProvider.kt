package com.example.ai

data class AIIntentResult(
    val spokenResponse: String,
    val toolName: String? = null,
    val toolArguments: Map<String, String> = emptyMap(),
    val rawModelResponse: String? = null,
    val providerUsed: String = "Local"
)

interface AIProvider {
    val providerName: String

    suspend fun processUserPrompt(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>, // sender to message
        memories: List<Pair<String, String>>, // key to value
        availableToolsDescription: String,
        apiKey: String
    ): AIIntentResult
}
