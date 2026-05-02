package com.flowpay.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val colors = lightColorScheme(
    primary = Color(0xFF216D5B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDF2EA),
    onPrimaryContainer = Color(0xFF0B2F26),
    secondary = Color(0xFF5D5F71),
    secondaryContainer = Color(0xFFE5E7F5),
    tertiaryContainer = Color(0xFFFFE8C7),
    background = Color(0xFFF7FAF8),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFE8EEE9),
    outlineVariant = Color(0xFFC9D4CE),
    error = Color(0xFFC04444),
)

@Composable
fun FlowPayTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = colors,
        content = content,
    )
}
