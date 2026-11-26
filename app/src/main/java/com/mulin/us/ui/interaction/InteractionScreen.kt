package com.mulin.us.ui.interaction

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 互动（Tab 3）—— B0.1 只立骨架，不做业务。
 *
 * 为什么现在就建这一页而不是等 B5：
 * 五 Tab 是信息架构，B0.1 的任务就是把架构立起来并让每个入口可达。
 * 如果这页留空（不存在），用户在 B1~B4 期间会以为"互动没了"。
 *
 * 但这一页**不放任何假按钮**：不写"点击开始问答"这种点下去没反应的东西。
 * 只写清楚现在是什么状态、下一步会有什么。这是工单"不把未实现描述成已实现"的要求。
 */
@Composable
fun InteractionScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("互动", style = MaterialTheme.typography.headlineSmall)
        Text("两个人之间的往来", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("这里会有什么", style = MaterialTheme.typography.titleMedium)
                Text(
                    "· 每日双人问答：双方都答完才揭晓\n" +
                        "· 回忆下的评论与回应\n" +
                        "· 悄悄话（短消息 + 已读状态）\n" +
                        "· 未读中心",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("当前状态", style = MaterialTheme.typography.titleMedium)
                Text(
                    "B0.1 只完成导航结构，互动功能在 B5 实施。\n" +
                        "这一页现在没有可点的功能按钮 —— 不是坏了，是还没做。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("一个必须知道的前提", style = MaterialTheme.typography.titleMedium)
                Text(
                    "每日问答的「双方提交后才揭晓」不能靠客户端判断，必须由服务端控制。\n" +
                        "所以 B5 强依赖 B1 的账号与空间、以及 B0 的 RLS 验证通过。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
