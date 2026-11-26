package com.mulin.us.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mulin.us.ui.theme.YuniRadius
import com.mulin.us.ui.theme.YuniSpacing

/**
 * 轻记式卡片：纯白 / 大圆角 / 无边框 / 极淡阴影。
 * 全 App 的卡片统一走这里，避免各页自己写 shape/elevation 导致视觉漂移。
 */
@Composable
fun YuniCard(
    modifier: Modifier = Modifier,
    tonal: Boolean = false,
    content: @Composable ColumnScope.() -> Unit
) {
    val container = if (tonal) MaterialTheme.colorScheme.surfaceVariant else Color.White
    Card(
        modifier = modifier
            .fillMaxWidth()
            .shadow(2.dp, RoundedCornerShape(YuniRadius.card), clip = true),
        shape = RoundedCornerShape(YuniRadius.card),
        colors = CardDefaults.cardColors(containerColor = container),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(content = content)
    }
}
