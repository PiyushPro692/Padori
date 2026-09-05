package com.example.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.UUID

class TextToSpeechManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private var currentCompletionCallback: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                // Prefer UK English for classic JARVIS persona if available, else default Locale
                val result = tts?.setLanguage(Locale.UK)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.setLanguage(Locale.getDefault())
                }
                setupListener()
            } else {
                Log.e("TTSManager", "TextToSpeech initialization failed: $status")
            }
        }
    }

    private fun setupListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _isSpeaking.value = true
            }

            override fun onDone(utteranceId: String?) {
                _isSpeaking.value = false
                currentCompletionCallback?.invoke()
                currentCompletionCallback = null
            }

            override fun onError(utteranceId: String?) {
                _isSpeaking.value = false
                currentCompletionCallback?.invoke()
                currentCompletionCallback = null
            }
        })
    }

    fun speak(
        text: String,
        speechRate: Float = 1.05f,
        speechPitch: Float = 0.95f,
        onComplete: (() -> Unit)? = null
    ) {
        if (!isInitialized || text.isBlank()) {
            onComplete?.invoke()
            return
        }

        stop()
        currentCompletionCallback = onComplete

        tts?.setSpeechRate(speechRate)
        tts?.setPitch(speechPitch)

        val utteranceId = UUID.randomUUID().toString()
        val result = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (result != TextToSpeech.SUCCESS) {
            _isSpeaking.value = false
            onComplete?.invoke()
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
        _isSpeaking.value = false
        currentCompletionCallback = null
    }

    fun shutdown() {
        stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
