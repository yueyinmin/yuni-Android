package com.mulin.us.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable

/**
 * 兼容入口：所有旧调用点继续用 YuniTheme，
 * 实际主题已切换为 V2.2 绘本水彩 UsTheme。
 */
@Composable
fun YuniTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    UsTheme(darkTheme = darkTheme, content = content)
}
