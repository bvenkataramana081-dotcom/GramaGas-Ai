package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isShiftPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.QuickActionType
import com.example.model.UiStrings
import com.example.model.VoiceInputState
import com.example.ui.theme.ActionGreenBg
import com.example.ui.theme.AssistantBubbleBorderLight
import com.example.ui.theme.GramaGreenPrimary
import com.example.ui.theme.GramaGreenVibrant
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun ChatInputBar(
    inputText: String,
    strings: UiStrings,
    isResponding: Boolean,
    isExtraLargeText: Boolean,
    showQuickChipsRow: Boolean,
    voiceState: VoiceInputState,
    showVoiceTranscribedHint: Boolean,
    onInputTextChanged: (String) -> Unit,
    onSendClick: () -> Unit,
    onQuickActionClick: (QuickActionType) -> Unit,
    onVoiceButtonClick: () -> Unit,
    onStopVoiceClick: () -> Unit,
    onCancelVoiceClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismissVoiceErrorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isListening = voiceState is VoiceInputState.Listening
    val isTranscribing = voiceState is VoiceInputState.Transcribing
    val canSend = inputText.isNotBlank() && !isResponding && !isListening && !isTranscribing
    val canUseMic = !isResponding && !isTranscribing

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color(0xFFF6FAF6),
        shadowElevation = 6.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(top = 6.dp, bottom = 10.dp)
        ) {
            VoiceListeningPanel(
                voiceState = voiceState,
                strings = strings,
                isExtraLargeText = isExtraLargeText,
                onStopVoiceClick = onStopVoiceClick,
                onCancelVoiceClick = onCancelVoiceClick,
                onRetryVoiceClick = onVoiceButtonClick,
                onOpenSettingsClick = onOpenSettingsClick,
                onDismissErrorClick = onDismissVoiceErrorClick
            )

            AnimatedVisibility(
                visible = showVoiceTranscribedHint && inputText.isNotBlank() && voiceState is VoiceInputState.Idle,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 4.dp)
                        .testTag("voice_ready_hint")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = GramaGreenVibrant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.voiceReadyHint,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = if (isExtraLargeText) 14.5.sp else 13.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        color = GramaGreenPrimary
                    )
                }
            }

            if (showQuickChipsRow && !isListening) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("quick_action_chip_strip")
                ) {
                    items(QuickActionType.entries, key = { it.name }) { action ->
                        val visual = quickActionVisual(action)
                        Surface(
                            onClick = { onQuickActionClick(action) },
                            enabled = !isResponding,
                            shape = RoundedCornerShape(20.dp),
                            color = Color.White,
                            border = BorderStroke(1.dp, visual.fgColor.copy(alpha = 0.28f)),
                            shadowElevation = 1.dp,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(action.testTag)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = visual.icon,
                                    contentDescription = null,
                                    tint = visual.fgColor,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = action.label(strings),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontSize = if (isExtraLargeText) 15.sp else 13.5.sp,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextPrimaryLight
                                )
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputTextChanged,
                    placeholder = {
                        Text(
                            text = strings.inputPlaceholder,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = if (isExtraLargeText) 18.sp else 16.sp
                            ),
                            color = TextSecondaryLight.copy(alpha = 0.78f)
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isExtraLargeText) 19.sp else 16.5.sp,
                        lineHeight = if (isExtraLargeText) 27.sp else 23.sp,
                        color = TextPrimaryLight
                    ),
                    shape = RoundedCornerShape(26.dp),
                    maxLines = 4,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = GramaGreenVibrant,
                        unfocusedBorderColor = AssistantBubbleBorderLight,
                        cursorColor = GramaGreenPrimary
                    ),
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Send
                    ),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (canSend) {
                                onSendClick()
                            }
                        }
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .onPreviewKeyEvent { keyEvent ->
                            if (keyEvent.type == KeyEventType.KeyDown &&
                                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter) &&
                                !keyEvent.isShiftPressed
                            ) {
                                if (canSend) {
                                    onSendClick()
                                }
                                true
                            } else {
                                false
                            }
                        }
                        .testTag("message_input_field")
                )

                Surface(
                    onClick = {
                        if (isListening) onStopVoiceClick() else onVoiceButtonClick()
                    },
                    enabled = canUseMic,
                    shape = CircleShape,
                    color = when {
                        isListening -> Color(0xFFD32F2F)
                        canUseMic -> ActionGreenBg
                        else -> ActionGreenBg.copy(alpha = 0.45f)
                    },
                    border = if (isListening) {
                        null
                    } else {
                        BorderStroke(1.2.dp, GramaGreenPrimary.copy(alpha = if (canUseMic) 0.45f else 0.15f))
                    },
                    shadowElevation = if (isListening) 4.dp else 1.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("voice_input_button")
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isListening) Icons.Rounded.Stop else Icons.Rounded.Mic,
                            contentDescription = if (isListening) strings.cdVoiceStop else strings.cdVoiceStart,
                            tint = when {
                                isListening -> Color.White
                                canUseMic -> GramaGreenPrimary
                                else -> GramaGreenPrimary.copy(alpha = 0.4f)
                            },
                            modifier = Modifier.size(25.dp)
                        )
                    }
                }

                Surface(
                    onClick = onSendClick,
                    enabled = canSend,
                    shape = CircleShape,
                    color = if (canSend) {
                        GramaGreenPrimary
                    } else {
                        GramaGreenPrimary.copy(alpha = 0.35f)
                    },
                    shadowElevation = if (canSend) 3.dp else 0.dp,
                    modifier = Modifier
                        .size(52.dp)
                        .testTag("send_message_button")
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.background(Color.Transparent)
                    ) {
                        if (isResponding) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(22.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.Send,
                                contentDescription = strings.cdSendMessage,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
