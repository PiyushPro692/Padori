package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserPreferences(
    val aiProvider: String = "Gemini", // "Gemini" or "OpenAI"
    val customApiKey: String = "",
    val speechRate: Float = 1.05f,
    val speechPitch: Float = 0.95f,
    val autoSpeak: Boolean = true,
    val wakeWordEnabled: Boolean = false,
    val sleepModeEnabled: Boolean = false,
    val quietHoursEnabled: Boolean = false,
    val quietHoursStartHour: Int = 22,
    val quietHoursEndHour: Int = 7,
    val modelName: String = "gemini-3.5-flash"
) {
    fun getEffectiveApiKey(): String {
        return if (customApiKey.isNotBlank()) customApiKey else BuildConfig.GEMINI_API_KEY
    }
}

class UserPreferencesRepository(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("jarvis_user_prefs", Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(loadPreferences())
    val preferences: StateFlow<UserPreferences> = _preferences.asStateFlow()

    private fun loadPreferences(): UserPreferences {
        return UserPreferences(
            aiProvider = prefs.getString("ai_provider", "Gemini") ?: "Gemini",
            customApiKey = prefs.getString("custom_api_key", "") ?: "",
            speechRate = prefs.getFloat("speech_rate", 1.05f),
            speechPitch = prefs.getFloat("speech_pitch", 0.95f),
            autoSpeak = prefs.getBoolean("auto_speak", true),
            wakeWordEnabled = prefs.getBoolean("wake_word_enabled", false),
            sleepModeEnabled = prefs.getBoolean("sleep_mode_enabled", false),
            quietHoursEnabled = prefs.getBoolean("quiet_hours_enabled", false),
            quietHoursStartHour = prefs.getInt("quiet_hours_start_hour", 22),
            quietHoursEndHour = prefs.getInt("quiet_hours_end_hour", 7),
            modelName = prefs.getString("model_name", "gemini-3.5-flash") ?: "gemini-3.5-flash"
        )
    }

    fun updatePreferences(update: (UserPreferences) -> UserPreferences) {
        val current = _preferences.value
        val updated = update(current)
        prefs.edit().apply {
            putString("ai_provider", updated.aiProvider)
            putString("custom_api_key", updated.customApiKey)
            putFloat("speech_rate", updated.speechRate)
            putFloat("speech_pitch", updated.speechPitch)
            putBoolean("auto_speak", updated.autoSpeak)
            putBoolean("wake_word_enabled", updated.wakeWordEnabled)
            putBoolean("sleep_mode_enabled", updated.sleepModeEnabled)
            putBoolean("quiet_hours_enabled", updated.quietHoursEnabled)
            putInt("quiet_hours_start_hour", updated.quietHoursStartHour)
            putInt("quiet_hours_end_hour", updated.quietHoursEndHour)
            putString("model_name", updated.modelName)
            apply()
        }
        _preferences.value = updated
    }

    fun setSleepMode(enabled: Boolean) {
        updatePreferences { it.copy(sleepModeEnabled = enabled) }
    }

    fun setWakeWord(enabled: Boolean) {
        updatePreferences { it.copy(wakeWordEnabled = enabled) }
    }
}
