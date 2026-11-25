package com.example.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.R
import com.example.model.LanguagePreferenceStore
import com.example.model.MessageSender
import com.example.tts.GramaGasTtsManager
import com.example.ui.components.ChatHeader
import com.example.ui.components.ChatInputBar
import com.example.ui.components.ChatMessageBubble
import com.example.ui.components.DateHeaderChip
import com.example.ui.components.SafetyNoticeBanner
import com.example.ui.components.TypingIndicatorBubble
import com.example.ui.components.WelcomeQuickActionsCard
import com.example.ui.theme.ActionGreenBg
import com.example.ui.theme.ChatCanvasLight
import com.example.ui.theme.ErrorTextDark
import com.example.ui.theme.GramaGreenPrimary
import com.example.ui.theme.TextPrimaryLight
import com.example.viewmodel.ChatViewModel
import com.example.voice.TeluguVoiceCallback
import com.example.voice.TeluguVoiceRecognizer
import com.example.screen.ScreenShareManager
import com.example.ui.ScreenShareAssistantScreen

@Composable
fun ChatScreen() {
    val context = LocalContext.current
    val languageStore = remember(context.applicationContext) {
        LanguagePreferenceStore(context.applicationContext)
    }
    val chatViewModel: ChatViewModel = viewModel(
        factory = remember(languageStore) { ChatViewModel.Factory(languageStore) }
    )

    val screenShareManager = remember(context.applicationContext) {
        ScreenShareManager(context.applicationContext)
    }

    DisposableEffect(screenShareManager) {
        onDispose {
            screenShareManager.stopProjection()
        }
    }

    val screenCaptureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK && result.data != null) {
            screenShareManager.startProjection(
                resultCode = result.resultCode,
                data = result.data!!,
                width = context.resources.displayMetrics.widthPixels,
                height = context.resources.displayMetrics.heightPixels,
                density = context.resources.displayMetrics.densityDpi
            )
        }
        chatViewModel.startScreenSharing()
    }

    val uiState by chatViewModel.uiState.collectAsStateWithLifecycle()
    val strings = uiState.strings
    val listState = rememberLazyListState()

    val isFreshSession = uiState.messages.size == 1 && !uiState.isResponding

    val ttsManager = remember(context.applicationContext) {
        GramaGasTtsManager(context.applicationContext)
    }

    DisposableEffect(ttsManager) {
        onDispose {
            ttsManager.release()
        }
    }

    val voiceRecognizer = remember(context.applicationContext, chatViewModel) {
        TeluguVoiceRecognizer(
            context = context.applicationContext,
            callback = object : TeluguVoiceCallback {
                override fun onListeningStarted(isFallbackRecorder: Boolean) {
                    chatViewModel.onVoiceListeningStarted(isFallbackRecorder)
                }

                override fun onPartialResult(partialText: String) {
                    chatViewModel.onVoicePartialResult(partialText)
                }

                override fun onAudioLevelChanged(normalizedLevel: Float) {
                    chatViewModel.onVoiceAudioLevelChanged(normalizedLevel)
                }

                override fun onFinalResult(recognizedText: String) {
                    chatViewModel.onVoiceFinalResult(recognizedText)
                }

                override fun onFallbackAudioCaptured(audioBytes: ByteArray) {
                    chatViewModel.onVoiceFallbackAudioCaptured(audioBytes)
                }

                override fun onError(localizedMessage: String) {
                    chatViewModel.onVoiceError(localizedMessage)
                }

                override fun onCancelled() {
                    chatViewModel.onVoiceCancelled()
                }
            }
        )
    }

    DisposableEffect(voiceRecognizer) {
        onDispose {
            voiceRecognizer.release()
        }
    }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            voiceRecognizer.startListening(uiState.selectedLanguage)
        } else {
            chatViewModel.onVoicePermissionDenied()
        }
    }

    val startVoiceInput: () -> Unit = {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPermission) {
            voiceRecognizer.startListening(uiState.selectedLanguage)
        } else {
            recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val lastAssistantMessage = remember(uiState.messages) {
        uiState.messages.lastOrNull { it.sender == MessageSender.ASSISTANT && !it.isStreaming && !it.isError }
    }

    LaunchedEffect(lastAssistantMessage?.id, uiState.isVoiceAssistantEnabled) {
        if (uiState.isVoiceAssistantEnabled) {
            lastAssistantMessage?.let { msg ->
                ttsManager.speak(msg.text, uiState.selectedLanguage) {
                    if (chatViewModel.uiState.value.isVoiceAssistantEnabled && !chatViewModel.uiState.value.isResponding) {
                        startVoiceInput()
                    }
                }
            }
        } else {
            ttsManager.stop()
        }
    }

    val openAppSettings: () -> Unit = {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", context.packageName, null)
        )
        context.startActivity(intent)
    }

    LaunchedEffect(
        uiState.messages.size,
        uiState.messages.lastOrNull()?.text?.length,
        uiState.isWaitingForFirstChunk
    ) {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
        }
    }

    val layoutDirection = if (uiState.selectedLanguage.isRtl) {
        LayoutDirection.Rtl
    } else {
        LayoutDirection.Ltr
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ChatCanvasLight)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
            ) {
                ChatHeader(
                    selectedLanguage = uiState.selectedLanguage,
                    strings = strings,
                    isTyping = uiState.isResponding,
                    isExtraLargeText = uiState.isExtraLargeText,
                    isVoiceAssistantEnabled = uiState.isVoiceAssistantEnabled,
                    onLanguageSelected = { newLanguage ->
                        voiceRecognizer.cancelListening()
                        chatViewModel.onLanguageSelected(newLanguage)
                    },
                    onToggleTextSize = chatViewModel::toggleTextSize,
                    onToggleVoiceAssistant = {
                        val willBeEnabled = !uiState.isVoiceAssistantEnabled
                        chatViewModel.toggleVoiceAssistant()
                        if (willBeEnabled) {
                            startVoiceInput()
                        } else {
                            voiceRecognizer.cancelListening()
                            ttsManager.stop()
                        }
                    },
                    onStartScreenSharing = {
                        val intent = screenShareManager.createScreenCaptureIntent()
                        screenCaptureLauncher.launch(intent)
                    },
                    onClearChatClick = chatViewModel::requestClearChat
                )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_chat_wallpaper_1795371472306),
                    contentDescription = stringResource(R.string.cd_chat_background),
                    contentScale = ContentScale.Crop,
                    alpha = 0.52f,
                    modifier = Modifier.fillMaxSize()
                )

                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(
                        horizontal = 12.dp,
                        vertical = 14.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .testTag("chat_message_list")
                ) {
                    item(key = "date_chip") {
                        DateHeaderChip(strings = strings)
                    }

                    item(key = "safety_banner") {
                        SafetyNoticeBanner(
                            strings = strings,
                            isExtraLargeText = uiState.isExtraLargeText
                        )
                    }

                    items(
                        items = uiState.messages,
                        key = { it.id }
                    ) { message ->
                        ChatMessageBubble(
                            message = message,
                            strings = strings,
                            isExtraLargeText = uiState.isExtraLargeText,
                            onRetryClick = chatViewModel::retryMessage,
                            onSpeakClick = { text ->
                                ttsManager.speak(text, uiState.selectedLanguage)
                            }
                        )
                    }

                    if (isFreshSession) {
                        item(key = "welcome_quick_actions") {
                            WelcomeQuickActionsCard(
                                selectedLanguage = uiState.selectedLanguage,
                                strings = strings,
                                isResponding = uiState.isResponding,
                                isExtraLargeText = uiState.isExtraLargeText,
                                onQuickActionClick = chatViewModel::onQuickActionSelected,
                                onProviderClick = chatViewModel::onProviderSelected
                            )
                        }
                    }

                    if (uiState.isWaitingForFirstChunk) {
                        item(key = "typing_indicator") {
                            TypingIndicatorBubble(
                                strings = strings,
                                statusText = uiState.retryingStatusMessage
                            )
                        }
                    }
                }

                // Subtle translated toast notification when switching languages
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.languageNoticeMessage != null,
                    enter = slideInVertically { -it } + fadeIn(),
                    exit = slideOutVertically { -it } + fadeOut(),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 10.dp, start = 16.dp, end = 16.dp)
                ) {
                    val noticeText = uiState.languageNoticeMessage.orEmpty()
                    Surface(
                        onClick = chatViewModel::dismissLanguageNotice,
                        shape = RoundedCornerShape(24.dp),
                        color = ActionGreenBg,
                        border = BorderStroke(1.dp, GramaGreenPrimary.copy(alpha = 0.4f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier.testTag("language_changed_notice")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = GramaGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = noticeText,
                                style = MaterialTheme.typography.labelLarge,
                                color = GramaGreenPrimary
                            )
                        }
                    }
                }
            }

            ChatInputBar(
                inputText = uiState.inputText,
                strings = strings,
                isResponding = uiState.isResponding,
                isExtraLargeText = uiState.isExtraLargeText,
                showQuickChipsRow = !isFreshSession,
                voiceState = uiState.voiceState,
                showVoiceTranscribedHint = uiState.showVoiceTranscribedHint,
                onInputTextChanged = chatViewModel::onInputTextChanged,
                onSendClick = {
                    voiceRecognizer.cancelListening()
                    chatViewModel.sendCurrentInput()
                },
                onQuickActionClick = { action ->
                    voiceRecognizer.cancelListening()
                    chatViewModel.onQuickActionSelected(action)
                },
                onVoiceButtonClick = startVoiceInput,
                onStopVoiceClick = { voiceRecognizer.stopListening() },
                onCancelVoiceClick = { voiceRecognizer.cancelListening() },
                onOpenSettingsClick = openAppSettings,
                onDismissVoiceErrorClick = chatViewModel::dismissVoiceError
            )
        }

        AnimatedVisibility(
            visible = uiState.isScreenSharingActive,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            ScreenShareAssistantScreen(
                strings = strings,
                selectedLanguage = uiState.selectedLanguage,
                guidanceText = uiState.screenAssistantGuidance,
                isAnalyzing = uiState.isAnalyzingScreen,
                isExtraLargeText = uiState.isExtraLargeText,
                onAnalyzeScreenClick = {
                    val bitmap = screenShareManager.captureLatestFrameBitmap(
                        fallbackWidth = context.resources.displayMetrics.widthPixels.coerceAtLeast(720),
                        fallbackHeight = context.resources.displayMetrics.heightPixels.coerceAtLeast(1280)
                    )
                    val bytes = screenShareManager.bitmapToJpegBytes(bitmap)
                    chatViewModel.analyzeScreenFrame(bytes)
                },
                onVoiceAskClick = startVoiceInput,
                onSpeakGuidanceClick = { text ->
                    ttsManager.speak(text, uiState.selectedLanguage)
                },
                onStopSharingClick = {
                    screenShareManager.stopProjection()
                    chatViewModel.stopScreenSharing()
                }
            )
        }

        if (uiState.showClearDialog) {
            AlertDialog(
                onDismissRequest = chatViewModel::dismissClearDialog,
                shape = RoundedCornerShape(22.dp),
                containerColor = Color.White,
                icon = {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = null,
                        tint = ErrorTextDark
                    )
                },
                title = {
                    Text(
                        text = strings.clearChatTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = TextPrimaryLight
                    )
                },
                text = {
                    Text(
                        text = strings.clearChatBody,
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimaryLight.copy(alpha = 0.85f)
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            voiceRecognizer.cancelListening()
                            chatViewModel.confirmClearChat()
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = GramaGreenPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("confirm_clear_chat_button")
                    ) {
                        Text(
                            text = strings.clearChatConfirm,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = chatViewModel::dismissClearDialog,
                        modifier = Modifier.testTag("cancel_clear_chat_button")
                    ) {
                        Text(
                            text = strings.clearChatCancel,
                            style = MaterialTheme.typography.labelLarge,
                            color = GramaGreenPrimary
                        )
                    }
                }
            )
        }
        }
    }
}
