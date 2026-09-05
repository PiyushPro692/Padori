package com.example.ui

import android.app.Application
import android.content.Context
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.JarvisApplication
import com.example.ai.AIIntentResult
import com.example.data.ChatMessageEntity
import com.example.data.MemoryItemEntity
import com.example.data.UserPreferences
import com.example.services.JarvisForegroundService
import com.example.voice.SpeechManager
import com.example.voice.SpeechState
import com.example.voice.TextToSpeechManager
import com.example.voice.WakeWordDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

class JarvisViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as JarvisApplication
    private val conversationRepo = app.conversationRepository
    private val memoryRepo = app.memoryRepository
    private val learningRepo = app.learningRepository
    private val prefsRepo = app.userPreferencesRepository
    private val toolRegistry = app.toolRegistry
    private val compositeAIProvider = app.compositeAIProvider

    val learningEngine = com.example.ai.JarvisLearningEngine(learningRepo)

    val conversationHistory: StateFlow<List<ChatMessageEntity>> =
        conversationRepo.allMessages.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val memoryList: StateFlow<List<MemoryItemEntity>> =
        memoryRepo.allMemories.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val learnedInsightsList: StateFlow<List<com.example.data.LearningInsightEntity>> =
        learningRepo.allInsights.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    val userPreferences: StateFlow<UserPreferences> = prefsRepo.preferences

    private val _uiState = MutableStateFlow(JarvisUiState())
    val uiState: StateFlow<JarvisUiState> = _uiState.asStateFlow()

    private val speechManager = SpeechManager(application)
    private val ttsManager = TextToSpeechManager(application)
    private var wakeWordDetector: WakeWordDetector? = null

    init {
        updateBatteryLevel()

        // Sync preferences to UI
        viewModelScope.launch {
            userPreferences.collect { prefs ->
                _uiState.value = _uiState.value.copy(
                    isWakeWordActive = prefs.wakeWordEnabled,
                    isSleepMode = prefs.sleepModeEnabled,
                    currentProvider = prefs.aiProvider,
                    hasApiKeyConfigured = prefs.getEffectiveApiKey().isNotBlank() &&
                            prefs.getEffectiveApiKey() != "MY_GEMINI_API_KEY"
                )

                if (prefs.sleepModeEnabled) {
                    _uiState.value = _uiState.value.copy(
                        status = AssistantStatus.SLEEPING,
                        statusMessage = "STANDBY / SLEEP"
                    )
                }

                handleWakeWordService(prefs.wakeWordEnabled)
            }
        }

        // Collect memory items count
        viewModelScope.launch {
            memoryList.collect { memories ->
                _uiState.value = _uiState.value.copy(memoryCount = memories.size)
            }
        }

        // Collect learned insights count
        viewModelScope.launch {
            learnedInsightsList.collect { insights ->
                _uiState.value = _uiState.value.copy(learningInsightsCount = insights.size)
            }
        }

        // Collect Speech recognition states
        viewModelScope.launch {
            speechManager.speechState.collect { state ->
                when (state) {
                    is SpeechState.Idle -> {
                        if (_uiState.value.status == AssistantStatus.LISTENING) {
                            _uiState.value = _uiState.value.copy(
                                status = AssistantStatus.IDLE,
                                statusMessage = "SYSTEM READY"
                            )
                        }
                    }
                    is SpeechState.Listening -> {
                        _uiState.value = _uiState.value.copy(
                            status = AssistantStatus.LISTENING,
                            statusMessage = "LISTENING...",
                            partialSpeech = ""
                        )
                    }
                    is SpeechState.PartialResult -> {
                        _uiState.value = _uiState.value.copy(
                            partialSpeech = state.text,
                            overlayText = state.text,
                            isOverlayJarvisResponse = false
                        )
                    }
                    is SpeechState.FinalResult -> {
                        _uiState.value = _uiState.value.copy(
                            partialSpeech = state.text,
                            overlayText = state.text,
                            isOverlayJarvisResponse = false
                        )
                        processUserInput(state.text)
                    }
                    is SpeechState.Error -> {
                        _uiState.value = _uiState.value.copy(
                            status = AssistantStatus.IDLE,
                            statusMessage = "READY",
                            errorMessage = state.message
                        )
                    }
                }
            }
        }

        // Collect RMS dB for visualizer
        viewModelScope.launch {
            speechManager.rmsDb.collect { rms ->
                if (_uiState.value.status == AssistantStatus.LISTENING) {
                    _uiState.value = _uiState.value.copy(audioRms = rms)
                }
            }
        }

        // Collect TTS speaking state
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { isSpeaking ->
                if (isSpeaking) {
                    _uiState.value = _uiState.value.copy(
                        status = AssistantStatus.SPEAKING,
                        statusMessage = "TRANSMITTING...",
                        audioRms = 0.65f
                    )
                } else if (_uiState.value.status == AssistantStatus.SPEAKING) {
                    _uiState.value = _uiState.value.copy(
                        status = if (_uiState.value.isSleepMode) AssistantStatus.SLEEPING else AssistantStatus.IDLE,
                        statusMessage = if (_uiState.value.isSleepMode) "STANDBY" else "SYSTEM READY",
                        audioRms = 0f
                    )
                }
            }
        }

        // Setup wake word detector
        setupWakeWord()
    }

    private fun updateBatteryLevel() {
        try {
            val filter = IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus = app.registerReceiver(null, filter)
            val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
            val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
            if (level >= 0 && scale > 0) {
                val pct = (level * 100 / scale.toFloat()).toInt()
                _uiState.value = _uiState.value.copy(batteryPercentage = pct)
            }
        } catch (ignored: Exception) {}
    }

    private fun setupWakeWord() {
        wakeWordDetector = WakeWordDetector(app) {
            viewModelScope.launch(Dispatchers.Main) {
                onWakeWordTriggered()
            }
        }
    }

    private fun handleWakeWordService(enabled: Boolean) {
        if (enabled) {
            wakeWordDetector?.startMonitoring()
            try {
                JarvisForegroundService.startService(app)
            } catch (e: Exception) {
                Log.w("JarvisViewModel", "Foreground service launch restriction: ${e.message}")
            }
        } else {
            wakeWordDetector?.stopMonitoring()
            try {
                JarvisForegroundService.stopService(app)
            } catch (ignored: Exception) {}
        }
    }

    private fun onWakeWordTriggered() {
        if (_uiState.value.isSleepMode) {
            // Wake word can awaken JARVIS from sleep!
            prefsRepo.setSleepMode(false)
        }

        ttsManager.speak("Yes, sir?") {
            // Auto start listening for the command immediately after acknowledging
            speechManager.startListening()
        }
    }

    fun onMicPressed() {
        if (_uiState.value.status == AssistantStatus.LISTENING) {
            speechManager.stopListening()
        } else {
            ttsManager.stop()
            speechManager.startListening()
        }
    }

    fun processUserInput(input: String) {
        val trimmed = input.trim()
        if (trimmed.isBlank()) return

        speechManager.resetState()

        viewModelScope.launch {
            // Check if waking from sleep
            if (_uiState.value.isSleepMode) {
                val lower = trimmed.lowercase()
                if (lower.contains("wake") || lower.contains("jarvis") || lower.contains("hello")) {
                    prefsRepo.setSleepMode(false)
                } else {
                    return@launch
                }
            }

            _uiState.value = _uiState.value.copy(
                status = AssistantStatus.THINKING,
                statusMessage = "PROCESSING INTENT...",
                partialSpeech = trimmed,
                overlayText = trimmed,
                isOverlayJarvisResponse = false,
                errorMessage = null
            )

            // Save user message in conversation DB
            conversationRepo.addMessage(sender = "USER", message = trimmed)

            // Prepare history & memories for reasoning
            val recentHistoryEntities = conversationRepo.getRecentMessages(6)
            val historyPairs = recentHistoryEntities.map { it.sender to it.message }
            val memories = memoryRepo.getMemoriesList().map { it.key to it.value }
            val toolsDescription = toolRegistry.buildSystemPromptToolDescriptions()

            val prefs = userPreferences.value
            val apiKey = prefs.getEffectiveApiKey()
            val learnedInsights = learningRepo.getInsightsList().map { "${it.category}: ${it.description} (weight: ${it.confidence})" }

            // Analyze intent via AI Provider
            val intentResult: AIIntentResult = try {
                compositeAIProvider.processWithRouting(
                    userPrompt = trimmed,
                    conversationHistory = historyPairs,
                    memories = memories,
                    availableToolsDescription = toolsDescription,
                    preferredProvider = prefs.aiProvider,
                    apiKey = apiKey,
                    learnedInsights = learnedInsights
                )
            } catch (e: Exception) {
                AIIntentResult(
                    spokenResponse = "My apologies, I encountered a reasoning malfunction.",
                    providerUsed = "Error (${e.localizedMessage})"
                )
            }

            var toolExecutionResultText: String? = null

            // Execute selected tool if any
            if (!intentResult.toolName.isNullOrBlank()) {
                val toolName = intentResult.toolName

                when (toolName) {
                    "sleep" -> {
                        prefsRepo.setSleepMode(true)
                        toolExecutionResultText = "Standby mode engaged."
                    }
                    "remember" -> {
                        val key = intentResult.toolArguments["key"] ?: "fact"
                        val value = intentResult.toolArguments["value"] ?: trimmed
                        memoryRepo.remember("preference", key, value)
                        toolExecutionResultText = "Stored in memory core: '$value'"
                    }
                    "forget" -> {
                        val key = intentResult.toolArguments["key"] ?: ""
                        if (key.isNotBlank()) {
                            memoryRepo.forgetByKey(key)
                        } else {
                            memoryRepo.clearAll()
                        }
                        toolExecutionResultText = "Memory entry cleared."
                    }
                    else -> {
                        // Execute on Android ToolRegistry
                        val result = toolRegistry.executeTool(toolName, intentResult.toolArguments)
                        toolExecutionResultText = result.message
                    }
                }
            }

            val finalSpeech = intentResult.spokenResponse

            // Save JARVIS message in conversation DB
            conversationRepo.addMessage(
                sender = "JARVIS",
                message = finalSpeech,
                toolName = intentResult.toolName,
                toolArgs = intentResult.toolArguments.toString(),
                toolResult = toolExecutionResultText
            )

            _uiState.value = _uiState.value.copy(
                spokenText = finalSpeech,
                overlayText = finalSpeech,
                isOverlayJarvisResponse = true,
                lastExecutedTool = intentResult.toolName,
                lastToolResult = toolExecutionResultText,
                partialSpeech = ""
            )

            // Autonomous machine learning adaptation pass
            launch(Dispatchers.IO) {
                try {
                    learningEngine.analyzeAndLearnFromInteraction(
                        userPrompt = trimmed,
                        spokenResponse = finalSpeech,
                        toolName = intentResult.toolName,
                        toolResult = toolExecutionResultText
                    )
                } catch (e: Exception) {
                    Log.w("JarvisViewModel", "Machine learning interaction analysis skipped: ${e.message}")
                }
            }

            // Voice response via TTS
            val isInQuietHours = checkQuietHours(prefs)
            val shouldSpeak = prefs.autoSpeak && !_uiState.value.isSleepMode && !isInQuietHours

            if (shouldSpeak) {
                ttsManager.speak(
                    text = finalSpeech,
                    speechRate = prefs.speechRate,
                    speechPitch = prefs.speechPitch
                ) {
                    _uiState.value = _uiState.value.copy(
                        status = AssistantStatus.IDLE,
                        statusMessage = "SYSTEM READY"
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(
                    status = if (_uiState.value.isSleepMode) AssistantStatus.SLEEPING else AssistantStatus.IDLE,
                    statusMessage = if (_uiState.value.isSleepMode) "STANDBY" else "SYSTEM READY"
                )
            }
        }
    }

    private fun checkQuietHours(prefs: UserPreferences): Boolean {
        if (!prefs.quietHoursEnabled) return false
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val start = prefs.quietHoursStartHour
        val end = prefs.quietHoursEndHour

        return if (start > end) {
            // Over midnight, e.g. 22:00 to 07:00
            currentHour >= start || currentHour < end
        } else {
            currentHour in start until end
        }
    }

    fun toggleSleepMode() {
        val nextState = !_uiState.value.isSleepMode
        prefsRepo.setSleepMode(nextState)
        if (nextState) {
            ttsManager.stop()
            speechManager.cancel()
            _uiState.value = _uiState.value.copy(
                status = AssistantStatus.SLEEPING,
                statusMessage = "STANDBY / SLEEP",
                spokenText = "Sleep mode enabled. Systems on standby."
            )
        } else {
            _uiState.value = _uiState.value.copy(
                status = AssistantStatus.IDLE,
                statusMessage = "SYSTEM READY",
                spokenText = "All systems online. How may I assist?"
            )
            ttsManager.speak("Online and ready, sir.")
        }
    }

    fun toggleWakeWord(enabled: Boolean) {
        prefsRepo.setWakeWord(enabled)
    }

    fun setAiProvider(provider: String) {
        prefsRepo.updatePreferences { it.copy(aiProvider = provider) }
    }

    fun setCustomApiKey(key: String) {
        prefsRepo.updatePreferences { it.copy(customApiKey = key.trim()) }
    }

    fun updateSpeechRate(rate: Float) {
        prefsRepo.updatePreferences { it.copy(speechRate = rate) }
    }

    fun updateSpeechPitch(pitch: Float) {
        prefsRepo.updatePreferences { it.copy(speechPitch = pitch) }
    }

    fun toggleAutoSpeak(enabled: Boolean) {
        prefsRepo.updatePreferences { it.copy(autoSpeak = enabled) }
    }

    fun toggleQuietHours(enabled: Boolean) {
        prefsRepo.updatePreferences { it.copy(quietHoursEnabled = enabled) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            conversationRepo.clearHistory()
        }
    }

    fun clearMemory() {
        viewModelScope.launch {
            memoryRepo.clearAll()
        }
    }

    fun deleteMemory(item: MemoryItemEntity) {
        viewModelScope.launch {
            memoryRepo.forget(item)
        }
    }

    fun addManualMemory(key: String, value: String) {
        if (key.isNotBlank() && value.isNotBlank()) {
            viewModelScope.launch {
                memoryRepo.remember("preference", key, value)
            }
        }
    }

    fun setSettingsOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isSettingsOpen = open)
    }

    fun setMemoryOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isMemoryOpen = open)
    }

    fun setToolsOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isToolsOpen = open)
    }

    fun setTextInputOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isTextInputOpen = open, textInputQuery = "")
    }

    fun updateTextInputQuery(query: String) {
        _uiState.value = _uiState.value.copy(textInputQuery = query)
    }

    fun dismissError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun setGlassOverlayVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(isGlassOverlayVisible = visible)
    }

    fun toggleGlassOverlay() {
        val next = !_uiState.value.isGlassOverlayVisible
        _uiState.value = _uiState.value.copy(isGlassOverlayVisible = next)
        if (next && _uiState.value.status != AssistantStatus.LISTENING) {
            onMicPressed()
        }
    }

    fun updateOverlayText(text: String, isJarvis: Boolean) {
        _uiState.value = _uiState.value.copy(
            overlayText = text,
            isOverlayJarvisResponse = isJarvis
        )
    }

    fun setLearningDialogOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isLearningDialogOpen = open, trainingStatusMessage = null)
    }

    fun triggerModelTrainingCycle() {
        if (_uiState.value.isTrainingActive) return

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isTrainingActive = true,
                trainingStatusMessage = "Ingesting neural memory & interaction traces..."
            )

            val history = conversationRepo.getAllMessagesList()
            
            kotlinx.coroutines.delay(800)
            _uiState.value = _uiState.value.copy(
                trainingStatusMessage = "Executing gradient descent on intent & tool vectors..."
            )
            
            val result = learningEngine.runTrainingCycle(history)
            kotlinx.coroutines.delay(600)

            _uiState.value = _uiState.value.copy(
                isTrainingActive = false,
                modelFitnessScore = result.modelFitnessScore,
                trainingStatusMessage = "Optimization complete! +${result.newPatternsDiscovered + result.insightsReinforced} weights refined. Fitness score: ${String.format(java.util.Locale.ROOT, "%.1f%%", result.modelFitnessScore)}"
            )
        }
    }

    fun deleteInsight(insight: com.example.data.LearningInsightEntity) {
        viewModelScope.launch {
            learningRepo.removeInsight(insight)
        }
    }

    fun clearAllLearnedInsights() {
        viewModelScope.launch {
            learningRepo.clearInsights()
            _uiState.value = _uiState.value.copy(modelFitnessScore = 80.0f)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechManager.destroy()
        ttsManager.shutdown()
        wakeWordDetector?.destroy()
    }
}
