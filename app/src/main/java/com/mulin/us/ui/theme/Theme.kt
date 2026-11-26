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
 * 予你的配色 —— 沿用轻记的视觉骨架（浅灰页面底 + 纯白大圆角卡 + 悬浮底栏），
 * 主色保留予你的胭脂红（不用轻记的翠绿），这样不像轻记换皮。
 *
 * 主色 0xFFB4556A：与轻记深翠绿同饱和度的胭脂；浅胶囊 0xFFF2E4E8。
 */
private val WarmRose = Color(0xFFB4556A)
private val WarmRoseDark = Color(0xFF8C3F52)
private val RoseTint = Color(0xFFF2E4E8)      // 选中胶囊 / primaryContainer
private val PageGray = Color(0xFFF5F6F8)     // 轻记式浅灰页面底
private val Ink = Color(0xFF1F2328)          // 主文字（比原来更深，接近轻记）
private val SecondaryInk = Color(0xFF8A9099) // 副文字灰
private val NightBg = Color(0xFF141013)
private val NightSurface = Color(0xFF201C1F)

private val LightColors = lightColorScheme(
    primary = WarmRose,
    onPrimary = Color.White,
    primaryContainer = RoseTint,
    onPrimaryContainer = WarmRoseDark,
    secondary = SecondaryInk,
    onSecondary = Color.White,
    background = PageGray,
    onBackground = Ink,
    surface = Color.White,          // 卡片纯白（轻记规范）
    onSurface = Ink,
    surfaceVariant = Color(0xFFEEF0F3),
    onSurfaceVariant = SecondaryInk,
    error = Color(0xFFC5384A)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFE5A9B6),
    onPrimary = Color(0xFF4A1F29),
    primaryContainer = Color(0xFF6B3341),
    onPrimaryContainer = Color(0xFFF9DCE2),
    secondary = SecondaryInk,
    onSecondary = Color.White,
    background = NightBg,
    onBackground = Color(0xFFEDE4E6),
    surface = NightSurface,
    onSurface = Color(0xFFEDE4E6),
    surfaceVariant = Color(0xFF2E272A),
    onSurfaceVariant = Color(0xFFA8A0A4),
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
        shapes = YuniShapes,
        content = content
    )
}
