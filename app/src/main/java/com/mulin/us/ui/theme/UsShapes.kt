package com.mulin.us.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object UsRadius {
    val xs = 8.dp
    val small = 16.dp
    val card = 24.dp
    val dialog = 24.dp
    val pill = 999.dp

    // 旧别名，兼容既有调用点
    val sm = small
    val md = small
    val lg = card
}

/**
 * V3 圆角：大卡 24dp、小卡 16dp、弹窗 24dp。
 * extraLarge 不再是 999dp，否则 AlertDialog 会变成巨大椭圆。
 */
val UsShapes = Shapes(
    extraSmall = RoundedCornerShape(UsRadius.xs),
    small = RoundedCornerShape(UsRadius.small),
    medium = RoundedCornerShape(UsRadius.small),
    large = RoundedCornerShape(UsRadius.card),
    extraLarge = RoundedCornerShape(UsRadius.dialog)
)
