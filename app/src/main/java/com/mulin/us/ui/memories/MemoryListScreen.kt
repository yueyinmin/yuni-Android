package com.mulin.us.ui.memories

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
import com.mulin.us.ui.components.SyncBadge

/**
 * 回忆（Tab 2）：共同回忆列表 + 日记入口。
 *
 * 结构调整（B0.1 T04）：
 * 原先「日记」是底部第五个独立 Tab，按 K3 整合为**回忆下的二级页面**。
 * 理由：回忆与日记都是"记录发生过的事"，同属记录类，平级放底部反而分散。
 *
 * B0 只有列表与新增；照片、时间轴、详情页在 B3。
 */
@Composable
fun MemoryListScreen(
    vm: YuniViewModel,
    onOpenDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    val list by vm.memories.collectAsState()
    val diaryCount by vm.diaryCount.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxSize().padding(20.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("回忆", style = MaterialTheme.typography.headlineSmall)
                Text("共 ${list.size} 条", style = MaterialTheme.typography.bodySmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenDiary) { Text("日记") }
                Button(onClick = { showAdd = true }) { Text("写一条") }
            }
        }

        // 日记入口除右上角按钮外，正文区再给一个带条数的入口。
        // 工单 7.3 第 6 条要求：不能因为减少 Tab 就把原功能藏到难以发现的位置。
        OutlinedButton(
            onClick = onOpenDiary,
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
        ) {
            Text("日记本（$diaryCount 条）· 私密与共同")
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(list, key = { it.localId }) { memory ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(memory.title, style = MaterialTheme.typography.titleMedium)
                        Text(memory.content, style = MaterialTheme.typography.bodyMedium)
                        SyncBadge(memory.syncStatus)
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
