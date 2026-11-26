package com.mulin.us.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.delay
import com.mulin.us.ui.theme.YuniMotion

/**
 * 轻记 `AnimatedAppear.kt` 的 AppearItem 移植：
 * 列表项错开 20ms 依次淡入（最多算前 5 项，总计 ≤80ms + 150ms 淡入）。
 *
 * 关键点：只改 alpha、content 始终参与测量——不撤掉布局，
 * 否则页面高度逐帧变化，切 Tab 时会出现"卡片往上跳"的抖动。
 */
@Composable
fun YuniAppearItem(
    index: Int = 0,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        val wait = index.coerceAtMost(YuniMotion.MaxStaggerIndex) * YuniMotion.StaggerStep
        if (wait > 0) delay(wait)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(durationMillis = YuniMotion.Fast, easing = YuniMotion.decelerate),
        label = "appearAlpha"
    )
    Box(modifier = modifier.graphicsLayer { this.alpha = alpha }) {
        content()
    }
}
