package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GramaGasLightColorScheme = lightColorScheme(
    primary = GramaGreenPrimary,
    onPrimary = Color.White,
    primaryContainer = UserBubbleLight,
    onPrimaryContainer = TextPrimaryLight,
    secondary = GramaGreenVibrant,
    onSecondary = Color.White,
    secondaryContainer = ActionGreenBg,
    onSecondaryContainer = GramaGreenPrimary,
    tertiary = GramaGreenAccent,
    onTertiary = TextPrimaryLight,
    background = ChatCanvasLight,
    onBackground = TextPrimaryLight,
    surface = AssistantBubbleLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = Color(0xFFE5EFE6),
    onSurfaceVariant = TextSecondaryLight,
    outline = AssistantBubbleBorderLight,
    error = ErrorTextDark,
    onError = Color.White,
    errorContainer = ErrorContainerSoft,
    onErrorContainer = ErrorTextDark
)

@Composable
fun GramaGasTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GramaGasLightColorScheme,
        typography = GramaGasTypography,
        content = content
    )
}
