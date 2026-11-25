package com.example.ai

import android.util.Log
import com.example.model.AppLanguage
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.ai.Chat
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Content
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

private const val REPO_TAG = "GramaGasAI"

interface AiChatClient {
    val activeModelName: String
    val isGroundingActive: Boolean
    fun sendMessageStream(
        userMessage: String,
        language: AppLanguage,
        preferLiteModel: Boolean = false
    ): Flow<String>
    fun recordCompletedTurn(userMessage: String, assistantReply: String)
    fun rollbackFailedAttempt()
    suspend fun transcribeAudio(
        audioBytes: ByteArray,
        language: AppLanguage,
        mimeType: String = "audio/mp4"
    ): String
    suspend fun analyzeScreenFrame(
        imageBytes: ByteArray,
        prompt: String,
        language: AppLanguage,
        mimeType: String = "image/jpeg"
    ): String
    fun resetConversation()
}

class GramaGasAiRepository : AiChatClient {

    companion object {
        // Verified Firebase AI Logic free-tier-compatible models (no Blaze billing required)
        const val PRIMARY_CHAT_MODEL = "gemini-3.8-flash"
        const val FALLBACK_LITE_MODEL = "gemini-3.5-flash-lite"
        const val TRANSCRIBE_MODEL = "gemini-3.8-flash"

        private val SYSTEM_INSTRUCTIONS = """
            You are GramaGas AI, a multilingual voice-enabled LPG Gas Booking Assistant and customer support helper for rural customers in India.

            Supported languages (always respond strictly in the active selected language using its proper native script):
            Telugu, Hindi, English, Tamil, Kannada, Malayalam, Bengali, Marathi, Gujarati, Punjabi, Odia, Assamese, and Urdu.

            Your core responsibilities:
            1. LPG Gas Booking & Support Assistant:
               - Help customers book a new LPG cylinder (Indane Gas, Bharatgas, or HP Gas).
               - Check existing booking status and provide official reference numbers (e.g. Reference: GG-2026-7841, estimated delivery in 2-3 working days).
               - Report gas-related issues (such as cylinder leakage safety, safety checks, or delayed delivery).
            2. Customer Identification:
               - Always politely ask registered customers for their Gas Consumer Number (10-digit LPG ID) and registered mobile number before processing a booking or status check.
            3. Conversational Details Collection:
               - Collect required details step by step through natural conversation (ask only one question at a time).
            4. Booking Confirmation:
               - Confirm all booking details (provider, consumer number, delivery address) with the customer before final submission.
               - Display a clear confirmation message with a unique reference number upon successful booking confirmation.
            5. Security & Trust:
               - Never ask customers for OTPs, passwords, or UPI PINs. Warn customers not to share confidential information.

            Conversation rules:
            - Always respond strictly in the customer's currently selected language using its authentic native script (Telugu script for Telugu, Devanagari for Hindi, etc.).
            - Be friendly, respectful, and patient.
            - Keep answers clear, concise, and easy to hear/read on mobile screens.
            - Do not use markdown asterisks (**) or complex markdown headers (#); write clean plain text with clear line breaks.
        """.trimIndent()
    }

    @Volatile
    override var activeModelName: String = PRIMARY_CHAT_MODEL
        private set

    override val isGroundingActive: Boolean = false

