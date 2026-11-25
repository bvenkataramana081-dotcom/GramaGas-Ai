package com.example

import com.example.ai.AiChatClient
import com.example.ai.AiErrorType
import com.example.ai.GramaGasAiRepository
import com.example.ai.classifyAiError
import com.example.model.AppLanguage
import com.example.model.MessageSender
import com.example.viewmodel.ChatViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GramaGasAiErrorHandlingTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun configuredModelsAreSupportedFreeTierFirebaseAiLogicModels() {
        assertEquals("gemini-3.8-flash", GramaGasAiRepository.PRIMARY_CHAT_MODEL)
        assertEquals("gemini-3.5-flash-lite", GramaGasAiRepository.FALLBACK_LITE_MODEL)
        assertEquals("gemini-3.8-flash", GramaGasAiRepository.TRANSCRIBE_MODEL)
    }

    @Test
    fun errorClassifierIdentifiesHttpStatusesAndRetryability() {
        val rateLimit = classifyAiError(RuntimeException("HTTP 429 RESOURCE_EXHAUSTED: Quota exceeded"))
        assertEquals(AiErrorType.HIGH_DEMAND_OR_RATE_LIMIT, rateLimit.type)
        assertEquals(429, rateLimit.httpStatusCode)
        assertTrue(rateLimit.isRetryable)

        val notFound = classifyAiError(RuntimeException("HTTP 404 NOT_FOUND: models/gemini-4-hydrogen is not found"))
        assertEquals(AiErrorType.SERVICE_UNAVAILABLE, notFound.type)
        assertEquals(404, notFound.httpStatusCode)
        assertFalse(notFound.isRetryable)

        val appCheck403 = classifyAiError(RuntimeException("HTTP 403 PERMISSION_DENIED: App Check token invalid"))
        assertEquals(AiErrorType.SERVICE_UNAVAILABLE, appCheck403.type)
        assertEquals(403, appCheck403.httpStatusCode)
        assertFalse(appCheck403.isRetryable)

        val networkErr = classifyAiError(IOException("Unable to resolve host"))
        assertEquals(AiErrorType.NETWORK, networkErr.type)
        assertTrue(networkErr.isRetryable)
    }

    @Test
    fun viewModelRetriesTemporary429WithLiteFallbackAndPreservesUserMessage() = runTest {
        var callCount = 0
        var usedLiteFallback = false

        val fakeClient = object : AiChatClient {
            override val activeModelName: String get() = "gemini-3.8-flash"
            override val isGroundingActive: Boolean get() = false

            override fun sendMessageStream(
                userMessage: String,
                language: AppLanguage,
                preferLiteModel: Boolean
            ): Flow<String> = flow {
                callCount++
                if (preferLiteModel) usedLiteFallback = true
                if (callCount == 1) {
                    throw RuntimeException("HTTP 429 RESOURCE_EXHAUSTED")
                }
                emit("మీ ఇండేన్ గ్యాస్ బుకింగ్ కోసం 7718955555 కు కాల్ చేయండి.")
            }

            override fun recordCompletedTurn(userMessage: String, assistantReply: String) = Unit
            override fun rollbackFailedAttempt() = Unit
            override suspend fun transcribeAudio(
                audioBytes: ByteArray,
                language: AppLanguage,
                mimeType: String
            ): String = ""
            override suspend fun analyzeScreenFrame(
                imageBytes: ByteArray,
                prompt: String,
                language: AppLanguage,
                mimeType: String
            ): String = ""
            override fun resetConversation() = Unit
        }

        val viewModel = ChatViewModel(
            aiRepository = fakeClient,
            retryBackoffProvider = { 0L }
        )
        viewModel.onInputTextChanged("ఇండేన్ గ్యాస్ ఎలా బుక్ చేయాలి?")
        viewModel.sendCurrentInput()
        advanceUntilIdle()

        assertEquals(2, callCount)
        assertTrue(usedLiteFallback)
        val messages = viewModel.uiState.value.messages
        assertEquals(3, messages.size) // Welcome + User + Assistant reply
        assertEquals(MessageSender.USER, messages[1].sender)
        assertEquals("ఇండేన్ గ్యాస్ ఎలా బుక్ చేయాలి?", messages[1].text)
        assertFalse(messages[2].isError)
    }

    @Test
    fun viewModelStopsAfterMaxRetriesAndNeverExposesRawApiError() = runTest {
        var callCount = 0
        val rawSecretError = "HTTP 503 UNAVAILABLE: internal backend stacktrace xyz123"

        val failingClient = object : AiChatClient {
            override val activeModelName: String get() = "gemini-3.8-flash"
            override val isGroundingActive: Boolean get() = false

            override fun sendMessageStream(
                userMessage: String,
                language: AppLanguage,
                preferLiteModel: Boolean
            ): Flow<String> = flow {
                callCount++
                throw RuntimeException(rawSecretError)
            }

            override fun recordCompletedTurn(userMessage: String, assistantReply: String) = Unit
            override fun rollbackFailedAttempt() = Unit
            override suspend fun transcribeAudio(
                audioBytes: ByteArray,
                language: AppLanguage,
                mimeType: String
            ): String = ""
            override suspend fun analyzeScreenFrame(
                imageBytes: ByteArray,
                prompt: String,
                language: AppLanguage,
                mimeType: String
            ): String = ""
            override fun resetConversation() = Unit
        }

        val viewModel = ChatViewModel(
            aiRepository = failingClient,
            retryBackoffProvider = { 0L }
        )
        viewModel.onInputTextChanged("HP Gas booking help")
        viewModel.sendCurrentInput()
        advanceUntilIdle()

        // 1 initial attempt + 2 automatic retries = 3 total attempts (bounded, never infinite)
        assertEquals(3, callCount)
        val messages = viewModel.uiState.value.messages
        assertEquals(3, messages.size)
        assertEquals("HP Gas booking help", messages[1].text)
        assertTrue(messages[2].isError)
        assertEquals("HP Gas booking help", messages[2].failedPrompt)
        assertFalse(messages[2].text.contains("503"))
        assertFalse(messages[2].text.contains("stacktrace"))
    }

    @Test
    fun viewModelSwitchesToLiteModelOnTimeoutAndRecovers() = runTest {
        var callCount = 0
        var usedLiteOnRetry = false

        val slowThenFastClient = object : AiChatClient {
            override val activeModelName: String get() = "gemini-3.8-flash"
            override val isGroundingActive: Boolean get() = false

            override fun sendMessageStream(
                userMessage: String,
                language: AppLanguage,
                preferLiteModel: Boolean
            ): Flow<String> = flow {
                callCount++
                if (callCount == 1) {
                    throw java.net.SocketTimeoutException("Timed out waiting for model")
                }
                usedLiteOnRetry = preferLiteModel
                emit("భారత్ గ్యాస్ బుకింగ్ కోసం మీ రిజిస్టర్డ్ నంబర్ నుండి 7715012345 కు కాల్ చేయండి.")
            }

            override fun recordCompletedTurn(userMessage: String, assistantReply: String) = Unit
            override fun rollbackFailedAttempt() = Unit
            override suspend fun transcribeAudio(
                audioBytes: ByteArray,
                language: AppLanguage,
                mimeType: String
            ): String = ""
            override suspend fun analyzeScreenFrame(
                imageBytes: ByteArray,
                prompt: String,
                language: AppLanguage,
                mimeType: String
            ): String = ""
            override fun resetConversation() = Unit
        }

        val viewModel = ChatViewModel(
            aiRepository = slowThenFastClient,
            retryBackoffProvider = { 0L }
        )
        viewModel.onInputTextChanged("Bharatgas booking")
        viewModel.sendCurrentInput()
        advanceUntilIdle()

        assertEquals(2, callCount)
        assertTrue(usedLiteOnRetry)
        val messages = viewModel.uiState.value.messages
        assertEquals(3, messages.size)
        assertFalse(messages[2].isError)
    }
}
