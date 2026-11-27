package com.mulin.us.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * 绘本排版：正文用系统字体保证可读与系统缩放；
 * 装饰性短标题使用同一种系统 Serif 变体，不打包来源不明字体。
 */
val UsBrand = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 34.sp,
    lineHeight = 42.sp,
    fontWeight = FontWeight.Bold
)

val UsDayNumber = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 84.sp,
    lineHeight = 92.sp,
    fontWeight = FontWeight.Bold
)

val UsDisplay = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 26.sp,
    lineHeight = 34.sp,
    fontWeight = FontWeight.SemiBold
)

val UsTitle = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 22.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.SemiBold
)

val UsCardTitle = TextStyle(
    fontSize = 17.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight.Medium
)

val UsBody = TextStyle(
    fontSize = 15.sp,
    lineHeight = 23.sp,
    fontWeight = FontWeight.Normal
)

val UsCaption = TextStyle(
    fontSize = 12.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight.Normal
)

val UsNumber = TextStyle(
    fontFamily = FontFamily.Serif,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    fontWeight = FontWeight.Bold
)

val UsTypography = Typography(
    displaySmall = UsDisplay,
    headlineSmall = UsTitle,
    titleLarge = UsTitle,
    titleMedium = UsCardTitle,
    bodyLarge = UsBody,
    bodyMedium = UsBody,
    bodySmall = UsCaption,
    labelMedium = UsCaption
)
