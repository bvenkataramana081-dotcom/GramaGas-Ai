package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ai.AiChatClient
import com.example.ai.AiErrorType
import com.example.ai.GramaGasAiRepository
import com.example.ai.classifyAiError
import com.example.ai.logAiDiagnosticFailure
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.ChatUiState
import com.example.model.LanguagePreferenceStore
import com.example.model.LpgProvider
import com.example.model.MessageSender
import com.example.model.QuickActionType
import com.example.model.UiStrings
import com.example.model.VoiceInputState
import com.example.model.formatCurrentTimestamp
import com.example.model.localizedStringsFor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.UUID
import kotlin.random.Random

private const val TAG = "ChatViewModel"
private const val AI_TIMEOUT_MS = 45_000L
private const val VOICE_TRANSCRIBE_TIMEOUT_MS = 45_000L
private const val LANGUAGE_NOTICE_DURATION_MS = 3_000L
private const val MAX_AUTO_RETRIES = 2
private const val MAX_VOICE_AUTO_RETRIES = 1
private const val INITIAL_RETRY_BACKOFF_MS = 1_500L

class ChatViewModel(
    private val languageStore: LanguagePreferenceStore? = null,
    private val aiRepository: AiChatClient = GramaGasAiRepository(),
    private val retryBackoffProvider: (Int) -> Long = { retryAttempt ->
        val exponentialBase = INITIAL_RETRY_BACKOFF_MS * (1L shl (retryAttempt - 1))
        val jitterMs = Random.nextLong(from = 100L, until = 400L)
        exponentialBase + jitterMs
    }
) : ViewModel() {

    private val initialLanguage = languageStore?.loadLanguage() ?: AppLanguage.TELUGU

    private val _uiState = MutableStateFlow(
        ChatUiState(
            selectedLanguage = initialLanguage,
            messages = listOf(
                ChatMessage(
                    id = "welcome_initial",
                    sender = MessageSender.ASSISTANT,
                    text = localizedStringsFor(initialLanguage).welcomeMessage,
                    timestampLabel = formatCurrentTimestamp()
                )
            )
        )
    )
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var activeResponseJob: Job? = null
    private var activeTranscriptionJob: Job? = null
    private var noticeDismissJob: Job? = null

    fun onLanguageSelected(language: AppLanguage) {
        if (_uiState.value.selectedLanguage == language) return
        languageStore?.saveLanguage(language)
        val newStrings = localizedStringsFor(language)

        _uiState.update { state ->
            // Preserve all existing chat history. If only the untouched initial welcome greeting
            // is present, update that greeting to the newly selected language for a smooth start.
            val updatedMessages = if (
                state.messages.size == 1 &&
                state.messages.first().id.startsWith("welcome_")
            ) {
                listOf(
                    state.messages.first().copy(text = newStrings.welcomeMessage)
                )
            } else {
                state.messages
            }

            state.copy(
                selectedLanguage = language,
                messages = updatedMessages,
                languageNoticeMessage = newStrings.languageChangedNotice,
                voiceState = if (state.voiceState is VoiceInputState.Error) {
                    VoiceInputState.Idle
                } else {
                    state.voiceState
                }
            )
        }

        noticeDismissJob?.cancel()
        noticeDismissJob = viewModelScope.launch {
            delay(LANGUAGE_NOTICE_DURATION_MS)
            _uiState.update { it.copy(languageNoticeMessage = null) }
        }
    }

    fun dismissLanguageNotice() {
        noticeDismissJob?.cancel()
        _uiState.update { it.copy(languageNoticeMessage = null) }
    }

    fun onInputTextChanged(newValue: String) {
        _uiState.update {
            it.copy(
                inputText = newValue,
                showVoiceTranscribedHint = if (newValue.isBlank()) false else it.showVoiceTranscribedHint
            )
        }
    }

    fun sendCurrentInput() {
        val trimmed = _uiState.value.inputText.trim()
        if (trimmed.isEmpty() || _uiState.value.isResponding) return
        _uiState.update {
            it.copy(
                inputText = "",
                showVoiceTranscribedHint = false,
                voiceState = VoiceInputState.Idle
            )
        }
        sendPrompt(trimmed)
    }

    fun onQuickActionSelected(action: QuickActionType) {
        if (_uiState.value.isResponding) return
        val strings = _uiState.value.strings
        _uiState.update { it.copy(showVoiceTranscribedHint = false) }
        sendPrompt(action.prompt(strings))
    }

    fun onProviderSelected(provider: LpgProvider) {
        if (_uiState.value.isResponding) return
        val strings = _uiState.value.strings
        _uiState.update { it.copy(showVoiceTranscribedHint = false) }
        sendPrompt(provider.prompt(strings))
    }

    fun onVoiceListeningStarted(isFallbackRecorder: Boolean) {
        activeTranscriptionJob?.cancel()
        _uiState.update {
            it.copy(
                voiceState = VoiceInputState.Listening(
                    partialText = "",
                    audioLevel = 0f,
                    isFallbackRecorder = isFallbackRecorder
                ),
                showVoiceTranscribedHint = false
            )
        }
    }

    fun onVoicePartialResult(partialText: String) {
        _uiState.update { state ->
            val currentVoice = state.voiceState
            if (currentVoice is VoiceInputState.Listening) {
                state.copy(voiceState = currentVoice.copy(partialText = partialText))
            } else {
                state
            }
        }
    }

    fun onVoiceAudioLevelChanged(normalizedLevel: Float) {
        _uiState.update { state ->
            val currentVoice = state.voiceState
            if (currentVoice is VoiceInputState.Listening) {
                state.copy(voiceState = currentVoice.copy(audioLevel = normalizedLevel))
            } else {
                state
            }
        }
    }

    fun onVoiceFinalResult(recognizedText: String) {
        val cleaned = recognizedText.trim()
        val strings = _uiState.value.strings
        if (cleaned.isEmpty()) {
            _uiState.update {
                it.copy(
                    voiceState = VoiceInputState.Error(
                        message = strings.voiceNoSpeechDetected,
                        isPermissionError = false
                    )
                )
            }
            return
        }
        _uiState.update { state ->
            val existing = state.inputText.trim()
            val combined = if (existing.isEmpty()) cleaned else "$existing $cleaned"
            state.copy(
                inputText = combined,
                voiceState = VoiceInputState.Idle,
                showVoiceTranscribedHint = true
            )
        }
    }

    fun onVoiceFallbackAudioCaptured(audioBytes: ByteArray) {
        activeTranscriptionJob?.cancel()
        val currentLanguage = _uiState.value.selectedLanguage
        val strings = localizedStringsFor(currentLanguage)
        activeTranscriptionJob = viewModelScope.launch {
            _uiState.update { it.copy(voiceState = VoiceInputState.Transcribing) }
            var attempt = 0
            while (true) {
                try {
                    val transcript = withTimeout(VOICE_TRANSCRIBE_TIMEOUT_MS) {
                        aiRepository.transcribeAudio(audioBytes, currentLanguage)
                    }
                    if (transcript.isNotBlank()) {
                        onVoiceFinalResult(transcript)
                    } else {
                        _uiState.update {
                            it.copy(
                                voiceState = VoiceInputState.Error(
                                    message = strings.voiceNoSpeechDetected
                                )
                            )
                        }
                    }
                    return@launch
                } catch (e: CancellationException) {
                    if (e !is TimeoutCancellationException) throw e
                    Log.e(TAG, "Voice transcription timed out (attempt ${attempt + 1})", e)
                    if (attempt < MAX_VOICE_AUTO_RETRIES) {
                        attempt++
                        delay(retryBackoffProvider(attempt))
                        continue
                    }
                    _uiState.update {
                        it.copy(
                            voiceState = VoiceInputState.Error(
                                message = strings.voiceTimeoutError
                            )
                        )
                    }
                    return@launch
                } catch (e: Exception) {
                    val classification = classifyAiError(e)
                    logAiDiagnosticFailure(
                        operation = "transcribe",
                        modelName = GramaGasAiRepository.TRANSCRIBE_MODEL,
                        groundingEnabled = false,
                        attempt = attempt + 1,
                        classification = classification,
                        throwable = e
                    )
                    if (classification.isRetryable && attempt < MAX_VOICE_AUTO_RETRIES) {
                        attempt++
                        delay(retryBackoffProvider(attempt))
                        continue
                    }
                    val friendlyVoiceError = when (classification.type) {
                        AiErrorType.TIMEOUT -> strings.voiceTimeoutError
                        AiErrorType.NETWORK -> strings.voiceNetworkError
                        AiErrorType.HIGH_DEMAND_OR_RATE_LIMIT -> strings.highDemandError
                        else -> strings.voiceGenericError
                    }
                    _uiState.update {
                        it.copy(
                            voiceState = VoiceInputState.Error(
                                message = friendlyVoiceError
                            )
                        )
                    }
                    return@launch
                }
            }
        }
    }

    fun onVoicePermissionDenied() {
        val strings = _uiState.value.strings
        _uiState.update {
            it.copy(
                voiceState = VoiceInputState.Error(
                    message = strings.voicePermissionDenied,
                    isPermissionError = true
                )
            )
        }
    }

    fun onVoiceError(message: String) {
        _uiState.update {
            it.copy(
                voiceState = VoiceInputState.Error(
                    message = message,
                    isPermissionError = false
                )
            )
        }
    }

    fun onVoiceCancelled() {
        activeTranscriptionJob?.cancel()
        _uiState.update { it.copy(voiceState = VoiceInputState.Idle) }
    }

    fun dismissVoiceError() {
        _uiState.update { state ->
            if (state.voiceState is VoiceInputState.Error) {
                state.copy(voiceState = VoiceInputState.Idle)
            } else {
                state
            }
        }
    }

    fun retryMessage(errorMessageId: String, originalPrompt: String) {
        if (_uiState.value.isResponding) return
        _uiState.update { state ->
            state.copy(messages = state.messages.filterNot { it.id == errorMessageId })
        }
        requestAiResponse(originalPrompt)
    }

    fun toggleTextSize() {
        _uiState.update { it.copy(isExtraLargeText = !it.isExtraLargeText) }
    }

    fun toggleVoiceAssistant() {
        _uiState.update { it.copy(isVoiceAssistantEnabled = !it.isVoiceAssistantEnabled) }
    }

    fun startScreenSharing() {
        val strings = _uiState.value.strings
        val welcomeGuidance = strings.screenAssistantPrompt
        _uiState.update {
            it.copy(
                isScreenSharingActive = true,
                screenAssistantGuidance = welcomeGuidance
            )
        }
    }

    fun stopScreenSharing() {
        _uiState.update {
            it.copy(
                isScreenSharingActive = false,
                screenAssistantGuidance = null,
                isAnalyzingScreen = false
            )
        }
    }

    fun analyzeScreenFrame(imageBytes: ByteArray, customPrompt: String? = null) {
        val currentLang = _uiState.value.selectedLanguage
        val strings = _uiState.value.strings
        val prompt = customPrompt ?: strings.screenAssistantPrompt

        viewModelScope.launch {
            _uiState.update { it.copy(isAnalyzingScreen = true) }
            try {
                val guidance = aiRepository.analyzeScreenFrame(imageBytes, prompt, currentLang)
                if (guidance.isNotBlank()) {
                    _uiState.update {
                        it.copy(
                            screenAssistantGuidance = guidance,
                            isAnalyzingScreen = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isAnalyzingScreen = false) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Screen analysis failed", e)
                _uiState.update { it.copy(isAnalyzingScreen = false) }
            }
        }
    }

    fun requestClearChat() {
        _uiState.update { it.copy(showClearDialog = true) }
    }

    fun dismissClearDialog() {
        _uiState.update { it.copy(showClearDialog = false) }
    }

    fun confirmClearChat() {
        activeResponseJob?.cancel()
        activeResponseJob = null
        activeTranscriptionJob?.cancel()
        activeTranscriptionJob = null
        aiRepository.resetConversation()
        _uiState.update { state ->
            val strings = localizedStringsFor(state.selectedLanguage)
            ChatUiState(
                selectedLanguage = state.selectedLanguage,
                messages = listOf(
                    ChatMessage(
                        id = "welcome_${UUID.randomUUID()}",
                        sender = MessageSender.ASSISTANT,
                        text = strings.welcomeMessage,
                        timestampLabel = formatCurrentTimestamp()
                    )
                ),
                isExtraLargeText = state.isExtraLargeText,
                showClearDialog = false
            )
        }
    }

    private fun sendPrompt(prompt: String) {
        val userMessage = ChatMessage(
            sender = MessageSender.USER,
            text = prompt,
            timestampLabel = formatCurrentTimestamp()
        )
        _uiState.update { state ->
            state.copy(messages = state.messages + userMessage)
        }
        requestAiResponse(prompt)
    }

    private fun requestAiResponse(prompt: String) {
        activeResponseJob?.cancel()

        activeResponseJob = viewModelScope.launch {
            val assistantMsgId = UUID.randomUUID().toString()
            _uiState.update { state ->
                state.copy(
                    isResponding = true,
                    isWaitingForFirstChunk = true,
                    retryingStatusMessage = null
                )
            }

            var attempt = 0
            var useFallbackLiteModel = false
            while (true) {
                val activeLanguage = _uiState.value.selectedLanguage
                val strings = localizedStringsFor(activeLanguage)
                val accumulated = StringBuilder()
                var hasInsertedAssistantBubble = false

                try {
                    withTimeout(AI_TIMEOUT_MS) {
                        aiRepository.sendMessageStream(
                            userMessage = prompt,
                            language = activeLanguage,
                            preferLiteModel = useFallbackLiteModel
                        ).collect { chunk ->
                            if (chunk.isEmpty()) return@collect
                            accumulated.append(chunk)
                            val cleanText = cleanMarkdownFormatting(accumulated.toString())

                            if (!hasInsertedAssistantBubble) {
                                hasInsertedAssistantBubble = true
                                val streamingBubble = ChatMessage(
                                    id = assistantMsgId,
                                    sender = MessageSender.ASSISTANT,
                                    text = cleanText,
                                    timestampLabel = formatCurrentTimestamp(),
                                    isStreaming = true
                                )
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages + streamingBubble,
                                        isWaitingForFirstChunk = false,
                                        retryingStatusMessage = null
                                    )
                                }
                            } else {
                                _uiState.update { state ->
                                    state.copy(
                                        messages = state.messages.map { msg ->
                                            if (msg.id == assistantMsgId) {
                                                msg.copy(text = cleanText, isStreaming = true)
                                            } else {
                                                msg
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }

                    val finalResponse = cleanMarkdownFormatting(accumulated.toString()).trim()
                    if (finalResponse.isNotEmpty()) {
                        aiRepository.recordCompletedTurn(prompt, finalResponse)
                        _uiState.update { state ->
                            val updatedMessages = if (hasInsertedAssistantBubble) {
                                state.messages.map { msg ->
                                    if (msg.id == assistantMsgId) {
                                        msg.copy(
                                            text = finalResponse,
                                            timestampLabel = formatCurrentTimestamp(),
                                            isStreaming = false,
                                            isError = false,
                                            errorType = null,
                                            failedPrompt = null
                                        )
                                    } else {
                                        msg
                                    }
                                }
                            } else {
                                state.messages + ChatMessage(
                                    id = assistantMsgId,
                                    sender = MessageSender.ASSISTANT,
                                    text = finalResponse,
                                    timestampLabel = formatCurrentTimestamp(),
                                    isStreaming = false
                                )
                            }
                            state.copy(
                                messages = updatedMessages,
                                isResponding = false,
                                isWaitingForFirstChunk = false,
                                retryingStatusMessage = null
                            )
                        }
                        return@launch
                    } else {
                        aiRepository.rollbackFailedAttempt()
                        if (attempt < MAX_AUTO_RETRIES) {
                            attempt++
                            prepareForAutomaticRetry(assistantMsgId, attempt, strings)
                            delay(retryBackoffProvider(attempt))
                            continue
                        }
                        appendErrorBubble(
                            assistantMsgId = assistantMsgId,
                            hasInsertedBubble = hasInsertedAssistantBubble,
                            errorType = AiErrorType.EMPTY_RESPONSE,
                            strings = strings,
                            failedPrompt = prompt
                        )
                        return@launch
                    }
                } catch (e: CancellationException) {
                    if (e !is TimeoutCancellationException) {
                        throw e
                    }
                    val classification = classifyAiError(e)
                    logAiDiagnosticFailure(
                        operation = "chat",
                        modelName = aiRepository.activeModelName,
                        groundingEnabled = aiRepository.isGroundingActive,
                        attempt = attempt + 1,
                        classification = classification,
                        throwable = e
                    )
                    aiRepository.rollbackFailedAttempt()
                    if (attempt < MAX_AUTO_RETRIES) {
                        attempt++
                        // Switch to ultra-fast Flash-Lite model after a timeout for rapid response
                        useFallbackLiteModel = true
                        prepareForAutomaticRetry(assistantMsgId, attempt, strings)
                        delay(retryBackoffProvider(attempt))
                        continue
                    }
                    appendErrorBubble(
                        assistantMsgId = assistantMsgId,
                        hasInsertedBubble = hasInsertedAssistantBubble,
                        errorType = AiErrorType.TIMEOUT,
                        strings = _uiState.value.strings,
                        failedPrompt = prompt
                    )
                    return@launch
                } catch (e: Exception) {
                    val classification = classifyAiError(e)
                    logAiDiagnosticFailure(
                        operation = "chat",
                        modelName = aiRepository.activeModelName,
                        groundingEnabled = aiRepository.isGroundingActive,
                        attempt = attempt + 1,
                        classification = classification,
                        throwable = e
                    )
                    aiRepository.rollbackFailedAttempt()
                    if (classification.isRetryable && attempt < MAX_AUTO_RETRIES) {
                        attempt++
                        // On rate-limit, high-demand, or timeout errors, switch to verified high-quota Flash-Lite model
                        if (classification.type == AiErrorType.HIGH_DEMAND_OR_RATE_LIMIT ||
                            classification.type == AiErrorType.TIMEOUT
                        ) {
                            useFallbackLiteModel = true
                        }
                        prepareForAutomaticRetry(assistantMsgId, attempt, strings)
                        delay(retryBackoffProvider(attempt))
                        continue
                    }
                    appendErrorBubble(
                        assistantMsgId = assistantMsgId,
                        hasInsertedBubble = hasInsertedAssistantBubble,
                        errorType = classification.type,
                        strings = _uiState.value.strings,
                        failedPrompt = prompt
                    )
                    return@launch
                }
            }
        }
    }

    private fun prepareForAutomaticRetry(
        assistantMsgId: String,
        retryAttempt: Int,
        strings: UiStrings
    ) {
        _uiState.update { state ->
            state.copy(
                messages = state.messages.filterNot { it.id == assistantMsgId },
                isResponding = true,
                isWaitingForFirstChunk = true,
                retryingStatusMessage = "${strings.retryingStatus} ($retryAttempt/$MAX_AUTO_RETRIES)"
            )
        }
    }

    private fun appendErrorBubble(
        assistantMsgId: String,
        hasInsertedBubble: Boolean,
        errorType: AiErrorType,
        strings: UiStrings,
        failedPrompt: String
    ) {
        val friendlyMessage = friendlyMessageForErrorType(errorType, strings)
        _uiState.update { state ->
            val errorBubble = ChatMessage(
                id = assistantMsgId,
                sender = MessageSender.ASSISTANT,
                text = friendlyMessage,
                timestampLabel = formatCurrentTimestamp(),
                isStreaming = false,
                isError = true,
                errorType = errorType,
                failedPrompt = failedPrompt
            )
            val updatedMessages = if (hasInsertedBubble) {
                state.messages.map { msg ->
                    if (msg.id == assistantMsgId) errorBubble else msg
                }
            } else {
                state.messages + errorBubble
            }
            state.copy(
                messages = updatedMessages,
                isResponding = false,
                isWaitingForFirstChunk = false,
                retryingStatusMessage = null
            )
        }
    }

    private fun cleanMarkdownFormatting(raw: String): String {
        return raw
            .replace("**", "")
            .replace("### ", "")
            .replace("## ", "")
            .replace("# ", "")
    }

    class Factory(
        private val languageStore: LanguagePreferenceStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(languageStore = languageStore) as T
        }
    }
}

fun friendlyMessageForErrorType(errorType: AiErrorType, strings: UiStrings): String {
    return when (errorType) {
        AiErrorType.HIGH_DEMAND_OR_RATE_LIMIT -> strings.highDemandError
        AiErrorType.TIMEOUT -> strings.timeoutError
        AiErrorType.NETWORK -> strings.networkError
        AiErrorType.SERVICE_UNAVAILABLE -> strings.serviceUnavailableError
        AiErrorType.EMPTY_RESPONSE -> strings.emptyResponseError
    }
}
