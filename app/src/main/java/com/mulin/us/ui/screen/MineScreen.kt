package com.mulin.us.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mulin.us.ui.YuniViewModel

/**
 * 「我的」页 = B0 的自检面板。
 *
 * 这一页是给验收用的，不是给用户用的：
 * 让它能一眼看出「数据库通不通、队列里有没有积压、云端到底配没配」。
 * B1 会把这块收进「设置 - 诊断」，普通用户平时看不到。
 */
@Composable
fun MineScreen(vm: YuniViewModel) {
    val user by vm.currentUser.collectAsState()
    val memoryCount by vm.memoryCount.collectAsState()
    val diaryCount by vm.diaryCount.collectAsState()
    val wishCount by vm.wishCount.collectAsState()
    val pending by vm.pendingCount.collectAsState()
    val lastSync by vm.lastSyncAt.collectAsState()
    val lastResult by vm.lastSyncResult.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("我的", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("本机档案", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = user?.let { "${it.nickname}（${it.id.take(12)}…）" } ?: "还没有档案",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedButton(onClick = { showProfileDialog = true }) {
                    Text(if (user == null) "创建本机档案" else "重建本机档案")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("本地数据库（Room）", style = MaterialTheme.typography.titleMedium)
                Text("回忆：$memoryCount 条", style = MaterialTheme.typography.bodySmall)
                Text("日记：$diaryCount 条", style = MaterialTheme.typography.bodySmall)
                Text("愿望：$wishCount 条", style = MaterialTheme.typography.bodySmall)
                Text(
                    "这三行有数字 = Room 建表、写入、读取都通了",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("云端与同步", style = MaterialTheme.typography.titleMedium)
                Text(vm.cloudStateText, style = MaterialTheme.typography.bodySmall)
                Text("待上传队列：$pending 条", style = MaterialTheme.typography.bodySmall)
                Text(
                    "上次同步：" + if (lastSync == 0L) "无" else formatTime(lastSync),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(lastResult, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { vm.syncNow() }) { Text("立即同步") }
                    OutlinedButton(onClick = { vm.scheduleBackgroundSync() }) { Text("排后台任务") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("安全说明", style = MaterialTheme.typography.titleMedium)
                Text(
                    "· 私密日记只存本机，永不进入上传队列\n" +
                        "· Token 用 Android Keystore 加密后落盘\n" +
                        "· 退出登录不会删除任何本地日记\n" +
                        "· 未配置环境时 App 不发任何网络请求",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }

    if (showProfileDialog) {
        var nickname by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("本机档案") },
            text = {
                Column {
                    Text(
                        "没配云端也能用：档案只存这台手机。",
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("昵称") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createLocalProfile(nickname)
                    showProfileDialog = false
                }) { Text("创建") }
            },
            dismissButton = {
                TextButton(onClick = { showProfileDialog = false }) { Text("取消") }
            }
        )
    }
}
