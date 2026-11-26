package com.mulin.us.ui.animation

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** 低频花瓣飘落 + 星光闪烁。循环动画在页面不可见/省电/减少动画时完全停掉。 */
@Composable
fun PetalFall(
    modifier: Modifier = Modifier,
    petals: Int = 3
) {
    val motion = rememberStoryMotionEnabled()
    if (!motion) return

    val transition = rememberInfiniteTransition(label = "storybook-petals")
    repeat(minOf(petals, 4)) { index ->
        val progress by transition.animateFloat(
            initialValue = -0.1f,
            targetValue = 1.1f,
            animationSpec = infiniteRepeatable(
                animation = tween(1000 + index * 120),
                repeatMode = RepeatMode.Restart
            ),
            label = "petal-$index"
        )
        val alpha by transition.animateFloat(
            initialValue = 0f,
            targetValue = 0.55f,
            animationSpec = infiniteRepeatable(
                animation = tween(900 + index * 100),
                repeatMode = RepeatMode.Reverse
            ),
            label = "petal-alpha-$index"
        )
        Box(
            modifier = modifier
                .fillMaxSize()
                .offset(y = (progress * 220).dp)
                .alpha(alpha),
            contentAlignment = androidx.compose.ui.Alignment.TopCenter
        ) {
            Box(
                Modifier
                    .offset(x = ((index * 47) % 160).dp)
                    .size(if (index % 2 == 0) 7.dp else 5.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEFA9A4))
            )
        }
    }
}

/** 星光闪烁：低频、低亮度，避免高频闪烁影响阅读 */
@Composable
fun StarTwinkle(
    modifier: Modifier = Modifier,
    starColor: Color = Color(0xFFD8B45B)
) {
    val motion = rememberStoryMotionEnabled()
    val transition = rememberInfiniteTransition(label = "storybook-stars")
    val glow by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(1600),
            repeatMode = RepeatMode.Reverse
        ),
        label = "star-glow"
    )
    Box(
        modifier = modifier
            .size(6.dp)
            .clip(CircleShape)
            .background(starColor)
            .alpha(if (motion) glow else 0.5f)
            .scale(if (motion) glow else 1f)
    )
}
