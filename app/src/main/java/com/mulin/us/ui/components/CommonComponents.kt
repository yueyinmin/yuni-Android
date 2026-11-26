package com.mulin.us.ui.components

import com.mulin.us.ui.theme.YuniSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mulin.us.domain.model.SyncStatus

/**
 * 同步状态徽标（一行小字）。
 *
 * 为什么单独做一个组件：
 * 六个状态如果在每个页面各写一遍 when，早晚有一天某个页面漏掉新状态导致编译报错、
 * 或者更糟 —— 用 else 兜住了，于是新状态在界面上显示成错误文案。
 * 集中在这里，编译器会强制补全所有分支。
 */
@Composable
fun SyncBadge(status: SyncStatus, modifier: Modifier = Modifier) {
    Text(
        text = "同步：" + status.shortLabel(),
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
    )
}

/**
 * 空状态提示。
 *
 * 工单要求"空状态要有引导，但不能制造强制任务感"。
 * 所以这里的文案一律是"可以做什么"，不是"你还没做什么"。
 */
@Composable
fun EmptyHint(title: String, hint: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(YuniSpacing.page),
            verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(hint, style = MaterialTheme.typography.bodySmall)
        }
    }
}

/** 一行"标签 + 值"，用于设置页/自检页 */
@Composable
fun LabeledRow(label: String, value: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Text(value, style = MaterialTheme.typography.bodySmall)
    }
}
