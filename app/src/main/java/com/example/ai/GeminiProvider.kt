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

class GeminiProvider(private val modelName: String = "gemini-3.5-flash") : AIProvider {
    override val providerName: String = "Google Gemini ($modelName)"

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
    ): AIIntentResult {
        return processWithInsights(
            userPrompt = userPrompt,
            conversationHistory = conversationHistory,
            memories = memories,
            availableToolsDescription = availableToolsDescription,
            apiKey = apiKey,
            learnedInsights = emptyList()
        )
    }

    suspend fun processWithInsights(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        memories: List<Pair<String, String>>,
        availableToolsDescription: String,
        apiKey: String,
        learnedInsights: List<String> = emptyList()
    ): AIIntentResult = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            throw IllegalArgumentException("No valid Gemini API key configured.")
        }

        val systemPrompt = PromptManager.buildSystemInstruction(availableToolsDescription, memories, learnedInsights)

        // Build contents array
        val contentsArray = JSONArray()

        // Add recent conversation history (up to last 6 turns)
        val recentTurns = conversationHistory.takeLast(6)
        for (turn in recentTurns) {
            val role = if (turn.first == "USER") "user" else "model"
            val turnObj = JSONObject().apply {
                put("role", role)
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", turn.second) })
                })
            }
            contentsArray.put(turnObj)
        }

        // Add current prompt
        contentsArray.put(JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", userPrompt) })
            })
        })

        // Request JSON object
        val requestJson = JSONObject().apply {
            put("contents", contentsArray)
            put("systemInstruction", JSONObject().apply {
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", systemPrompt) })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.3)
                put("responseMimeType", "application/json")
            })
        }

        val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
        val url = "https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            val errorBody = response.body?.string() ?: "HTTP ${response.code}"
            throw RuntimeException("Gemini API error ($response.code): $errorBody")
        }

        val responseBody = response.body?.string() ?: throw RuntimeException("Empty response from Gemini")
        parseGeminiResponse(responseBody)
    }

    private fun parseGeminiResponse(rawJson: String): AIIntentResult {
        val root = JSONObject(rawJson)
        val candidates = root.optJSONArray("candidates")
        val firstCandidate = candidates?.optJSONObject(0)
        val content = firstCandidate?.optJSONObject("content")
        val parts = content?.optJSONArray("parts")
        val rawText = parts?.optJSONObject(0)?.optString("text") ?: ""

        // Parse structured JSON response
        try {
            val jsonClean = extractJsonString(rawText)
            val parsedObj = JSONObject(jsonClean)

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

            return AIIntentResult(
                spokenResponse = finalSpoken,
                toolName = toolName,
                toolArguments = argsMap,
                rawModelResponse = rawText,
                providerUsed = providerName
            )
        } catch (e: Exception) {
            return AIIntentResult(
                spokenResponse = rawText.ifBlank { "Acknowledged, sir." },
                toolName = null,
                rawModelResponse = rawText,
                providerUsed = providerName
            )
        }
    }

    private fun extractJsonString(text: String): String {
        val trimmed = text.trim()
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed
        val firstBrace = trimmed.indexOf('{')
        val lastBrace = trimmed.lastIndexOf('}')
        if (firstBrace != -1 && lastBrace != -1 && lastBrace > firstBrace) {
            return trimmed.substring(firstBrace, lastBrace + 1)
        }
        return trimmed
    }
}
