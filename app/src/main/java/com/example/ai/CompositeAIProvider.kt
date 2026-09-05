package com.example.ai

import android.util.Log

class CompositeAIProvider : AIProvider {
    override val providerName: String = "Composite Brain"

    private val geminiProvider = GeminiProvider()
    private val openAIProvider = OpenAIProvider()
    private val localProvider = FallbackLocalProvider()

    suspend fun processWithRouting(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        memories: List<Pair<String, String>>,
        availableToolsDescription: String,
        preferredProvider: String, // "Gemini", "OpenAI", or "Local"
        apiKey: String,
        learnedInsights: List<String> = emptyList()
    ): AIIntentResult {
        val hasKey = apiKey.isNotBlank() && apiKey != "MY_GEMINI_API_KEY"

        if (!hasKey || preferredProvider == "Local") {
            return localProvider.processUserPrompt(
                userPrompt,
                conversationHistory,
                memories,
                availableToolsDescription,
                apiKey
            )
        }

        return try {
            when (preferredProvider) {
                "OpenAI" -> openAIProvider.processUserPrompt(
                    userPrompt,
                    conversationHistory,
                    memories,
                    availableToolsDescription,
                    apiKey
                )
                else -> geminiProvider.processWithInsights(
                    userPrompt,
                    conversationHistory,
                    memories,
                    availableToolsDescription,
                    apiKey,
                    learnedInsights
                )
            }
        } catch (e: Exception) {
            Log.w("CompositeAIProvider", "Primary provider failed (${e.message}), failing over to Local Core.")
            val fallbackResult = localProvider.processUserPrompt(
                userPrompt,
                conversationHistory,
                memories,
                availableToolsDescription,
                apiKey
            )
            fallbackResult.copy(
                spokenResponse = fallbackResult.spokenResponse,
                providerUsed = "${fallbackResult.providerUsed} (Failover: ${e.localizedMessage ?: "Offline"})"
            )
        }
    }

    override suspend fun processUserPrompt(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        memories: List<Pair<String, String>>,
        availableToolsDescription: String,
        apiKey: String
    ): AIIntentResult {
        return processWithRouting(
            userPrompt = userPrompt,
            conversationHistory = conversationHistory,
            memories = memories,
            availableToolsDescription = availableToolsDescription,
            preferredProvider = "Gemini",
            apiKey = apiKey
        )
    }
}
