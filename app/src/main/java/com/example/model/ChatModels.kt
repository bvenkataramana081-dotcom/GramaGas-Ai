package com.example.model

import com.example.ai.AiErrorType
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

const val INITIAL_WELCOME_MESSAGE =
    "నమస్కారం! GramaGas AI కి స్వాగతం.\nమీకు ఏ సహాయం కావాలి?"

enum class MessageSender {
    USER,
    ASSISTANT
}

data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val sender: MessageSender,
    val text: String,
    val timestampLabel: String = formatCurrentTimestamp(),
    val isStreaming: Boolean = false,
    val isError: Boolean = false,
    val errorType: AiErrorType? = null,
    val failedPrompt: String? = null
)

enum class QuickActionType(
    val testTag: String,
    val englishSubtitle: String
) {
    LPG_BOOKING_HELP(
        testTag = "quick_action_booking_help",
        englishSubtitle = "LPG Booking Help"
    ),
    UNDERSTAND_SMS(
        testTag = "quick_action_understand_sms",
        englishSubtitle = "Understand SMS"
    ),
    DELIVERY_INFO(
        testTag = "quick_action_delivery_info",
        englishSubtitle = "Delivery Information"
    ),
    SUSPICIOUS_MESSAGE(
        testTag = "quick_action_suspicious_message",
        englishSubtitle = "Suspicious Message"
    );

    fun label(strings: UiStrings): String = when (this) {
        LPG_BOOKING_HELP -> strings.quickBookingLabel
        UNDERSTAND_SMS -> strings.quickSmsLabel
        DELIVERY_INFO -> strings.quickDeliveryLabel
        SUSPICIOUS_MESSAGE -> strings.quickScamLabel
    }

    fun prompt(strings: UiStrings): String = when (this) {
        LPG_BOOKING_HELP -> strings.quickBookingPrompt
        UNDERSTAND_SMS -> strings.quickSmsPrompt
        DELIVERY_INFO -> strings.quickDeliveryPrompt
        SUSPICIOUS_MESSAGE -> strings.quickScamPrompt
    }
}

enum class LpgProvider(
    val displayName: String,
    val testTag: String
) {
    INDANE(
        displayName = "Indane Gas",
        testTag = "provider_chip_indane"
    ),
    BHARATGAS(
        displayName = "Bharatgas",
        testTag = "provider_chip_bharatgas"
    ),
    HP_GAS(
        displayName = "HP Gas",
        testTag = "provider_chip_hp"
    );

    fun prompt(strings: UiStrings): String = strings.providerPromptTemplate(displayName)
}

sealed interface VoiceInputState {
    data object Idle : VoiceInputState

    data class Listening(
        val partialText: String = "",
        val audioLevel: Float = 0f,
        val isFallbackRecorder: Boolean = false
    ) : VoiceInputState

    data object Transcribing : VoiceInputState

    data class Error(
        val message: String,
        val isPermissionError: Boolean = false
    ) : VoiceInputState
}

data class ChatUiState(
    val selectedLanguage: AppLanguage = AppLanguage.TELUGU,
    val messages: List<ChatMessage> = listOf(
        ChatMessage(
            id = "welcome_initial",
            sender = MessageSender.ASSISTANT,
            text = localizedStringsFor(AppLanguage.TELUGU).welcomeMessage
        )
    ),
    val inputText: String = "",
    val isResponding: Boolean = false,
    val isWaitingForFirstChunk: Boolean = false,
    val retryingStatusMessage: String? = null,
    val isExtraLargeText: Boolean = false,
    val showClearDialog: Boolean = false,
    val voiceState: VoiceInputState = VoiceInputState.Idle,
    val showVoiceTranscribedHint: Boolean = false,
    val languageNoticeMessage: String? = null,
    val speakingMessageId: String? = null,
    val isVoiceAssistantEnabled: Boolean = false,
    val isScreenSharingActive: Boolean = false,
    val screenAssistantGuidance: String? = null,
    val isAnalyzingScreen: Boolean = false,
    val showBookingHistoryDialog: Boolean = false,
    val showCustomerProfileDialog: Boolean = false,
    val showNotificationsDialog: Boolean = false,
    val showPrivacyDialog: Boolean = false,
    val showGeneralSettingsDialog: Boolean = false,
    val showHelpSupportDialog: Boolean = false,
    val showGasBookingDialog: Boolean = false,
    val showLogoutDialog: Boolean = false
) {
    val strings: UiStrings
        get() = localizedStringsFor(selectedLanguage)
}

fun formatCurrentTimestamp(): String {
    val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault())
    return formatter.format(Date())
}
