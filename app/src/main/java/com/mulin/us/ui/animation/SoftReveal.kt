package com.mulin.us.ui.animation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 卡片柔和出现：220ms，遵守 Reduce Motion 与页面生命周期 */
@Composable
fun SoftReveal(
    visible: Boolean,
    modifier: Modifier = Modifier,
    delayMillis: Int = 0,
    content: @Composable () -> Unit
) {
    val motion = rememberStoryMotionEnabled()
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = if (motion) fadeIn(
            animationSpec = tween(220, delayMillis = delayMillis)
        ) + slideInVertically(
            animationSpec = tween(220, delayMillis = delayMillis),
            initialOffsetY = { it / 8 }
        ) else fadeIn(animationSpec = tween(0))
    ) {
        content()
    }
}
