package com.mulin.us.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val UsLightColors = lightColorScheme(
    primary = UsColor.Primary,
    onPrimary = Color.White,
    primaryContainer = UsColor.PrimaryContainer,
    onPrimaryContainer = UsColor.TextPrimary,
    secondary = UsColor.Lavender,
    onSecondary = UsColor.TextPrimary,
    secondaryContainer = UsColor.LavenderPale,
    onSecondaryContainer = UsColor.TextPrimary,
    tertiary = UsColor.Accent,
    onTertiary = Color.White,
    tertiaryContainer = UsColor.PrimaryContainer,
    onTertiaryContainer = UsColor.TextPrimary,
    background = UsColor.Background,
    onBackground = UsColor.TextPrimary,
    surface = UsColor.Surface,
    onSurface = UsColor.TextPrimary,
    surfaceVariant = UsColor.Divider,
    onSurfaceVariant = UsColor.TextSecondary,
    outline = UsColor.Divider,
    error = UsColor.Error,
    onError = Color.White
)

private val UsDarkColors = darkColorScheme(
    primary = Color(0xFFF2B9B2),
    onPrimary = Color(0xFF4A2526),
    primaryContainer = Color(0xFF6D3C3E),
    onPrimaryContainer = Color(0xFFFFE9E4),
    secondary = Color(0xFFC3D5E6),
    onSecondary = Color(0xFF25313A),
    secondaryContainer = Color(0xFF354653),
    onSecondaryContainer = Color(0xFFE8F1F8),
    tertiary = Color(0xFFD6C7E6),
    onTertiary = Color(0xFF30243B),
    tertiaryContainer = Color(0xFF453653),
    onTertiaryContainer = Color(0xFFF3EAFB),
    background = UsColor.NightPaper,
    onBackground = UsColor.NightInk,
    surface = UsColor.NightSurface,
    onSurface = UsColor.NightInk,
    surfaceVariant = Color(0xFF3B3238),
    onSurfaceVariant = Color(0xFFD8C9C2),
    outline = Color(0xFF8C7E79),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

/** 绘本水彩全局主题；业务页面只依赖 MaterialTheme 与 Us* Token */
@Composable
fun UsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) UsDarkColors else UsLightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colors,
        typography = UsTypography,
        shapes = UsShapes,
        content = content
    )
}
