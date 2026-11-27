package com.mulin.us.ui.interaction

import com.mulin.us.ui.components.YuniCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mulin.us.domain.model.DailyQuestion
import com.mulin.us.domain.model.Message
import com.mulin.us.ui.theme.YuniDockInset
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.theme.UsSpacing

/**
 * 互动（Tab 3）—— S5 真功能：每日问答 + 悄悄话。
 *
 * 问答揭晓由服务端控制：未揭晓时对方答案根本不下发（不是 UI 藏起来）。
 */
@Composable
fun InteractionScreen(vm: YuniViewModel, modifier: Modifier = Modifier) {
    var tab by rememberSaveable { mutableStateOf(0) } // 0=问答 1=悄悄话；Tab 切换后恢复

    Column(
        modifier = modifier.fillMaxSize().padding(UsSpacing.page),
        verticalArrangement = Arrangement.spacedBy(UsSpacing.cardGap)
    ) {
        Text("互动", style = MaterialTheme.typography.headlineSmall)

        Row(horizontalArrangement = Arrangement.spacedBy(UsSpacing.sm)) {
            FilterChip(selected = tab == 0, onClick = { tab = 0 }, label = { Text("每日问答") })
            FilterChip(selected = tab == 1, onClick = { tab = 1 }, label = { Text("悄悄话") })
        }

        if (tab == 0) QuestionTab(vm) else MessageTab(vm)
    }
}

@Composable
private fun QuestionTab(vm: YuniViewModel) {
    val question by vm.todayQuestion.collectAsState()
    var showAnswer by remember { mutableStateOf(false) }
    var answer by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(UsSpacing.cardGap)
    ) {
        OutlinedButton(onClick = { vm.loadTodayQuestion() }, modifier = Modifier.fillMaxWidth()) {
            Text("刷新今日问题")
        }

        if (question == null) {
            YuniCard {
                Text(
                    "还没有加载问题。点上面「刷新今日问题」，或先确认已登录云端账号。",
                    modifier = Modifier.padding(UsSpacing.page),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            YuniCard {
                Column(modifier = Modifier.padding(UsSpacing.page), verticalArrangement = Arrangement.spacedBy(UsSpacing.sm)) {
                    Text("今天的问题", style = MaterialTheme.typography.labelMedium)
                    Text(question!!.content, style = MaterialTheme.typography.titleMedium)

                    Text(
                        "我的回答：${question!!.myAnswer ?: "还没答"}",
                        style = MaterialTheme.typography.bodyMedium
                    )

                    if (question!!.revealed) {
                        Text(
                            "对方的回答：${question!!.partnerAnswer ?: "（空）"}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        Text(
                            "对方还没答，答案揭晓前看不到",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    if (question!!.myAnswer == null) {
                        Button(onClick = { showAnswer = true }, modifier = Modifier.fillMaxWidth()) {
                            Text("写下我的答案")
                        }
                    }
                }
            }
        }

        YuniCard {
            Text(
                "规则：双方都提交后才揭晓；服务端控制，改手机时间或拆包都拿不到对方答案。",
                modifier = Modifier.padding(UsSpacing.page),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    if (showAnswer) {
        AlertDialog(
            onDismissRequest = { showAnswer = false },
            title = { Text("我的答案") },
            text = {
                OutlinedTextField(
                    value = answer, onValueChange = { answer = it },
                    label = { Text("写下想说的话") }, minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.answerToday(answer)
                    answer = ""
                    showAnswer = false
                }) { Text("提交") }
            },
            dismissButton = { TextButton(onClick = { showAnswer = false }) { Text("取消") } }
        )
        // 悬浮 Dock 页尾让位
        Spacer(Modifier.height(YuniDockInset.TailSpacer))

    }
}

@Composable
private fun MessageTab(vm: YuniViewModel) {
    val messages by vm.messages.collectAsState()
    val unread by vm.unreadCount.collectAsState()
    var showCompose by remember { mutableStateOf(false) }
    var content by remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(UsSpacing.sm)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(UsSpacing.sm)) {
            OutlinedButton(onClick = { vm.loadMessages() }) { Text("刷新") }
            Button(onClick = { showCompose = true }) { Text("写悄悄话") }
        }
        if (unread > 0) {
            Text("有 $unread 条未读", style = MaterialTheme.typography.labelMedium)
        }

        if (messages.isEmpty()) {
            YuniCard {
                Text(
                    "还没有消息。点「写悄悄话」给对方发一条。",
                    modifier = Modifier.padding(UsSpacing.page),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(UsSpacing.sm)) {
                items(messages, key = { it.cloudId ?: it.localId }) { msg ->
                    MessageCard(msg, vm)
                }
            }
        }
    }

    if (showCompose) {
        AlertDialog(
            onDismissRequest = { showCompose = false },
            title = { Text("悄悄话") },
            text = {
                OutlinedTextField(
                    value = content, onValueChange = { content = it },
                    label = { Text("想说的话") }, minLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.sendMessage(content)
                    content = ""
                    showCompose = false
                }) { Text("发送") }
            },
            dismissButton = { TextButton(onClick = { showCompose = false }) { Text("取消") } }
        )
    }
}

@Composable
private fun MessageCard(msg: Message, vm: YuniViewModel) {
    YuniCard {
        Column(modifier = Modifier.padding(UsSpacing.md), verticalArrangement = Arrangement.spacedBy(UsSpacing.xs)) {
            Text(
                if (msg.senderId == "local-me" || msg.readAt == null) "对方" else "我",
                style = MaterialTheme.typography.labelSmall
            )
            Text(msg.content, style = MaterialTheme.typography.bodyMedium)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (msg.readAt == null) {
                    TextButton(onClick = { msg.cloudId?.let { vm.markMessageRead(it) } }) {
                        Text("标已读")
                    }
                }
            }
        }
        // 悬浮 Dock 页尾让位
        Spacer(Modifier.height(YuniDockInset.TailSpacer))

    }
}
