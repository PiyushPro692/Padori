package com.example.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class OpenAIProvider(private val modelName: String = "gpt-4o-mini") : AIProvider {
    override val providerName: String = "OpenAI ($modelName)"

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    override suspend fun processUserPrompt(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        memories: List<Pair<String, String>>,
        availableToolsDescription: String,
        apiKey: String
    ): AIIntentResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) {
            throw IllegalArgumentException("No OpenAI API key provided in settings.")
        }

        val systemPrompt = PromptManager.buildSystemInstruction(availableToolsDescription, memories)
        val messagesArray = JSONArray()

        messagesArray.put(JSONObject().apply {
            put("role", "system")
            put("content", systemPrompt)
        })

        val recentTurns = conversationHistory.takeLast(6)
        for (turn in recentTurns) {
            val role = if (turn.first == "USER") "user" else "assistant"
            messagesArray.put(JSONObject().apply {
                put("role", role)
                put("content", turn.second)
            })
        }

        messagesArray.put(JSONObject().apply {
            put("role", "user")
            put("content", userPrompt)
        })

        val requestJson = JSONObject().apply {
            put("model", modelName)
            put("messages", messagesArray)
            put("temperature", 0.3)
            put("response_format", JSONObject().apply {
                put("type", "json_object")
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://api.openai.com/v1/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "HTTP ${response.code}"
            throw RuntimeException("OpenAI API error ($response.code): $errorBody")
        }

        val responseBody = response.body?.string() ?: throw RuntimeException("Empty response from OpenAI")
        parseOpenAIResponse(responseBody)
    }

    private fun parseOpenAIResponse(rawJson: String): AIIntentResult {
        val root = JSONObject(rawJson)
        val choices = root.optJSONArray("choices")
        val firstChoice = choices?.optJSONObject(0)
        val message = firstChoice?.optJSONObject("message")
        val content = message?.optString("content") ?: ""

        return try {
            val parsedObj = JSONObject(content)
            val action = parsedObj.optString("action", "none")
            val spokenResponse = parsedObj.optString("response", "")
            val argsObj = parsedObj.optJSONObject("arguments")
            val argsMap = mutableMapOf<String, String>()

            if (argsObj != null) {
                val keys = argsObj.keys()
                while (keys.hasNext()) {
                    val key = keys.next()
                    argsMap[key] = argsObj.optString(key)
                }
            }

            val toolName = if (action.isNotBlank() && action != "none") action else null
            val finalSpoken = if (spokenResponse.isNotBlank()) spokenResponse else "Understood."

            AIIntentResult(
                spokenResponse = finalSpoken,
                toolName = toolName,
                toolArguments = argsMap,
                rawModelResponse = content,
                providerUsed = providerName
            )
        } catch (e: Exception) {
            AIIntentResult(
                spokenResponse = content.ifBlank { "Acknowledged." },
                toolName = null,
                rawModelResponse = content,
                providerUsed = providerName
            )
        }
    }
}
