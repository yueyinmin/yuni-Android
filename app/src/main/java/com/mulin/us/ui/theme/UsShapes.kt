package com.mulin.us.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

object UsRadius {
    val xs = 10.dp
    val sm = 12.dp
    val md = 16.dp
    val card = 20.dp
    val lg = 22.dp
    val pill = 999.dp
}

val UsShapes = Shapes(
    extraSmall = RoundedCornerShape(UsRadius.xs),
    small = RoundedCornerShape(UsRadius.sm),
    medium = RoundedCornerShape(UsRadius.card),
    large = RoundedCornerShape(UsRadius.lg),
    extraLarge = RoundedCornerShape(UsRadius.pill)
)
