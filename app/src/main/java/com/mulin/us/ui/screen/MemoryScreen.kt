package com.mulin.us.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
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
import com.mulin.us.domain.model.SyncStatus
import com.mulin.us.ui.YuniViewModel

/**
 * 共同回忆（B0 骨架）。
 * 能写、能读、能看同步状态 —— 这三个能跑通，说明 Room + Repository + Outbox 链路是通的。
 * 照片、时间轴、详情页在 B5。
 */
@Composable
fun MemoryScreen(vm: YuniViewModel) {
    val list by vm.memories.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("共同回忆", style = MaterialTheme.typography.headlineSmall)
                Text("共 ${list.size} 条", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { showAdd = true }) { Text("写一条") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(list, key = { it.localId }) { memory ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(memory.title, style = MaterialTheme.typography.titleMedium)
                        Text(memory.content, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = memory.syncStatus.name,
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        var content by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新的回忆") },
            text = {
                Column {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("标题") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("内容") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addMemory(title, content)
                    showAdd = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            }
        )
    }
}

/** 同步状态的中文短标，列表里用 */
internal fun SyncStatus.shortLabel(): String = when (this) {
    SyncStatus.LOCAL_ONLY -> "仅本机"
    SyncStatus.PENDING_UPLOAD -> "待上传"
    SyncStatus.SYNCED -> "已同步"
    SyncStatus.PENDING_DELETE -> "待删除"
    SyncStatus.CONFLICT -> "冲突"
    SyncStatus.FAILED -> "失败"
}
