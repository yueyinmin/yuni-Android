package com.mulin.us.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 设计 token（路线图 1.6 补齐）。
 *
 * 之前每个页面各自写 padding(20.dp)、RoundedCornerShape(12.dp)、
 * MaterialTheme.typography.bodySmall —— 改一个字号要全工程搜。
 * 现在间距/圆角/字号各只有一处定义。
 */

object YuniSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val card = 18.dp
    val page = 20.dp
    val xl = 24.dp
    val block = 14.dp
    val xxl = 32.dp
}

object YuniRadius {
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    /** 轻记式大圆角卡片 */
    val card = 20.dp
    val dock = 28.dp
    val pill = 999.dp
}

/** Material 组件统一圆角：卡片/对话框/按钮都用大圆角 */
val YuniShapes = Shapes(
    extraSmall = RoundedCornerShape(YuniRadius.sm),
    small = RoundedCornerShape(YuniRadius.md),
    medium = RoundedCornerShape(YuniRadius.card),
    large = RoundedCornerShape(YuniRadius.card),
    extraLarge = RoundedCornerShape(YuniRadius.dock)
)

/** 基于 Material3 默认排版，只覆盖需要统一的三处，其余沿用系统 */
val YuniTypography = Typography(
    displaySmall = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Medium, lineHeight = 44.sp),
    headlineSmall = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Medium, lineHeight = 32.sp),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
)
