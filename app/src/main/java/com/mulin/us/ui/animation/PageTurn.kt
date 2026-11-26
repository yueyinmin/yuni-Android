package com.mulin.us.ui.animation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 绘本翻页：由真实页面状态 key 驱动，不额外请求数据 */
@Composable
fun <T> PageTurn(
    stateKey: T,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit
) {
    val motion = rememberStoryMotionEnabled()
    AnimatedContent(
        targetState = stateKey,
        modifier = modifier,
        transitionSpec = {
            if (motion) {
                (slideInHorizontally(
                    animationSpec = tween(320),
                    initialOffsetX = { it / 10 }
                ) + fadeIn(tween(260))) togetherWith
                    (slideOutHorizontally(
                        animationSpec = tween(320),
                        targetOffsetX = { -it / 10 }
                    ) + fadeOut(tween(220)))
            } else {
                fadeIn(tween(0)) togetherWith fadeOut(tween(0))
            }
        },
        label = "storybook-page-turn"
    ) { target -> content(target) }
}
