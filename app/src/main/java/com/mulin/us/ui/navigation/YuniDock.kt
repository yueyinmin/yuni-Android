package com.mulin.us.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.theme.YuniMotion
import com.mulin.us.ui.theme.YuniRadius
import com.mulin.us.ui.theme.YuniSpacing

/**
 * 轻记式悬浮底栏（Dock），动效照搬轻记 v1.1.3：
 *
 * 1. **共享胶囊指示器**：不是每个 Tab 自己画背景（会瞬现/瞬失、转场时出现两条胶囊），
 *    而是一条浅胭脂胶囊垫在底层，切换时用 spring(0.85, 900) 从旧槽位滑到新槽位；
 * 2. **选中弹跳**：目标项图标+文字放大到 1.12，用 MediumBouncy 弹簧先冲过再回落；
 *    取消选中的旧项走 150ms tween 平滑缩回，不跟着弹；
 * 3. **颜色渐变**：图标与文字颜色 150ms 渐变；
 * 4. 白圆角 Dock 浮在内容上层，页面需自己留约 88.dp 底部 padding。
 */
@Composable
fun YuniDock(
    tabs: List<YuniTabSpec>,
    currentRoute: String?,
    onTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = YuniSpacing.lg, vertical = YuniSpacing.sm),
        contentAlignment = Alignment.BottomCenter
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(8.dp, RoundedCornerShape(YuniRadius.dock))
                .clip(RoundedCornerShape(YuniRadius.dock))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = YuniSpacing.sm, vertical = YuniSpacing.sm)
        ) {
            val slotWidth = maxWidth / tabs.size.coerceAtLeast(1)
            val activeSlot = tabs.indexOfFirst { it.route == currentRoute }.coerceAtLeast(0)

            // 共享指示器：首帧直接落在当前槽位（animateFloatAsState 首次组合不打动画）
            val animatedSlot by animateFloatAsState(
                targetValue = activeSlot.toFloat(),
                animationSpec = YuniMotion.smooth(),
                label = "dock-indicator"
            )
            Box(
                modifier = Modifier
                    .offset(x = slotWidth * animatedSlot)
                    .width(slotWidth)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(YuniRadius.pill))
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )

            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    DockItem(
                        tab = tab,
                        selected = currentRoute == tab.route,
                        onClick = { onTab(tab.route) }
                    )
                }
            }
        }
    }
}

@Composable
private fun RowScope.DockItem(tab: YuniTabSpec, selected: Boolean, onClick: () -> Unit) {
    // 选中弹跳 1.12；取消选中用 150ms tween 平滑缩回（轻记 barfx 规范）
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = if (selected) YuniMotion.bouncy() else tween(YuniMotion.Fast),
        label = "dock-bounce"
    )
    val tint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = YuniMotion.fast(),
        label = "dock-tint"
    )

    Column(
        modifier = Modifier
            .weight(1f)
            .fillMaxHeight()
            .clickable(onClick = onClick)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = tint,
            modifier = Modifier.size(22.dp)
        )
        androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
        Text(
            text = tab.label,
            color = tint,
            fontSize = 11.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold
            else androidx.compose.ui.text.font.FontWeight.Normal
        )
    }
}
