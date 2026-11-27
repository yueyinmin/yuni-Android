package com.mulin.us.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 予你 · Us 绘本水彩视觉 Token（V3）。
 * 所有页面统一从此处取色，禁止页面内另定义相近色。
 */
object UsColor {
    // V3 基础色
    val Background = Color(0xFFFFF9EF)
    val Surface = Color(0xFFFFFEFA)
    val Primary = Color(0xFFE9A0A5)
    val PrimaryContainer = Color(0xFFFBE9E7)
    val Secondary = Color(0xFFDCD2E8)
    val Accent = Color(0xFFD9B96E)
    val TextPrimary = Color(0xFF514541)
    val TextSecondary = Color(0xFF938780)
    val Divider = Color(0xFFF0E8DF)

    // V3 素材包 Token（AND-01）
    val BgCreamTop = Color(0xFFFDF4EE)
    val BgBlushMid = Color(0xFFFBEAE5)
    val BgRoseBottom = Color(0xFFF9E4E0)
    val CardSurface = Color(0xDBFFFFFF)
    val CardBorder = Color(0xE6FFFFFF)
    val AccentRose = Color(0xFFE27D8E)
    val PillBlush = Color(0xFFFCE4E7)
    val StarGold = Color(0xFFF5C76B)

    // V2.2 别名：保留旧调用点，值对齐 V3
    val PaperWhite = Background
    val CoralPink = Primary
    val MistBlue = Color(0xFFB9C9DA)
    val Lavender = Secondary
    val SageGreen = Color(0xFFB7C6A4)
    val CaramelBrown = Color(0xFFB98569)
    val DeepBrown = TextPrimary
    val MutedBrown = TextSecondary
    val PaperBeige = Divider

    val RoseDeep = Color(0xFFC86E72)
    val RosePale = PrimaryContainer
    val BluePale = Color(0xFFE7EFF6)
    val LavenderPale = Color(0xFFF1EBF7)
    val SagePale = Color(0xFFEAF0E3)

    val Success = SageGreen
    val Warning = Color(0xFFC99A52)
    val Error = Color(0xFFC45C5C)

    val NightPaper = Color(0xFF241F22)
    val NightSurface = Color(0xFF302930)
    val NightInk = Color(0xFFF5EDE6)
}
