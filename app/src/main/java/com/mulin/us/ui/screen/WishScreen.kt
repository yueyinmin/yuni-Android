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
import com.mulin.us.ui.YuniViewModel

/** 愿望清单（B0 骨架） */
@Composable
fun WishScreen(vm: YuniViewModel) {
    val list by vm.wishes.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(20.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("愿望清单", style = MaterialTheme.typography.headlineSmall)
                Text("共 ${list.size} 条", style = MaterialTheme.typography.bodySmall)
            }
            Button(onClick = { showAdd = true }) { Text("加一条") }
        }

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 12.dp)) {
            items(list, key = { it.localId }) { wish ->
                Card(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = (if (wish.doneAt != null) "✓ " else "○ ") + wish.title,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "同步：" + wish.syncStatus.shortLabel(),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
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
