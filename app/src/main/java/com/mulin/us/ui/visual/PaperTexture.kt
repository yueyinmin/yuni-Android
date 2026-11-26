package com.mulin.us.ui.visual

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.mulin.us.R
import com.mulin.us.ui.theme.UsColor
import io.androidpoet.mirage.paperTexture

/**
 * 纸张纹理适配器。业务页不直接依赖 Mirage。
 *
 * API 33+：使用 Mirage 的 AGSL paperTexture（Apache-2.0）。
 * API 26~32：降级为打包的静态水彩纸纹理 WebP，无网络依赖。
 */
object PaperTexture {

    @Composable
    fun Apply(
        modifier: Modifier = Modifier,
        intensity: Float = 0.55f,
        content: @Composable () -> Unit
    ) {
        Box(
            modifier = modifier.fillMaxSize().then(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Modifier.paperTexture(
                        colorFront = UsColor.PaperWhite,
                        colorBack = UsColor.PaperBeige,
                        contrast = 0.18f * intensity,
                        roughness = 0.30f * intensity,
                        fiber = 0.28f * intensity,
                        crumples = 0.12f * intensity,
                        folds = 0.10f * intensity,
                        drops = 0f
                    )
                } else Modifier
            )
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Image(
                    painter = painterResource(R.drawable.ic_launcher_background),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize().alpha(0.22f * intensity),
                    contentScale = ContentScale.Crop
                )
            }
            content()
        }
    }
}
