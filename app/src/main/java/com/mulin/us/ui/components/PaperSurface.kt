package com.mulin.us.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mulin.us.R
import com.mulin.us.ui.visual.PaperTexture

/** 全页纸张底：统一背景色 + 水彩纸张纹理，带低版本静态降级 */
@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    textureIntensity: Float = 0.55f,
    content: @Composable () -> Unit
) {
    // Insets 只在根容器处理一次，避免各页面重复 statusBarsPadding 导致起点不一致
    Box(modifier = modifier.fillMaxSize().statusBarsPadding()) {
        // 参考图采样生成的暖粉水彩底，全局统一
        Image(
            painter = painterResource(R.drawable.bg_app_soft),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        PaperTexture.Apply(intensity = textureIntensity, content = content)
    }
}
