package com.mulin.us.ui.screen

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 首页骨架。
 * B0 只展示三块：相恋天数占位 / 同步状态 / 快捷入口。
 * 真正的纪念日、倒计时、今日问答都在 B1~B3。
 */
@Composable
fun HomeScreen(vm: YuniViewModel) {
    val user by vm.currentUser.collectAsState()
    val pending by vm.pendingCount.collectAsState()
    val lastSync by vm.lastSyncAt.collectAsState()
    val lastResult by vm.lastSyncResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("予你", style = MaterialTheme.typography.displaySmall)
        Text("Us · 两个人的私密空间", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text("相恋", style = MaterialTheme.typography.labelLarge)
                Text(
                    text = if (user == null) "还没有本机档案" else "第 1 天（示例）",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "真实天数需要先设立恋爱开始日（B1 绑定空间后才会有）",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("同步状态", style = MaterialTheme.typography.titleMedium)
                Text("云端：${vm.cloudStateText}", style = MaterialTheme.typography.bodySmall)
                Text(
                    text = "待上传：$pending 条",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "最近同步：" + if (lastSync == 0L) "还没同步过" else formatTime(lastSync),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(lastResult, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { vm.syncNow() }) { Text("立即同步") }
                    OutlinedButton(onClick = { vm.scheduleBackgroundSync() }) { Text("排队后台同步") }
                }
            }
        }

        if (!vm.cloudConfigured) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
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

internal fun formatTime(millis: Long): String {
    if (millis <= 0) return "-"
    val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    return fmt.format(Date(millis))
}
