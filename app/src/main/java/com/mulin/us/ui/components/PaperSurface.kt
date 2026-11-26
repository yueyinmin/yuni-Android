package com.mulin.us.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mulin.us.ui.visual.PaperTexture

/** 全页纸张底：统一背景色 + 水彩纸张纹理，带低版本静态降级 */
@Composable
fun PaperSurface(
    modifier: Modifier = Modifier,
    textureIntensity: Float = 0.55f,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        PaperTexture.Apply(intensity = textureIntensity, content = content)
    }
}
