package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MicOff
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.UiStrings
import com.example.model.VoiceInputState
import com.example.ui.theme.ActionGreenBg
import com.example.ui.theme.ErrorBorderSoft
import com.example.ui.theme.ErrorContainerSoft
import com.example.ui.theme.ErrorTextDark
import com.example.ui.theme.GramaGreenPrimary
import com.example.ui.theme.GramaGreenVibrant
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun VoiceListeningPanel(
    voiceState: VoiceInputState,
    strings: UiStrings,
    isExtraLargeText: Boolean,
    onStopVoiceClick: () -> Unit,
    onCancelVoiceClick: () -> Unit,
    onRetryVoiceClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismissErrorClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = voiceState !is VoiceInputState.Idle,
        enter = expandVertically(animationSpec = spring()) + fadeIn(tween(180)),
        exit = shrinkVertically(animationSpec = spring()) + fadeOut(tween(140)),
        modifier = modifier.fillMaxWidth()
    ) {
        when (voiceState) {
            is VoiceInputState.Listening -> {
                ListeningCard(
                    state = voiceState,
                    strings = strings,
                    isExtraLargeText = isExtraLargeText,
                    onStopClick = onStopVoiceClick,
                    onCancelClick = onCancelVoiceClick
                )
            }

            VoiceInputState.Transcribing -> {
                TranscribingCard(
                    strings = strings,
                    isExtraLargeText = isExtraLargeText
                )
            }

            is VoiceInputState.Error -> {
                VoiceErrorCard(
                    state = voiceState,
                    strings = strings,
                    isExtraLargeText = isExtraLargeText,
                    onRetryClick = onRetryVoiceClick,
                    onOpenSettingsClick = onOpenSettingsClick,
                    onDismissClick = onDismissErrorClick
                )
            }

            VoiceInputState.Idle -> Unit
        }
    }
}

@Composable
private fun ListeningCard(
    state: VoiceInputState.Listening,
    strings: UiStrings,
    isExtraLargeText: Boolean,
    onStopClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    val pulseTransition = rememberInfiniteTransition(label = "voice_pulse")
    val pulsePhase by pulseTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_phase"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("voice_listening_panel"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.5.dp, GramaGreenVibrant.copy(alpha = 0.55f)),
        shadowElevation = 3.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFDECEC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size((14 + (state.audioLevel * 10f) + (pulsePhase * 4f)).dp)
                                .clip(CircleShape)
                                .background(Color(0xFFD32F2F).copy(alpha = 0.25f))
                        )
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color(0xFFD32F2F),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Column {
                        Text(
                            text = strings.voiceListeningTitle,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontSize = if (isExtraLargeText) 17.sp else 15.5.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = GramaGreenPrimary
                        )
                        LiveVoiceWaveform(
                            audioLevel = state.audioLevel,
                            pulsePhase = pulsePhase
                        )
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = ActionGreenBg.copy(alpha = 0.7f)
            ) {
                Text(
                    text = state.partialText.ifBlank { strings.voiceListeningHint },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = if (isExtraLargeText) 18.5.sp else 16.sp,
                        lineHeight = if (isExtraLargeText) 26.sp else 23.sp
                    ),
                    color = if (state.partialText.isBlank()) {
                        TextSecondaryLight
                    } else {
                        TextPrimaryLight
                    },
                    modifier = Modifier
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                        .testTag("voice_partial_transcript")
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancelClick,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, TextSecondaryLight.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondaryLight),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("voice_cancel_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.voiceCancelButton,
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onStopClick,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GramaGreenPrimary,
                        contentColor = Color.White
                    ),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    modifier = Modifier
                        .weight(1.2f)
                        .minimumInteractiveComponentSize()
                        .testTag("voice_done_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.voiceStopButton,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveVoiceWaveform(
    audioLevel: Float,
    pulsePhase: Float
) {
    val barMultipliers = listOf(0.55f, 0.9f, 1.0f, 0.8f, 0.5f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.height(20.dp)
    ) {
        barMultipliers.forEachIndexed { idx, weight ->
            val phaseOffset = if (idx % 2 == 0) pulsePhase else (1.35f - pulsePhase)
            val targetHeight = (6f + (audioLevel * 12f * weight) + (phaseOffset * 4f * weight))
                .coerceIn(5f, 20f)
            val animatedHeight by animateDpAsState(
                targetValue = targetHeight.dp,
                animationSpec = tween(durationMillis = 90),
                label = "bar_$idx"
            )
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(animatedHeight)
                    .clip(RoundedCornerShape(4.dp))
                    .background(GramaGreenVibrant)
            )
        }
    }
}

@Composable
private fun TranscribingCard(
    strings: UiStrings,
    isExtraLargeText: Boolean
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("voice_transcribing_panel"),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, GramaGreenVibrant.copy(alpha = 0.4f)),
        shadowElevation = 2.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            CircularProgressIndicator(
                color = GramaGreenPrimary,
                strokeWidth = 2.5.dp,
                modifier = Modifier.size(22.dp)
            )
            Text(
                text = strings.voiceTranscribingTitle,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = if (isExtraLargeText) 17.sp else 15.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = TextPrimaryLight
            )
        }
    }
}

@Composable
private fun VoiceErrorCard(
    state: VoiceInputState.Error,
    strings: UiStrings,
    isExtraLargeText: Boolean,
    onRetryClick: () -> Unit,
    onOpenSettingsClick: () -> Unit,
    onDismissClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 10.dp, vertical = 6.dp)
            .testTag("voice_error_panel"),
        shape = RoundedCornerShape(16.dp),
        color = ErrorContainerSoft,
        border = BorderStroke(1.dp, ErrorBorderSoft)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.MicOff,
                    contentDescription = null,
                    tint = ErrorTextDark,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = state.message,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = if (isExtraLargeText) 16.sp else 14.sp,
                        lineHeight = if (isExtraLargeText) 23.sp else 20.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = ErrorTextDark,
                    modifier = Modifier.weight(1f)
                )
                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("dismiss_voice_error_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_dismiss_error),
                        tint = ErrorTextDark,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (state.isPermissionError) {
                    OutlinedButton(
                        onClick = onOpenSettingsClick,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorTextDark.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorTextDark),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("voice_open_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Settings,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.voiceOpenSettings,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Button(
                    onClick = onRetryClick,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = GramaGreenPrimary,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("voice_retry_button")
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = strings.voiceTryAgain,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}