    private val primaryChatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = PRIMARY_CHAT_MODEL,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 1536
            },
            systemInstruction = content {
                text(SYSTEM_INSTRUCTIONS)
            }
        )
    }

    private val fallbackLiteModel by lazy {
        Firebase.ai.generativeModel(
            modelName = FALLBACK_LITE_MODEL,
            generationConfig = generationConfig {
                temperature = 0.4f
                maxOutputTokens = 1536
            },
            systemInstruction = content {
                text(SYSTEM_INSTRUCTIONS)
            }
        )
    }

    private val transcriptionModel by lazy {
        Firebase.ai.generativeModel(
            modelName = TRANSCRIBE_MODEL,
            generationConfig = generationConfig {
                temperature = 0.0f
                maxOutputTokens = 512
            }
        )
    }

    private var activeChat: Chat? = null
    private var activeChatModelName: String = PRIMARY_CHAT_MODEL
    private var lastAppliedLanguage: AppLanguage? = null
    private val confirmedHistory = mutableListOf<Content>()

    private fun getOrCreateChat(useLiteModel: Boolean): Chat {
        val targetModelName = if (useLiteModel) FALLBACK_LITE_MODEL else PRIMARY_CHAT_MODEL
        val current = activeChat
        if (current != null && activeChatModelName == targetModelName) {
            return current
        }

        val model = if (useLiteModel) fallbackLiteModel else primaryChatModel
        val newChat = model.startChat(history = confirmedHistory.toList())
        activeChat = newChat
        activeChatModelName = targetModelName
        activeModelName = targetModelName
        return newChat
    }

    override fun sendMessageStream(
        userMessage: String,
        language: AppLanguage,
        preferLiteModel: Boolean
    ): Flow<String> = flow {
        val projectId = runCatching { FirebaseApp.getInstance().options.projectId }.getOrNull() ?: "uninitialized"
        val promptToSend = buildPromptWithLanguageDirective(userMessage, language)
        val targetModelName = if (preferLiteModel) FALLBACK_LITE_MODEL else PRIMARY_CHAT_MODEL
        activeModelName = targetModelName

        val startMs = System.currentTimeMillis()
        var firstChunkLogged = false
        runCatching {
            Log.d(
                REPO_TAG,
                "[chat] REQUEST project=$projectId model=$targetModelName grounding=false " +
                    "lang=${language.code} historyTurns=${confirmedHistory.size / 2}"
            )
        }

        val chat = getOrCreateChat(useLiteModel = preferLiteModel)
        chat.sendMessageStream(promptToSend).collect { response ->
            val textChunk = response.text
            if (!textChunk.isNullOrEmpty()) {
                if (!firstChunkLogged) {
                    firstChunkLogged = true
                    val ttftMs = System.currentTimeMillis() - startMs
                    runCatching {
                        Log.d(REPO_TAG, "[chat] FIRST_CHUNK model=$targetModelName ttftMs=$ttftMs")
                    }
                }
                emit(textChunk)
            }
        }

        val elapsedMs = System.currentTimeMillis() - startMs
        runCatching {
            Log.d(
                REPO_TAG,
                "[chat] SUCCESS model=$targetModelName elapsedMs=$elapsedMs"
            )
        }
    }.flowOn(Dispatchers.IO)

    private fun buildPromptWithLanguageDirective(userMessage: String, language: AppLanguage): String {
        return if (lastAppliedLanguage != language) {
            lastAppliedLanguage = language
            "[Active language: ${language.englishName} (${language.nativeName}) — ${language.scriptName} script. " +
                "Reply strictly in simple ${language.englishName} using ${language.scriptName} script while keeping our conversation context.]\n\n$userMessage"
        } else {
            "[Reply in simple ${language.englishName} using ${language.scriptName} script.]\n\n$userMessage"
        }
    }

    override fun recordCompletedTurn(userMessage: String, assistantReply: String) {
        confirmedHistory.add(content(role = "user") { text(userMessage) })
        confirmedHistory.add(content(role = "model") { text(assistantReply) })
    }

    override fun rollbackFailedAttempt() {
        activeChat = null
        lastAppliedLanguage = null
    }

    override suspend fun transcribeAudio(
        audioBytes: ByteArray,
        language: AppLanguage,
        mimeType: String
    ): String {
        val startMs = System.currentTimeMillis()
        runCatching {
            Log.d(
                REPO_TAG,
                "[transcribe] REQUEST model=$TRANSCRIBE_MODEL bytes=${audioBytes.size} lang=${language.code}"
            )
        }
        val response = transcriptionModel.generateContent(
            content {
                inlineData(audioBytes, mimeType)
                text(
                    "Listen carefully to this voice recording from an Indian LPG gas customer speaking in ${language.englishName} (${language.nativeName}). " +
                        "They may also mention LPG terms like Indane, Bharatgas, HP Gas, LPG, SMS, OTP, or delivery. " +
                        "Return ONLY the clean verbatim transcript in ${language.englishName} using ${language.scriptName} script. " +
                        "Do not answer the question, do not add explanations, quotes, or extra words. " +
                        "If no clear speech is audible, return an empty string."
                )
            }
        )
        val elapsedMs = System.currentTimeMillis() - startMs
        val transcript = response.text?.trim().orEmpty()
        runCatching {
            Log.d(
                REPO_TAG,
                "[transcribe] SUCCESS model=$TRANSCRIBE_MODEL chars=${transcript.length} elapsedMs=$elapsedMs"
            )
        }
        return transcript
    }

    override suspend fun analyzeScreenFrame(
        imageBytes: ByteArray,
        prompt: String,
        language: AppLanguage,
        mimeType: String
    ): String {
        val startMs = System.currentTimeMillis()
        runCatching {
            Log.d(
                REPO_TAG,
                "[screen_analyze] REQUEST model=$PRIMARY_CHAT_MODEL bytes=${imageBytes.size} lang=${language.code}"
            )
        }
        val response = primaryChatModel.generateContent(
            content {
                inlineData(imageBytes, mimeType)
                text(
                    "[Active language: ${language.englishName} (${language.nativeName}) — ${language.scriptName} script]\n" +
                        "You are GramaGas AI Wishflow Screen Sharing Assistant. Analyze this screen capture from an Indian LPG customer (Indane, Bharatgas, or HP Gas). " +
                        "Identify current page, buttons, forms, input fields, or error messages. " +
                        "Provide clear, concise, step-by-step guidance and instructions in ${language.englishName} using ${language.scriptName} script. " +
                        "User prompt: $prompt"
                )
            }
        )
        val elapsedMs = System.currentTimeMillis() - startMs
        val analysis = response.text?.trim().orEmpty()
        runCatching {
            Log.d(
                REPO_TAG,
                "[screen_analyze] SUCCESS model=$PRIMARY_CHAT_MODEL chars=${analysis.length} elapsedMs=$elapsedMs"
            )
        }
        return analysis
    }

    override fun resetConversation() {
        activeChat = null
        lastAppliedLanguage = null
        confirmedHistory.clear()
    }
}
