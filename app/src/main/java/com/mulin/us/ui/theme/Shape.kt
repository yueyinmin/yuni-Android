package com.mulin.us.ui.theme

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection

/** Hero 上下双波浪异形裁切（AND-04）。波幅与圆角随宽度缩放，窄屏不变形。 */
val YuniWaveShape = object : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path().apply {
            val w = size.width
            val h = size.height
            val wave = (12f * (w / 360f)).coerceAtLeast(1f)
            val corner = (24f * (w / 360f)).coerceAtLeast(1f)

            moveTo(0f, wave + corner)
            quadraticTo(0f, wave, corner, wave * 0.6f)
            cubicTo(w * 0.28f, -wave * 0.4f, w * 0.65f, wave * 2.2f, w - corner, wave * 0.8f)
            quadraticTo(w, wave * 0.6f, w, wave + corner)

            lineTo(w, h - wave - corner)
            quadraticTo(w, h - wave, w - corner, h - wave * 0.6f)
            cubicTo(w * 0.68f, h + wave * 0.5f, w * 0.30f, h - wave * 2.0f, corner, h - wave * 0.7f)
            quadraticTo(0f, h - wave * 0.5f, 0f, h - wave - corner)
            close()
        }
        return Outline.Generic(path)
    }
}
