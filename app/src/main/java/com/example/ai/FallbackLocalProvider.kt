package com.example.ai

import java.util.Locale

class FallbackLocalProvider : AIProvider {
    override val providerName: String = "JARVIS Local Core"

    override suspend fun processUserPrompt(
        userPrompt: String,
        conversationHistory: List<Pair<String, String>>,
        memories: List<Pair<String, String>>,
        availableToolsDescription: String,
        apiKey: String
    ): AIIntentResult {
        val clean = userPrompt.trim()
        val lower = clean.lowercase(Locale.ROOT)

        // 1. Sleep commands
        if (lower.contains("sleep") || lower == "standby" || lower == "go to sleep" || lower == "shut down") {
            return AIIntentResult(
                spokenResponse = "Sleep mode enabled. Standing by.",
                toolName = "sleep",
                toolArguments = emptyMap(),
                providerUsed = providerName
            )
        }

        // 2. Wake-up / Greetings
        if (lower == "hey jarvis" || lower == "jarvis" || lower == "hello" || lower == "hi" || lower == "wake up") {
            return AIIntentResult(
                spokenResponse = "At your service, sir. How may I assist you?",
                toolName = null,
                providerUsed = providerName
            )
        }

        // 3. App Launching: "open youtube", "launch chrome", "start spotify", "can you open x"
        val openAppRegex = Regex(
            "^(?:hey\\s+jarvis[,\\s]*)?(?:jarvis[,\\s]*)?(?:please\\s+)?(?:can\\s+you\\s+)?(?:open|launch|start|run|take\\s+me\\s+to)\\s+(?:the\\s+)?(?:app\\s+)?([a-zA-Z0-9\\s]+)$",
            RegexOption.IGNORE_CASE
        )
        val appMatch = openAppRegex.find(lower)
        if (appMatch != null) {
            var appTarget = appMatch.groupValues[1].trim()
            appTarget = appTarget.removeSuffix("app").removeSuffix("website").trim()

            // If user says "open google", "open google website"
            if (appTarget.equals("google", ignoreCase = true) || appTarget.equals("google.com", ignoreCase = true)) {
                return AIIntentResult(
                    spokenResponse = "Opening Google.",
                    toolName = "open_website",
                    toolArguments = mapOf("url" to "https://www.google.com"),
                    providerUsed = providerName
                )
            }

            val canonicalTarget = when {
                appTarget.equals("youtube", ignoreCase = true) -> "YouTube"
                appTarget.equals("whatsapp", ignoreCase = true) -> "WhatsApp"
                appTarget.equals("tiktok", ignoreCase = true) -> "TikTok"
                else -> appTarget.split(" ").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
            }

            return AIIntentResult(
                spokenResponse = "Opening $canonicalTarget.",
                toolName = "open_app",
                toolArguments = mapOf("app_name" to canonicalTarget),
                providerUsed = providerName
            )
        }

        // 4. Web Search: "search google for x", "search x", "google x", "look up x"
        val searchRegex = Regex(
            "^(?:hey\\s+jarvis[,\\s]*)?(?:jarvis[,\\s]*)?(?:please\\s+)?(?:search|google|look\\s+up|find)\\s+(?:google\\s+for\\s+|for\\s+|on\\s+google\\s+)?([a-zA-Z0-9\\s_\\-\\.]+?)(?:\\s+on\\s+google)?$",
            RegexOption.IGNORE_CASE
        )
        val searchMatch = searchRegex.find(lower)
        if (searchMatch != null) {
            val rawMatch = searchRegex.find(userPrompt.trim())
            val query = rawMatch?.groupValues?.get(1)?.trim() ?: searchMatch.groupValues[1].trim()
            if (query.isNotBlank()) {
                return AIIntentResult(
                    spokenResponse = "Searching Google for $query.",
                    toolName = "web_search",
                    toolArguments = mapOf("query" to query),
                    providerUsed = providerName
                )
            }
        }

        // 5. Battery
        if (lower.contains("battery") || lower.contains("power level") || lower.contains("charge percentage")) {
            return AIIntentResult(
                spokenResponse = "Checking power systems.",
                toolName = "get_battery_status",
                toolArguments = emptyMap(),
                providerUsed = providerName
            )
        }

        // 6. Time & Date
        if (lower.contains("what time") || lower.contains("the time") || lower == "time") {
            return AIIntentResult(
                spokenResponse = "Accessing current system time.",
                toolName = "get_time",
                toolArguments = emptyMap(),
                providerUsed = providerName
            )
        }
        if (lower.contains("what date") || lower.contains("today's date") || lower.contains("what day") || lower == "date") {
            return AIIntentResult(
                spokenResponse = "Accessing current date.",
                toolName = "get_date",
                toolArguments = emptyMap(),
                providerUsed = providerName
            )
        }

        // 7. Device Info
        if (lower.contains("device info") || lower.contains("device status") || lower.contains("what phone") || lower.contains("system status")) {
            return AIIntentResult(
                spokenResponse = "Reading hardware telemetries.",
                toolName = "get_device_info",
                toolArguments = emptyMap(),
                providerUsed = providerName
            )
        }

        // 8. Alarm: "set an alarm for 7 am", "set alarm 7:30"
        if (lower.contains("alarm") || lower.contains("wake me up")) {
            val timeRegex = Regex("(\\d{1,2}(?::\\d{2})?\\s*(?:am|pm)?)", RegexOption.IGNORE_CASE)
            val timeMatch = timeRegex.find(lower)
            val parsedTime = timeMatch?.value ?: "7:00 AM"
            return AIIntentResult(
                spokenResponse = "Configuring alarm for $parsedTime.",
                toolName = "set_alarm",
                toolArguments = mapOf("time" to parsedTime),
                providerUsed = providerName
            )
        }

        // 9. Camera: "open camera", "take a picture", "take photo"
        if (lower.contains("camera") || lower.contains("take a picture") || lower.contains("take photo")) {
            return AIIntentResult(
                spokenResponse = "Initializing optical sensor.",
                toolName = "open_camera",
                toolArguments = emptyMap(),
                providerUsed = providerName
            )
        }

        // 10. Settings: "open wifi settings", "open bluetooth settings", "open settings"
        if (lower.contains("setting")) {
            val section = when {
                lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("internet") -> "wifi"
                lower.contains("bluetooth") -> "bluetooth"
                lower.contains("display") || lower.contains("screen") || lower.contains("brightness") -> "display"
                lower.contains("sound") || lower.contains("audio") || lower.contains("volume") -> "sound"
                lower.contains("battery") -> "battery"
                lower.contains("app") -> "apps"
                lower.contains("location") || lower.contains("gps") -> "location"
                else -> "general"
            }
            return AIIntentResult(
                spokenResponse = "Opening $section settings.",
                toolName = "open_settings",
                toolArguments = mapOf("section" to section),
                providerUsed = providerName
            )
        }

        // 11. Website Opening: "open wikipedia.org", "open https://..."
        if (lower.startsWith("http://") || lower.startsWith("https://") || lower.contains(".com") || lower.contains(".org") || lower.contains(".net")) {
            val url = clean.split(" ").firstOrNull { it.contains(".") } ?: clean
            return AIIntentResult(
                spokenResponse = "Navigating to website.",
                toolName = "open_website",
                toolArguments = mapOf("url" to url),
                providerUsed = providerName
            )
        }

        // 12. Memory commands: "remember that ...", "remember ..."
        if (lower.startsWith("remember that ") || lower.startsWith("remember ")) {
            val toRemember = clean.substringAfter("remember", "").substringAfter("that", "").trim()
            return AIIntentResult(
                spokenResponse = "Understood. I have committed that to internal memory.",
                toolName = "remember",
                toolArguments = mapOf("key" to "fact", "value" to toRemember),
                providerUsed = providerName
            )
        }

        // 13. Contextual search / follow-up:
        // If previous conversation was about searching or browsing, resolve query as search
        if (conversationHistory.isNotEmpty()) {
            val lastUserTurn = conversationHistory.findLast { it.first == "USER" }?.second?.lowercase() ?: ""
            if (lastUserTurn.contains("search") || lastUserTurn.contains("google")) {
                return AIIntentResult(
                    spokenResponse = "Searching Google for $clean.",
                    toolName = "web_search",
                    toolArguments = mapOf("query" to clean),
                    providerUsed = providerName
                )
            }
        }

        // Default conversational response
        return AIIntentResult(
            spokenResponse = "Understood, sir. Processing: '$clean'. All systems remain at peak readiness.",
            toolName = null,
            providerUsed = providerName
        )
    }
}
