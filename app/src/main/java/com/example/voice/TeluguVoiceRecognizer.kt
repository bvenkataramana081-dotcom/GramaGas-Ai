package com.example.voice

import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.example.model.AppLanguage
import com.example.model.UiStrings
import com.example.model.localizedStringsFor
import java.io.File
import kotlin.math.min

private const val TAG = "TeluguVoiceRecognizer"
private const val AMPLITUDE_POLL_MS = 100L
private const val MAX_FALLBACK_RECORDING_MS = 30_000L

interface TeluguVoiceCallback {
    fun onListeningStarted(isFallbackRecorder: Boolean)
    fun onPartialResult(partialText: String)
    fun onAudioLevelChanged(normalizedLevel: Float)
    fun onFinalResult(recognizedText: String)
    fun onFallbackAudioCaptured(audioBytes: ByteArray)
    fun onError(localizedMessage: String)
    fun onCancelled()
}

/**
 * Manages multilingual voice-to-text across all 13 supported languages using Android's
 * native [SpeechRecognizer] (configured with the active language's BCP-47 locale) and
 * automatically falls back to [MediaRecorder] audio capture + Gemini transcription on
 * devices without a system SpeechRecognitionService.
 */
class TeluguVoiceRecognizer(
    private val context: Context,
    private val callback: TeluguVoiceCallback
) {
    private val mainHandler = Handler(Looper.getMainLooper())

    private var speechRecognizer: SpeechRecognizer? = null
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null

    private var isActive = false
    private var isUsingFallbackRecorder = false
    private var latestPartialText = ""
    private var activeLanguage: AppLanguage = AppLanguage.TELUGU
    private var activeStrings: UiStrings = localizedStringsFor(AppLanguage.TELUGU)

    private val amplitudePollRunnable = object : Runnable {
        override fun run() {
            val recorder = mediaRecorder ?: return
            if (!isActive || !isUsingFallbackRecorder) return
            try {
                val amp = recorder.maxAmplitude
                val normalized = min(1f, amp / 16000f)
                callback.onAudioLevelChanged(normalized)
            } catch (e: Exception) {
                Log.w(TAG, "Unable to read recorder amplitude", e)
            }
            mainHandler.postDelayed(this, AMPLITUDE_POLL_MS)
        }
    }

    private val autoStopFallbackRunnable = Runnable {
        if (isActive && isUsingFallbackRecorder) {
            stopListening()
        }
    }

    fun startListening(language: AppLanguage = AppLanguage.TELUGU) {
        mainHandler.post {
            if (isActive) {
                stopListening()
                return@post
            }
            activeLanguage = language
            activeStrings = localizedStringsFor(language)
            latestPartialText = ""

            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                startNativeSpeechRecognizer(language)
            } else {
                Log.i(TAG, "System SpeechRecognizer unavailable; using MediaRecorder fallback for ${language.bcp47Locale}.")
                startFallbackMediaRecorder()
            }
        }
    }

    fun stopListening() {
        mainHandler.post {
            if (!isActive) return@post
            if (isUsingFallbackRecorder) {
                stopFallbackMediaRecorder(deliverAudio = true)
            } else {
                try {
                    speechRecognizer?.stopListening()
                } catch (e: Exception) {
                    Log.e(TAG, "Error stopping SpeechRecognizer", e)
                    cleanupNativeRecognizer()
                    isActive = false
                    if (latestPartialText.isNotBlank()) {
                        callback.onFinalResult(latestPartialText)
                    } else {
                        callback.onCancelled()
                    }
                }
            }
        }
    }

    fun cancelListening() {
        mainHandler.post {
            if (!isActive) return@post
            if (isUsingFallbackRecorder) {
                stopFallbackMediaRecorder(deliverAudio = false)
            } else {
                cleanupNativeRecognizer()
            }
            isActive = false
            isUsingFallbackRecorder = false
            latestPartialText = ""
            callback.onCancelled()
        }
    }

    fun release() {
        mainHandler.post {
            mainHandler.removeCallbacks(amplitudePollRunnable)
            mainHandler.removeCallbacks(autoStopFallbackRunnable)
            cleanupNativeRecognizer()
            releaseFallbackRecorder(deleteFile = true)
            isActive = false
            isUsingFallbackRecorder = false
        }
    }

    private fun startNativeSpeechRecognizer(language: AppLanguage) {
        cleanupNativeRecognizer()
        val strings = activeStrings
        try {
            val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
            recognizer.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    Log.d(TAG, "Native SpeechRecognizer ready for ${language.bcp47Locale}.")
                }

                override fun onBeginningOfSpeech() = Unit

                override fun onRmsChanged(rmsdB: Float) {
                    val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
                    callback.onAudioLevelChanged(normalized)
                }

                override fun onBufferReceived(buffer: ByteArray?) = Unit

                override fun onEndOfSpeech() = Unit

                override fun onError(error: Int) {
                    Log.w(TAG, "SpeechRecognizer error code=$error")
                    val partialSnapshot = latestPartialText
                    cleanupNativeRecognizer()
                    isActive = false

                    if (error == SpeechRecognizer.ERROR_CLIENT ||
                        error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY ||
                        error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS
                    ) {
                        if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                            callback.onError(strings.voicePermissionDenied)
                        } else if (partialSnapshot.isNotBlank()) {
                            callback.onFinalResult(partialSnapshot)
                        } else {
                            startFallbackMediaRecorder()
                        }
                        return
                    }

                    if (partialSnapshot.isNotBlank() &&
                        (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)
                    ) {
                        callback.onFinalResult(partialSnapshot)
                        return
                    }

                    val errorText = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> strings.voiceAudioError
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> strings.voiceNetworkError
                        SpeechRecognizer.ERROR_NO_MATCH,
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> strings.voiceNoSpeechDetected
                        else -> strings.voiceGenericError
                    }
                    callback.onError(errorText)
                }

                override fun onResults(results: Bundle?) {
                    val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val recognized = matches?.firstOrNull()?.trim().orEmpty()
                        .ifEmpty { latestPartialText.trim() }
                    cleanupNativeRecognizer()
                    isActive = false
                    if (recognized.isNotEmpty()) {
                        callback.onFinalResult(recognized)
                    } else {
                        callback.onError(strings.voiceNoSpeechDetected)
                    }
                }

                override fun onPartialResults(partialResults: Bundle?) {
                    val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    val partial = matches?.firstOrNull()?.trim().orEmpty()
                    if (partial.isNotEmpty()) {
                        latestPartialText = partial
                        callback.onPartialResult(partial)
                    }
                }

                override fun onEvent(eventType: Int, params: Bundle?) = Unit
            })

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.bcp47Locale)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language.bcp47Locale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2200L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1600L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }

            speechRecognizer = recognizer
            isActive = true
            isUsingFallbackRecorder = false
            callback.onListeningStarted(isFallbackRecorder = false)
            recognizer.startListening(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start native SpeechRecognizer, switching to fallback recorder", e)
            cleanupNativeRecognizer()
            startFallbackMediaRecorder()
        }
    }

    private fun startFallbackMediaRecorder() {
        releaseFallbackRecorder(deleteFile = true)
        try {
            val outFile = File(context.cacheDir, "gramagas_voice_${System.currentTimeMillis()}.m4a")
            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            recorder.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(16000)
                setAudioEncodingBitRate(64000)
                setOutputFile(outFile.absolutePath)
                prepare()
                start()
            }
            recordingFile = outFile
            mediaRecorder = recorder
            isActive = true
            isUsingFallbackRecorder = true
            callback.onListeningStarted(isFallbackRecorder = true)
            mainHandler.postDelayed(amplitudePollRunnable, AMPLITUDE_POLL_MS)
            mainHandler.postDelayed(autoStopFallbackRunnable, MAX_FALLBACK_RECORDING_MS)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start fallback MediaRecorder", e)
            releaseFallbackRecorder(deleteFile = true)
            isActive = false
            isUsingFallbackRecorder = false
            callback.onError(activeStrings.voiceMicStartError)
        }
    }

    private fun stopFallbackMediaRecorder(deliverAudio: Boolean) {
        mainHandler.removeCallbacks(amplitudePollRunnable)
        mainHandler.removeCallbacks(autoStopFallbackRunnable)
        val file = recordingFile
        try {
            mediaRecorder?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "MediaRecorder stop threw (recording may have been too short)", e)
        }
        releaseFallbackRecorder(deleteFile = !deliverAudio)
        isActive = false
        isUsingFallbackRecorder = false

        if (deliverAudio) {
            val bytes = try {
                if (file != null && file.exists() && file.length() > 0L) {
                    file.readBytes()
                } else {
                    ByteArray(0)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed reading recorded audio file", e)
                ByteArray(0)
            } finally {
                file?.delete()
                recordingFile = null
            }

            if (bytes.size > 1024) {
                callback.onFallbackAudioCaptured(bytes)
            } else {
                callback.onError(activeStrings.voiceNoSpeechDetected)
            }
        }
    }

    private fun cleanupNativeRecognizer() {
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.w(TAG, "Error destroying SpeechRecognizer", e)
        }
        speechRecognizer = null
    }

    private fun releaseFallbackRecorder(deleteFile: Boolean) {
        try {
            mediaRecorder?.reset()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing MediaRecorder", e)
        }
        mediaRecorder = null
        if (deleteFile) {
            recordingFile?.delete()
            recordingFile = null
        }
    }
}
