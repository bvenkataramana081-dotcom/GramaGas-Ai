package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DoneAll
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.LocalShipping
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sms
import androidx.compose.material.icons.rounded.VolumeUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.ChatMessage
import com.example.model.LpgProvider
import com.example.model.MessageSender
import com.example.model.QuickActionType
import com.example.model.UiStrings
import com.example.viewmodel.friendlyMessageForErrorType
import com.example.ui.theme.ActionBlueBg
import com.example.ui.theme.ActionBlueFg
import com.example.ui.theme.ActionGreenBg
import com.example.ui.theme.ActionGreenFg
import com.example.ui.theme.ActionOrangeBg
import com.example.ui.theme.ActionOrangeFg
import com.example.ui.theme.ActionRedBg
import com.example.ui.theme.ActionRedFg
import com.example.ui.theme.AssistantBubbleBorderLight
import com.example.ui.theme.AssistantBubbleLight
import com.example.ui.theme.ErrorBorderSoft
import com.example.ui.theme.ErrorContainerSoft
import com.example.ui.theme.ErrorTextDark
import com.example.ui.theme.GramaGreenPrimary
import com.example.ui.theme.GramaGreenVibrant
import com.example.ui.theme.SafetyBannerBgLight
import com.example.ui.theme.SafetyBannerBorderLight
import com.example.ui.theme.SafetyBannerTextLight
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight
import com.example.ui.theme.TimestampColorLight
import com.example.ui.theme.UserBubbleBorderLight
import com.example.ui.theme.UserBubbleLight

@Composable
fun DateHeaderChip(
    strings: UiStrings,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White.copy(alpha = 0.92f),
            border = BorderStroke(1.dp, AssistantBubbleBorderLight),
            shadowElevation = 1.dp
        ) {
            Text(
                text = strings.todayLabel,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                ),
                color = TextSecondaryLight,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
            )
        }
    }
}

