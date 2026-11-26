package com.mulin.us.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.theme.YuniRadius
import com.mulin.us.ui.theme.YuniSpacing

/**
 * 轻记式悬浮底栏（Dock）：
 * - 白色圆角长条，左右/底部留间距，浮在内容上层
 * - 选中项是浅胭脂胶囊（不是整条变色）
 * - 用 Material 内置图标，不再用汉字占位
 * - 页面内容需自己给底部留 padding（约 88.dp），否则会被 Dock 挡住
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(8.dp, RoundedCornerShape(YuniRadius.dock))
                .clip(RoundedCornerShape(YuniRadius.dock))
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = YuniSpacing.sm, vertical = YuniSpacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
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

@Composable
private fun DockItem(tab: YuniTabSpec, selected: Boolean, onClick: () -> Unit) {
    val capsule by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surface,
        label = "dock-capsule"
    )
    val content by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "dock-content"
    )
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(YuniRadius.pill))
            .background(capsule)
            .clickable(onClick = onClick)
            .padding(horizontal = YuniSpacing.md, vertical = YuniSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.label,
            tint = content
        )
        Spacer(Modifier.width(YuniSpacing.xs))
        Text(
            text = tab.label,
            color = content,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
    }
}
