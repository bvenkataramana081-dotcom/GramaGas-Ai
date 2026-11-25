package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AppLanguage
import com.example.model.UiStrings
import com.example.ui.theme.ActionGreenBg
import com.example.ui.theme.AssistantBubbleBorderLight
import com.example.ui.theme.GramaGreenAccent
import com.example.ui.theme.GramaGreenDark
import com.example.ui.theme.GramaGreenPrimary
import com.example.ui.theme.TextPrimaryLight
import com.example.ui.theme.TextSecondaryLight

@Composable
fun ChatHeader(
    selectedLanguage: AppLanguage,
    strings: UiStrings,
    isTyping: Boolean,
    isExtraLargeText: Boolean,
    isVoiceAssistantEnabled: Boolean,
    onLanguageSelected: (AppLanguage) -> Unit,
    onToggleTextSize: () -> Unit,
    onToggleVoiceAssistant: () -> Unit,
    onStartScreenSharing: () -> Unit,
    onClearChatClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showLanguageMenu by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shadowElevation = 4.dp,
        color = GramaGreenPrimary
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(GramaGreenDark, GramaGreenPrimary)
                    )
                )
                .statusBarsPadding()
                .padding(horizontal = 10.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(modifier = Modifier.size(44.dp)) {
                        Image(
                            painter = painterResource(id = R.drawable.img_gramagas_avatar_1795371473422),
                            contentDescription = stringResource(R.string.cd_assistant_avatar),
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, Color.White.copy(alpha = 0.85f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .align(Alignment.BottomEnd)
                                .clip(CircleShape)
                                .background(GramaGreenAccent)
                                .border(2.dp, GramaGreenPrimary, CircleShape)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = if (isTyping) strings.subtitleTyping else strings.subtitleOnline,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontSize = 12.5.sp,
                                fontWeight = if (isTyping) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isTyping) {
                                Color(0xFFB9F6CA)
                            } else {
                                Color.White.copy(alpha = 0.88f)
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Voice Assistant ON/OFF Toggle Button
                    Surface(
                        onClick = onToggleVoiceAssistant,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isVoiceAssistantEnabled) {
                            Color(0xFF2E7D32).copy(alpha = 0.85f)
                        } else {
                            Color(0xFFC62828).copy(alpha = 0.85f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("voice_assistant_toggle_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isVoiceAssistantEnabled) Color(0xFFA5D6A7)
                                        else Color(0xFFFFCDD2)
                                    )
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = if (isVoiceAssistantEnabled) strings.voiceAssistantOn else strings.voiceAssistantOff,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Screen Share Assistant Button
                    Surface(
                        onClick = onStartScreenSharing,
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.22f),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("start_screen_sharing_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 7.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = strings.startScreenAssistant,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Wishflow",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.5.sp
                                ),
                                color = Color.White
                            )
                        }
                    }

                    // Language Selector Dropdown Button
                    Box {
                        Surface(
                            onClick = { showLanguageMenu = true },
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.18f),
                            border = BorderStroke(
                                width = 1.dp,
                                color = Color.White.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("language_selector_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(start = 8.dp, end = 4.dp, top = 7.dp, bottom = 7.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Translate,
                                    contentDescription = strings.cdSelectLanguage,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = selectedLanguage.nativeName,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.5.sp
                                    ),
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Icon(
                                    imageVector = Icons.Rounded.ArrowDropDown,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            containerColor = Color.White,
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .heightIn(max = 420.dp)
                                .testTag("language_selector_menu")
                        ) {
                            Text(
                                text = strings.selectLanguageTitle,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontWeight = FontWeight.Bold
                                ),
                                color = GramaGreenPrimary,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                            )
                            HorizontalDivider(color = AssistantBubbleBorderLight)

                            AppLanguage.entries.forEach { language ->
                                val isSelected = language == selectedLanguage
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = language.nativeName,
                                                    style = MaterialTheme.typography.titleSmall.copy(
                                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                        fontSize = 15.5.sp
                                                    ),
                                                    color = if (isSelected) GramaGreenPrimary else TextPrimaryLight
                                                )
                                                Text(
                                                    text = language.englishName,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = TextSecondaryLight
                                                )
                                            }
                                            if (isSelected) {
                                                Icon(
                                                    imageVector = Icons.Rounded.Check,
                                                    contentDescription = null,
                                                    tint = GramaGreenPrimary,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        showLanguageMenu = false
                                        onLanguageSelected(language)
                                    },
                                    modifier = Modifier
                                        .background(
                                            if (isSelected) ActionGreenBg else Color.Transparent
                                        )
                                        .testTag("language_option_${language.code}")
                                )
                            }
                        }
                    }

                    Surface(
                        onClick = onToggleTextSize,
                        shape = RoundedCornerShape(12.dp),
                        color = if (isExtraLargeText) {
                            Color.White
                        } else {
                            Color.White.copy(alpha = 0.16f)
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.4f)
                        ),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("toggle_text_size_button")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = if (isExtraLargeText) strings.textSizeLarge else strings.textSizeNormal,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                color = if (isExtraLargeText) GramaGreenPrimary else Color.White
                            )
                        }
                    }

                    IconButton(
                        onClick = onClearChatClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("clear_chat_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = strings.cdClearChat,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}