@Composable
fun SafetyNoticeBanner(
    strings: UiStrings,
    isExtraLargeText: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("safety_notice_banner"),
        shape = RoundedCornerShape(14.dp),
        color = SafetyBannerBgLight,
        border = BorderStroke(1.dp, SafetyBannerBorderLight)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Rounded.Shield,
                contentDescription = stringResource(R.string.cd_safety_icon),
                tint = SafetyBannerTextLight,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = strings.safetyBannerText,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontSize = if (isExtraLargeText) 15.sp else 13.5.sp,
                    lineHeight = if (isExtraLargeText) 22.sp else 19.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = SafetyBannerTextLight
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WelcomeQuickActionsCard(
    selectedLanguage: AppLanguage,
    strings: UiStrings,
    isResponding: Boolean,
    isExtraLargeText: Boolean,
    onQuickActionClick: (QuickActionType) -> Unit,
    onProviderClick: (LpgProvider) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("welcome_quick_actions_card"),
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, AssistantBubbleBorderLight),
        shadowElevation = 2.dp
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.supportedProvidersLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = if (isExtraLargeText) 15.sp else 13.5.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextSecondaryLight
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LpgProvider.entries.forEach { provider ->
                        Surface(
                            onClick = { onProviderClick(provider) },
                            enabled = !isResponding,
                            shape = RoundedCornerShape(20.dp),
                            color = ActionGreenBg,
                            border = BorderStroke(1.dp, GramaGreenPrimary.copy(alpha = 0.25f)),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag(provider.testTag)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocalFireDepartment,
                                    contentDescription = null,
                                    tint = GramaGreenPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = provider.displayName,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = if (isExtraLargeText) 14.5.sp else 13.sp
                                    ),
                                    color = GramaGreenPrimary
                                )
                            }
                        }
                    }
                }
            }

            Text(
                text = strings.quickActionsTitle,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = if (isExtraLargeText) 16.sp else 14.5.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimaryLight
            )

            val actions = QuickActionType.entries
            val showEnglishSubtitle = selectedLanguage != AppLanguage.ENGLISH
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionGridButton(
                        action = actions[0],
                        label = actions[0].label(strings),
                        showEnglishSubtitle = showEnglishSubtitle,
                        enabled = !isResponding,
                        isExtraLargeText = isExtraLargeText,
                        onClick = { onQuickActionClick(actions[0]) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionGridButton(
                        action = actions[1],
                        label = actions[1].label(strings),
                        showEnglishSubtitle = showEnglishSubtitle,
                        enabled = !isResponding,
                        isExtraLargeText = isExtraLargeText,
                        onClick = { onQuickActionClick(actions[1]) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    QuickActionGridButton(
                        action = actions[2],
                        label = actions[2].label(strings),
                        showEnglishSubtitle = showEnglishSubtitle,
                        enabled = !isResponding,
                        isExtraLargeText = isExtraLargeText,
                        onClick = { onQuickActionClick(actions[2]) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickActionGridButton(
                        action = actions[3],
                        label = actions[3].label(strings),
                        showEnglishSubtitle = showEnglishSubtitle,
                        enabled = !isResponding,
                        isExtraLargeText = isExtraLargeText,
                        onClick = { onQuickActionClick(actions[3]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActionGridButton(
    action: QuickActionType,
    label: String,
    showEnglishSubtitle: Boolean,
    enabled: Boolean,
    isExtraLargeText: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val visual = quickActionVisual(action)
    Surface(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(14.dp),
        color = if (enabled) visual.bgColor else visual.bgColor.copy(alpha = 0.5f),
        border = BorderStroke(1.dp, visual.fgColor.copy(alpha = 0.2f)),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .testTag("${action.testTag}_card")
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visual.icon,
                    contentDescription = null,
                    tint = visual.fgColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontSize = if (isExtraLargeText) 16.5.sp else 15.sp,
                    lineHeight = if (isExtraLargeText) 23.sp else 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = TextPrimaryLight
            )
            if (showEnglishSubtitle) {
                Text(
                    text = action.englishSubtitle,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = if (isExtraLargeText) 12.5.sp else 11.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = visual.fgColor
                )
            }
        }
    }
}

@Composable
fun ChatMessageBubble(
    message: ChatMessage,
    strings: UiStrings,
    isExtraLargeText: Boolean,
    onRetryClick: (String, String) -> Unit,
    onSpeakClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isUser = message.sender == MessageSender.USER
    val bodyFontSize = if (isExtraLargeText) 20.sp else 17.sp
    val bodyLineHeight = if (isExtraLargeText) 31.sp else 26.5.sp

    val bubbleShape = if (isUser) {
        RoundedCornerShape(
            topStart = 18.dp,
            topEnd = 4.dp,
            bottomEnd = 18.dp,
            bottomStart = 18.dp
        )
    } else {
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = 18.dp,
            bottomEnd = 18.dp,
            bottomStart = 18.dp
        )
    }

    val bubbleColor = when {
        message.isError -> ErrorContainerSoft
        isUser -> UserBubbleLight
        else -> AssistantBubbleLight
    }

    val borderColor = when {
        message.isError -> ErrorBorderSoft
        isUser -> UserBubbleBorderLight
        else -> AssistantBubbleBorderLight
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag(if (isUser) "user_message_bubble" else "assistant_message_bubble"),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = bubbleShape,
            color = bubbleColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 1.5.dp,
            modifier = Modifier.widthIn(min = 96.dp, max = 330.dp)
        ) {
            Column(
                modifier = Modifier.padding(
                    start = 14.dp,
                    end = 14.dp,
                    top = 10.dp,
                    bottom = 8.dp
                )
            ) {
                if (message.isError) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.WarningAmber,
                            contentDescription = null,
                            tint = ErrorTextDark,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.connectionErrorTitle,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.Bold
                            ),
                            color = ErrorTextDark
                        )
                    }
                }

                val displayText = if (message.isError && message.errorType != null) {
                    friendlyMessageForErrorType(message.errorType, strings)
                } else {
                    message.text
                }

                Text(
                    text = displayText,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontSize = bodyFontSize,
                        lineHeight = bodyLineHeight
                    ),
                    color = if (message.isError) ErrorTextDark else TextPrimaryLight
                )

                if (message.isError && !message.failedPrompt.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onRetryClick(message.id, message.failedPrompt) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ErrorTextDark.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorTextDark),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("retry_message_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.retryAction,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (!isUser) {
                        IconButton(
                            onClick = { onSpeakClick(displayText) },
                            modifier = Modifier
                                .size(28.dp)
                                .testTag("speak_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.VolumeUp,
                                contentDescription = "Read aloud",
                                tint = GramaGreenPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = message.timestampLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 11.5.sp
                            ),
                            color = if (message.isError) {
                                ErrorTextDark.copy(alpha = 0.75f)
                            } else {
                                TimestampColorLight
                            },
                            textAlign = TextAlign.End
                        )
                        if (isUser) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Rounded.DoneAll,
                                contentDescription = stringResource(R.string.cd_message_sent),
                                tint = GramaGreenVibrant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TypingIndicatorBubble(
    strings: UiStrings,
    statusText: String? = null,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "typing_dots")
    val dot1Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot1"
    )
    val dot2Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, delayMillis = 170),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot2"
    )
    val dot3Alpha by transition.animateFloat(
        initialValue = 0.25f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, delayMillis = 340),
            repeatMode = RepeatMode.Reverse
        ),
        label = "dot3"
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("typing_indicator_bubble"),
        horizontalArrangement = Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 4.dp,
                topEnd = 18.dp,
                bottomEnd = 18.dp,
                bottomStart = 18.dp
            ),
            color = AssistantBubbleLight,
            border = BorderStroke(1.dp, AssistantBubbleBorderLight),
            shadowElevation = 1.5.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(dot1Alpha, dot2Alpha, dot3Alpha).forEach { alpha ->
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .alpha(alpha)
                            .clip(CircleShape)
                            .background(GramaGreenPrimary)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = statusText ?: strings.subtitleTyping,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp
                    ),
                    color = TextSecondaryLight
                )
            }
        }
    }
}

internal data class QuickActionVisual(
    val icon: ImageVector,
    val bgColor: Color,
    val fgColor: Color
)

internal fun quickActionVisual(action: QuickActionType): QuickActionVisual {
    return when (action) {
        QuickActionType.LPG_BOOKING_HELP -> QuickActionVisual(
            icon = Icons.Rounded.LocalFireDepartment,
            bgColor = ActionOrangeBg,
            fgColor = ActionOrangeFg
        )
        QuickActionType.UNDERSTAND_SMS -> QuickActionVisual(
            icon = Icons.Rounded.Sms,
            bgColor = ActionBlueBg,
            fgColor = ActionBlueFg
        )
        QuickActionType.DELIVERY_INFO -> QuickActionVisual(
            icon = Icons.Rounded.LocalShipping,
            bgColor = ActionGreenBg,
            fgColor = ActionGreenFg
        )
        QuickActionType.SUSPICIOUS_MESSAGE -> QuickActionVisual(
            icon = Icons.Rounded.Security,
            bgColor = ActionRedBg,
            fgColor = ActionRedFg
        )
    }
}
