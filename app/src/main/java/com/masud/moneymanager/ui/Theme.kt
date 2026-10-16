// FILE: app/src/main/java/com/masud/moneymanager/ui/Theme.kt
package com.masud.moneymanager.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// থিন রেড অ্যাকসেন্ট — দুই থিমেই ব্যবহার হবে
private val ThinRedDark = Color(0xFFE03131)
private val ThinRedLight = Color(0xFFD32F2F)

private val OffBlack = Color(0xFF0D0D0D)
private val OffBlackSurface = Color(0xFF171414)

private val OffWhite = Color(0xFFFAFAF8)
private val OffWhiteSurface = Color(0xFFFFFFFF)

private val AppDarkColors = darkColorScheme(
    primary = ThinRedDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF3A1A1A),
    onPrimaryContainer = Color(0xFFFFD6D6),

    secondary = Color(0xFFB8ADAD),
    onSecondary = Color(0xFF1A1414),

    background = OffBlack,
    onBackground = Color(0xFFF2EDEC),

    surface = OffBlackSurface,
    onSurface = Color(0xFFF2EDEC),

    surfaceVariant = Color(0xFF231F1F),
    onSurfaceVariant = Color(0xFFB8ADAD),

    outline = Color(0xFF3A3232),
    outlineVariant = Color(0xFF2A2424),
    error = ThinRedDark
)

private val AppLightColors = lightColorScheme(
    primary = ThinRedLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFE1E1),
    onPrimaryContainer = Color(0xFF3A0A0A),

    secondary = Color(0xFF6B5F5F),
    onSecondary = Color.White,

    background = OffWhite,
    onBackground = Color(0xFF1C1816),

    surface = OffWhiteSurface,
    onSurface = Color(0xFF1C1816),

    surfaceVariant = Color(0xFFF1EBE8),
    onSurfaceVariant = Color(0xFF554C4A),

    outline = Color(0xFFE2D8D5),
    outlineVariant = Color(0xFFEDE5E2),
    error = ThinRedLight
)

@Composable
fun MoneyManagerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AppDarkColors else AppLightColors,
        content = content
    )
}
