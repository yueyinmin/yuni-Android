package com.mulin.us.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.mulin.us.ui.navigation.YuniDock
import com.mulin.us.ui.navigation.YuniTabSpec

/**
 * 绘本底部导航统一入口。
 * 不重写动画，直接复用现有 YuniDock（共享胶囊 + 原生转场），
 * 避免重复实现导致状态与路由分叉。
 */
@Composable
fun StorybookBottomBar(
    tabs: List<YuniTabSpec>,
    currentRoute: String?,
    onTab: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    YuniDock(tabs = tabs, currentRoute = currentRoute, onTab = onTab, modifier = modifier)
}
