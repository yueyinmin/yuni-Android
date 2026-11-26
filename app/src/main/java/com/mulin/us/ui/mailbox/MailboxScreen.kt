package com.mulin.us.ui.mailbox

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
 * 信箱（Tab 4）—— B0.1 只立骨架，不做业务。
 *
 * 与互动页同样的原则：入口必须存在（否则用户以为功能没了），
 * 但不放假按钮（点了没反应的按钮比没有按钮更让人困惑）。
 *
 * 这一页最关键的一条写在下面的"解锁规则"里：
 * 未来的信必须在服务端用可信时间判断，客户端改系统时间不能提前解锁。
 * 这条决定了 B6 不能只靠 Android 本地定时器实现。
 */
@Composable
fun MailboxScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("信箱", style = MaterialTheme.typography.headlineSmall)
        Text("写给以后的彼此", style = MaterialTheme.typography.bodyMedium)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("这里会有什么", style = MaterialTheme.typography.titleMedium)
                Text(
                    "· 写给未来的信（设定解锁日期）\n" +
                        "· 时间胶囊（写给未来的自己 / 对方 / 两个人）\n" +
                        "· 已收到的信与已解锁的信",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("解锁规则（B6 的核心约束）", style = MaterialTheme.typography.titleMedium)
                Text(
                    "1. 未到解锁时间，服务端不返回正文 —— 不是客户端藏起来\n" +
                        "2. 用服务端时间判断，改手机时间无效\n" +
                        "3. 客户端改代码也拿不到，因为正文根本没有下发\n" +
                        "4. 正文不以明文存云端",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("当前状态", style = MaterialTheme.typography.titleMedium)
                Text(
                    "B0.1 只完成导航结构，写信与解锁在 B6 实施。\n" +
                        "本地已有 letter 表骨架，但故意没有正文字段 —— 正文不该落在客户端。",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
