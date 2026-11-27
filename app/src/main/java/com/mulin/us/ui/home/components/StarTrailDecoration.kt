package com.mulin.us.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.mulin.us.ui.theme.UsColor

/** 星轨与四角星装饰：代码绘制，不打包位图（AND-03 / AND-05）。 */
@Composable
fun StarTrailDecoration(
    modifier: Modifier = Modifier,
    color: Color = UsColor.StarGold,
    showStars: Boolean = true
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        // 参考图的金色椭圆星轨环绕「予你」
        drawArc(
            color = color.copy(alpha = 0.55f),
            startAngle = 205f,
            sweepAngle = 320f,
            useCenter = false,
            topLeft = Offset(w * 0.02f, h * 0.04f),
            size = androidx.compose.ui.geometry.Size(w * 0.46f, h * 0.92f),
            style = Stroke(width = 1.8f)
        )
        val trail = Path().apply {
            moveTo(w * 0.50f, h * 0.30f)
            cubicTo(w * 0.64f, h * 0.16f, w * 0.82f, h * 0.18f, w * 0.96f, h * 0.42f)
        }
        drawPath(trail, color.copy(alpha = 0.35f), style = Stroke(width = 1.2f))
        if (showStars) {
            fourStar(Offset(w * 0.30f, h * 0.42f), 5f, color)
            fourStar(Offset(w * 0.62f, h * 0.24f), 4f, color.copy(alpha = 0.85f))
            fourStar(Offset(w * 0.86f, h * 0.66f), 3f, color.copy(alpha = 0.7f))
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.fourStar(
    center: Offset,
    radius: Float,
    color: Color
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x + radius * 0.18f, center.y - radius * 0.18f, center.x + radius, center.y)
        quadraticTo(center.x + radius * 0.18f, center.y + radius * 0.18f, center.x, center.y + radius)
        quadraticTo(center.x - radius * 0.18f, center.y + radius * 0.18f, center.x - radius, center.y)
        quadraticTo(center.x - radius * 0.18f, center.y - radius * 0.18f, center.x, center.y - radius)
        close()
    }
    drawPath(path, color)
}
