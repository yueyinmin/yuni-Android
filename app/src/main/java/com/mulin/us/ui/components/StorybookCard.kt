package com.mulin.us.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.mulin.us.ui.theme.UsRadius

/**
 * 绘本卡片：在 PaperCard 基础上加一条可配色「书脊」和柔和水彩晕染。
 * 通用组件不接收业务对象，只接收颜色与槽位。
 */
@Composable
fun StorybookCard(
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
    wash: Color = MaterialTheme.colorScheme.primaryContainer,
    showSpine: Boolean = true,
    padding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(UsRadius.card),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box {
            if (showSpine) {
                Box(
                    Modifier
                        .padding(start = 10.dp, top = 18.dp, bottom = 18.dp)
                        .width(3.dp)
                        .clip(RoundedCornerShape(UsRadius.pill))
                        .background(accent.copy(alpha = 0.55f))
                )
            }
            Box(
                Modifier
                    .align(Alignment.TopEnd)
                    .size(84.dp)
                    .clip(RoundedCornerShape(50))
                    .background(wash.copy(alpha = 0.35f))
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = if (showSpine) 26.dp else padding, end = padding, top = padding, bottom = padding),
                content = content
            )
        }
    }
}
