package com.mulin.us.ui.animation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 信封拆封视觉：只由已获得的真实状态 [opened] 驱动。
 * 不加载、不预览任何受保护正文，也不改变业务状态。
 */
@Composable
fun EnvelopeOpen(
    opened: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp
) {
    val motion = rememberStoryMotionEnabled()
    val rotation by animateFloatAsState(
        targetValue = if (opened) -32f else 0f,
        animationSpec = tween(if (motion) 560 else 0),
        label = "envelope-flap"
    )
    val scale by animateFloatAsState(
        targetValue = if (opened) 1.04f else 1f,
        animationSpec = tween(if (motion) 420 else 0),
        label = "envelope-scale"
    )
    val wax by animateColorAsState(
        targetValue = if (opened) Color.Transparent else MaterialTheme.colorScheme.primary,
        animationSpec = tween(if (motion) 360 else 0),
        label = "wax-fade"
    )

    Box(
        modifier = modifier.size(size).scale(scale),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxSize(0.9f).clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .size(width = size * 0.9f, height = size * 0.5f)
                .rotate(rotation)
                .clip(RoundedCornerShape(bottomStart = 60.dp, bottomEnd = 60.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        Box(
            Modifier.size(18.dp).clip(RoundedCornerShape(50)).background(wax)
        )
    }
}
