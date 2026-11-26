package com.mulin.us.ui.mailbox

import com.mulin.us.ui.components.YuniCard
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
import com.mulin.us.domain.model.Letter
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.theme.YuniSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * 信箱（Tab 4）—— S6 真功能：写给未来的信 / 时间胶囊。
 *
 * 解锁由服务端用可信时间判断；未到时间正文根本不下发，
 * 客户端改系统时间或拆包都拿不到。
 */
@Composable
fun MailboxScreen(vm: YuniViewModel, modifier: Modifier = Modifier) {
    val letters by vm.letters.collectAsState()
    val opened by vm.openedLetter.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var isCapsule by remember { mutableStateOf(false) }

    Column(
        modifier = modifier.fillMaxSize().padding(YuniSpacing.page),
        verticalArrangement = Arrangement.spacedBy(YuniSpacing.block)
    ) {
        Text("信箱", style = MaterialTheme.typography.headlineSmall)
        Text("写给以后的彼此", style = MaterialTheme.typography.bodyMedium)

        Row(horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
            OutlinedButton(onClick = { vm.loadLetters() }) { Text("刷新") }
            Button(onClick = { isCapsule = false; showCreate = true }) { Text("写封信") }
            OutlinedButton(onClick = { isCapsule = true; showCreate = true }) { Text("时间胶囊") }
        }

        if (letters.isEmpty()) {
            YuniCard {
                Text(
                    "还没有信。写封给未来的信，设个解锁日期吧。",
                    modifier = Modifier.padding(YuniSpacing.page),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                items(letters, key = { it.cloudId ?: it.localId }) { letter ->
                    LetterCard(letter, vm)
                }
            }
        }
    }

    if (showCreate) CreateLetterDialog(vm, isCapsule) { showCreate = false }

    if (opened != null) {
        AlertDialog(
            onDismissRequest = { vm.openedLetter.value = null },
            title = { Text("信的内容") },
            text = { Text(opened!!, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = { TextButton(onClick = { vm.openedLetter.value = null }) { Text("收好") } }
        )
    }
}

@Composable
private fun LetterCard(letter: Letter, vm: YuniViewModel) {
    val date = java.time.Instant.ofEpochMilli(letter.unlockAt)
        .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
    YuniCard {
        Column(modifier = Modifier.padding(YuniSpacing.md), verticalArrangement = Arrangement.spacedBy(YuniSpacing.xs)) {
            Text(letter.title, style = MaterialTheme.typography.titleMedium)
            Text("解锁日期：$date", style = MaterialTheme.typography.bodySmall)
            Text(if (letter.senderId == "me") "我写的" else "对方写的", style = MaterialTheme.typography.labelSmall)
            when (letter.status) {
                com.mulin.us.domain.model.LetterStatus.UNLOCKED ->
                    OutlinedButton(onClick = { letter.cloudId?.let { vm.openLetter(it) } }) { Text("打开阅读") }
                else -> Text("还没到解锁时间，正文在服务端锁着", style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun CreateLetterDialog(vm: YuniViewModel, isCapsule: Boolean, onClose: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var days by remember { mutableStateOf("30") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(if (isCapsule) "时间胶囊" else "写给未来的信") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true)
                OutlinedTextField(value = content, onValueChange = { content = it }, label = { Text("想说的话") }, minLines = 4)
                OutlinedTextField(value = days, onValueChange = { days = it }, label = { Text("多少天后解锁") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val d = days.toLongOrNull() ?: 30L
                val unlock = LocalDate.now().plusDays(d).atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                vm.createLetter(title, content, unlock)
                onClose()
            }) { Text("封存") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}
