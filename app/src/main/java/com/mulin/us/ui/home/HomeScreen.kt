package com.mulin.us.ui.home

import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.theme.YuniSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.formatTime

/**
 * 首页（Tab 1）。
 *
 * B0 只展示三块：相恋天数占位 / 同步状态 / 本地模式说明。
 * 真正的双人资料卡、纪念日、倒计时、今日问答都在 B2 —— B0.1 不提前做业务。
 */
@Composable
fun HomeScreen(vm: YuniViewModel, modifier: Modifier = Modifier) {
    val user by vm.currentUser.collectAsState()
    val pending by vm.pendingCount.collectAsState()
    val lastSync by vm.lastSyncAt.collectAsState()
    val lastResult by vm.lastSyncResult.collectAsState()
    val daysTogether by vm.daysTogether.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(YuniSpacing.page),
        verticalArrangement = Arrangement.spacedBy(YuniSpacing.lg)
    ) {
        Text("予你", style = MaterialTheme.typography.displaySmall)
        Text("Us · 两个人的私密空间", style = MaterialTheme.typography.bodyMedium)

        YuniCard {
            Column(modifier = Modifier.padding(YuniSpacing.page)) {
                Text("相恋", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = daysTogether?.let { "第 $it 天" } ?: "还没设置开始日",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(YuniSpacing.sm))
                if (daysTogether == null) {
                    OutlinedButton(onClick = {
                        // 默认用今天；精确日期可在「我们」页改
                        vm.setRelationshipStart(java.time.LocalDate.now().toEpochDay())
                    }) { Text("从今天开始记录") }
                } else {
                    Text("纪念日与见面倒计时见「我们」页", style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        YuniCard {
            Column(modifier = Modifier.padding(YuniSpacing.page), verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                Text("同步状态", style = MaterialTheme.typography.titleMedium)
                Text("云端：${vm.cloudStateText}", style = MaterialTheme.typography.bodySmall)
                Text("待上传：$pending 条", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "最近同步：" + if (lastSync == 0L) "还没同步过" else formatTime(lastSync),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(lastResult, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(YuniSpacing.xs))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = { vm.syncNow() }) { Text("立即同步") }
                    OutlinedButton(onClick = { vm.scheduleBackgroundSync() }) { Text("排队后台同步") }
                }
            }
        }

        if (!vm.cloudConfigured) {
            YuniCard {
                Column(modifier = Modifier.padding(YuniSpacing.page), verticalArrangement = Arrangement.spacedBy(YuniSpacing.xs)) {
                    Text("当前是本地模式", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "没有配置 CloudBase 环境：所有数据只存在这台手机上，" +
                            "不会发出任何网络请求，也不会消耗任何云资源点（费用为 0）。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}
