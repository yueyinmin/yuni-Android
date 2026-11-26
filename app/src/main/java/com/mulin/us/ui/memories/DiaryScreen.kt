package com.mulin.us.ui.memories

import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.theme.YuniSpacing
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.SubPageScaffold
import com.mulin.us.ui.components.SyncBadge

/**
 * 日记（回忆 Tab 下的二级页面，B0.1 T04 从底部 Tab 迁移至此）。
 *
 * 功能与迁移前完全一致，只多了顶部返回：
 * 列表、全部/只看私密筛选、写日记弹窗、私密开关 —— 一项没删。
 *
 * 这一页最值得看的是「私密」那一档：
 * 切到私密保存后 syncStatus 永远是 LOCAL_ONLY，Outbox 里也不会出现这条。
 * 界面上写「仅本机」不是提示文案，而是真的没有上传任务。
 */
@Composable
fun DiaryScreen(
    vm: YuniViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val all by vm.diaries.collectAsState()
    var onlyPrivate by remember { mutableStateOf(false) }
    var showAdd by remember { mutableStateOf(false) }

    val list = if (onlyPrivate) all.filter { it.visibility == DiaryVisibility.PRIVATE } else all

    SubPageScaffold(title = "日记", onBack = onBack) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding).padding(YuniSpacing.page)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("日记", style = MaterialTheme.typography.headlineSmall)
                    Text("共 ${list.size} 条", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { showAdd = true }) { Text("写日记") }
            }

            Row(
                modifier = Modifier.padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm)
            ) {
                FilterChip(
                    selected = !onlyPrivate,
                    onClick = { onlyPrivate = false },
                    label = { Text("全部") }
                )
                FilterChip(
                    selected = onlyPrivate,
                    onClick = { onlyPrivate = true },
                    label = { Text("只看私密") }
                )
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                items(list, key = { it.localId }) { diary ->
                    YuniCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Column(modifier = Modifier.padding(YuniSpacing.lg)) {
                            Text(
                                text = if (diary.isPrivate) "私密" else "共同",
                                style = MaterialTheme.typography.labelMedium
                            )
                            if (!diary.title.isNullOrBlank()) {
                                Text(diary.title, style = MaterialTheme.typography.titleMedium)
                            }
                            Text(diary.content, style = MaterialTheme.typography.bodyMedium)
                            SyncBadge(diary.syncStatus)
                        }
                    }
                }
            }
        }
    }

    if (showAdd) {
        var content by remember { mutableStateOf("") }
        var isPrivate by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("写日记") },
            text = {
                Column {
                    OutlinedTextField(
                        value = content,
                        onValueChange = { content = it },
                        label = { Text("今天想记什么") }
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("仅自己可见", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "开启后只存本机，永不上传",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(checked = isPrivate, onCheckedChange = { isPrivate = it })
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addDiary(isPrivate, content)
                    showAdd = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            }
        )
    }
}
