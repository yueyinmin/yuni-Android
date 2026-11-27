package com.mulin.us.ui.home.components

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 首页专属 Token：只供 Home 使用，避免影响其他四个 Tab。 */
object HomeTokens {
    val Background = Color(0xFFFFF8F1)
    val Surface = Color(0xFFFFFEFc)
    val Primary = Color(0xFFE9A0A5)
    val PrimaryContainer = Color(0xFFFBE9E8)
    val Secondary = Color(0xFFDCCBDD)
    val Accent = Color(0xFFE7C878)
    val TextPrimary = Color(0xFF51464A)
    val TextSecondary = Color(0xFF93827E)
    val Divider = Color(0xFFF1E9E4)

    val CardSurface = Color(0xEDFFFEFC)
    val CardBorder = Color(0xE6FFFFFF)
    val AccentRose = Primary
    val PillBlush = PrimaryContainer
    val StarGold = Accent
    val ReferenceRose = Primary

    val Apricot = Color(0xFFF3D9CA)
    val Lavender = Secondary

    val HeroShape = RoundedCornerShape(26.dp)
    val AnniversaryShape = RoundedCornerShape(26.dp)
    val InteractionShape = RoundedCornerShape(24.dp)
    val MemoriesShape = RoundedCornerShape(22.dp)
    val SmallPillShape = RoundedCornerShape(24.dp)
}
