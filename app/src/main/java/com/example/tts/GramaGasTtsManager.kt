package com.example.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import com.example.model.AppLanguage
import java.util.Locale

private const val TAG = "GramaGasTts"

class GramaGasTtsManager(context: Context) {
    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingTextToSpeak: String? = null
    private var pendingLanguage: AppLanguage? = null
    private var onSpeakingFinishedListener: (() -> Unit)? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                isInitialized = true
                Log.d(TAG, "TextToSpeech initialized successfully.")
                pendingTextToSpeak?.let { text ->
                    val lang = pendingLanguage ?: AppLanguage.TELUGU
                    pendingTextToSpeak = null
                    pendingLanguage = null
                    speak(text, lang, onSpeakingFinishedListener)
                }
            } else {
                Log.e(TAG, "TextToSpeech initialization failed with status: $status")
            }
        }.apply {
            setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started utterance: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS finished utterance: $utteranceId")
                    onSpeakingFinishedListener?.invoke()
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error utterance: $utteranceId")
                    onSpeakingFinishedListener?.invoke()
                }
            })
        }
    }

    fun speak(text: String, language: AppLanguage, onComplete: (() -> Unit)? = null) {
        onSpeakingFinishedListener = onComplete
        if (!isInitialized || tts == null) {
            Log.w(TAG, "TTS not yet initialized. Queuing text.")
            pendingTextToSpeak = text
            pendingLanguage = language
            return
        }

        val locale = try {
            Locale.forLanguageTag(language.bcp47Locale)
        } catch (e: Exception) {
            Locale("te", "IN")
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.w(TAG, "Language ${language.bcp47Locale} not supported by TTS engine. Falling back to default locale.")
            tts?.setLanguage(Locale.getDefault())
        }

        val utteranceId = "gramagas_tts_${System.currentTimeMillis()}"
        val speakResult = tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
        if (speakResult == TextToSpeech.ERROR) {
            Log.e(TAG, "TTS speak failed for text.")
            onComplete?.invoke()
        }
    }

    fun stop() {
        try {
            tts?.stop()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping TTS", e)
        }
    }

    fun release() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
            Log.d(TAG, "TextToSpeech released.")
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down TTS", e)
        }
    }
}
