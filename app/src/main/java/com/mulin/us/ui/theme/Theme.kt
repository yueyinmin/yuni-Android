package com.mulin.us.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * 予你的配色。
 * 走"暖木色 + 淡胭脂"的路子：两个人用的东西，别做得像后台管理系统。
 */
private val WarmRose = Color(0xFFB4556A)
private val WarmRoseDark = Color(0xFF8C3F52)
private val Sand = Color(0xFFF7F1EC)
private val Ink = Color(0xFF2B2426)
private val NightBg = Color(0xFF141013)
private val NightSurface = Color(0xFF1F1A1D)

private val LightColors = lightColorScheme(
    primary = WarmRose,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFF6DDE2),
    onPrimaryContainer = WarmRoseDark,
    secondary = Color(0xFF8A7A6D),
    background = Sand,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = Color(0xFFEFE6E0),
    error = Color(0xFFB3261E)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5A9B6),
    onPrimary = Color(0xFF4A1F29),
    primaryContainer = Color(0xFF6B3341),
    onPrimaryContainer = Color(0xFFF9DCE2),
    secondary = Color(0xFFC9B8A8),
    background = NightBg,
    onBackground = Color(0xFFEDE4E6),
    surface = NightSurface,
    onSurface = Color(0xFFEDE4E6),
    surfaceVariant = Color(0xFF2E272A),
    error = Color(0xFFF2B8B5)
)

@Composable
fun YuniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = YuniTypography,
        content = content
    )
}
