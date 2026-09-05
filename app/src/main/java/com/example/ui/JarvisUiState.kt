package com.example.ui

enum class AssistantStatus {
    IDLE,
    LISTENING,
    THINKING,
    SPEAKING,
    SLEEPING,
    ERROR
}

data class JarvisUiState(
    val status: AssistantStatus = AssistantStatus.IDLE,
    val statusMessage: String = "SYSTEM READY",
    val spokenText: String = "Good day, sir. JARVIS online and awaiting your command.",
    val partialSpeech: String = "",
    val lastExecutedTool: String? = null,
    val lastToolResult: String? = null,
    val audioRms: Float = 0f,
    val isWakeWordActive: Boolean = false,
    val isSleepMode: Boolean = false,
    val currentProvider: String = "Gemini",
    val hasApiKeyConfigured: Boolean = true,
    val memoryCount: Int = 0,
    val batteryPercentage: Int = 100,
    val isSettingsOpen: Boolean = false,
    val isMemoryOpen: Boolean = false,
    val isToolsOpen: Boolean = false,
    val isTextInputOpen: Boolean = false,
    val textInputQuery: String = "",
    val errorMessage: String? = null,
    val isGlassOverlayVisible: Boolean = false,
    val overlayText: String = "",
    val isOverlayJarvisResponse: Boolean = false,
    val isLearningDialogOpen: Boolean = false,
    val isTrainingActive: Boolean = false,
    val trainingStatusMessage: String? = null,
    val learningInsightsCount: Int = 0,
    val modelFitnessScore: Float = 94.2f
)
