package com.mulin.us.ui.couple

import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.theme.YuniSpacing
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
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
import com.mulin.us.ui.theme.YuniDockInset
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.SubPageScaffold
import com.mulin.us.ui.components.SyncBadge

/**
 * 愿望清单（「我们」Tab 下的二级页面，B0.1 T04 从底部 Tab 迁移至此）。
 *
 * 功能与迁移前完全一致：列表、新增、完成标记（勾选显示）、同步状态。一项没删。
 */
@Composable
fun WishScreen(
    vm: YuniViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val list by vm.wishes.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    SubPageScaffold(title = "愿望清单", onBack = onBack) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding).padding(YuniSpacing.page)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("愿望清单", style = MaterialTheme.typography.headlineSmall)
                    Text("共 ${list.size} 条", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { showAdd = true }) { Text("加一条") }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
                items(list, key = { it.localId }) { wish ->
                    YuniCard(modifier = Modifier.padding(bottom = 10.dp)) {
                        Column(modifier = Modifier.padding(YuniSpacing.lg)) {
                            Text(
                                text = (if (wish.doneAt != null) "✓ " else "○ ") + wish.title,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                SyncBadge(wish.syncStatus)
                                Row {
                                    TextButton(onClick = { vm.toggleWishDone(wish.localId, wish.doneAt == null) }) {
                                        Text(if (wish.doneAt == null) "完成" else "恢复")
                                    }
                                    TextButton(onClick = { vm.deleteWish(wish.localId) }) { Text("删除") }
                                }
                            }
                        }
                    }
                }
                    // 悬浮 Dock 页尾让位：最后一条不被 Dock 压住
        item { Spacer(Modifier.height(YuniDockInset.TailSpacer)) }
    }
        }
    }

    if (showAdd) {
        var title by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAdd = false },
            title = { Text("新的愿望") },
            text = {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("比如：一起去看海") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.addWish(title)
                    showAdd = false
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { showAdd = false }) { Text("取消") }
            }
        )
    }
}
