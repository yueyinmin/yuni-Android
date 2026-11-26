package com.mulin.us.ui.couple

import com.mulin.us.ui.theme.YuniSpacing
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.LabeledRow
import com.mulin.us.ui.components.formatTime

/**
 * 我们（Tab 5）—— B0.1 T04 由原「我的」页改造而来。
 *
 * 保留原 MineScreen 的全部内容（本机档案、数据库自检、云端与同步、安全说明），
 * 只做两件事：
 * 1. 标题「我的」→「我们」（K3 命名）；
 * 2. 增加「愿望清单」二级页入口 —— 因为愿望已从底部 Tab 降级为这里的二级页，
 *    必须给一个显眼入口，否则等于把功能藏起来（工单 7.3 第 6 条）。
 *
 * 这一页同时也是 B0 的自检面板：数据库通不通、队列有没有积压、云端配没配，一眼可见。
 * B2 会把自检块收进「设置 - 诊断」，普通用户平时看不到。
 */
@Composable
fun UsScreen(
    vm: YuniViewModel,
    onOpenWishes: () -> Unit,
    onOpenMeetings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by vm.currentUser.collectAsState()
    val couple by vm.couple.collectAsState()
    val inviteCode by vm.lastInviteCode.collectAsState()
    val toast by vm.toast.collectAsState()
    val context = LocalContext.current
    val memoryCount by vm.memoryCount.collectAsState()
    val diaryCount by vm.diaryCount.collectAsState()
    val wishCount by vm.wishCount.collectAsState()
    val pending by vm.pendingCount.collectAsState()
    val lastSync by vm.lastSyncAt.collectAsState()
    val lastResult by vm.lastSyncResult.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var authIsRegister by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(YuniSpacing.page),
        verticalArrangement = Arrangement.spacedBy(YuniSpacing.block)
    ) {
        Text("我们", style = MaterialTheme.typography.headlineSmall)

        // 操作结果提示（原来 vm.toast 没有任何地方收集，导致邀请码提示一闪而过都看不到）
        if (!toast.isNullOrBlank()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(YuniSpacing.block), verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                    Text(toast!!, style = MaterialTheme.typography.bodyMedium)
                    TextButton(onClick = { vm.dismissToast() }) { Text("知道了") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(YuniSpacing.card), verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                Text("本机档案", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = user?.let { "${it.nickname}（${it.id.take(12)}…）" } ?: "还没有档案",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    "对方：未绑定（B1 双人空间后出现）",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedButton(onClick = { showProfileDialog = true }) {
                    Text(if (user == null) "创建本机档案" else "重建本机档案")
                }
            }
        }

        // 云端账号与双人空间（ECS 跳板联调入口）
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(YuniSpacing.card), verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                Text("云端账号与双人空间", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (user?.id?.startsWith("local-") == false) "状态：云端账号 ${user?.nickname ?: ""}" else "状态：仅本机档案（未登录云端）",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "空间：${couple?.id?.take(16) ?: "未创建"}${if (!couple?.memberBId.isNullOrBlank()) " · 已双人绑定" else " · 等待对方加入"}",
                    style = MaterialTheme.typography.bodySmall
                )
                Row(horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                    OutlinedButton(onClick = { authIsRegister = true; showAuthDialog = true }) { Text("注册") }
                    OutlinedButton(onClick = { authIsRegister = false; showAuthDialog = true }) { Text("登录") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                    OutlinedButton(onClick = { vm.createSpaceAndInvite(null) }) { Text("建空间+发邀请码") }
                    OutlinedButton(onClick = { showInviteDialog = true }) { Text("输入邀请码") }
                }
                // 邀请码持久显示：这是必须抄给对方的信息，不能只靠一闪而过的提示
                if (!inviteCode.isNullOrBlank()) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                cm.setPrimaryClip(ClipData.newPlainText("予你邀请码", inviteCode))
                                vm.showToast("邀请码 $inviteCode 已复制，发给另一半")
                            }
                    ) {
                        Column(modifier = Modifier.padding(YuniSpacing.block), verticalArrangement = Arrangement.spacedBy(YuniSpacing.xs)) {
                            Text("你的邀请码（点此复制，发给另一半）", style = MaterialTheme.typography.labelMedium)
                            Text(inviteCode!!, style = MaterialTheme.typography.headlineMedium)
                            Text("对方在「我们」页点「输入邀请码」粘贴即可", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                if (user?.id?.startsWith("local-") == false) {
                    TextButton(onClick = { vm.logout() }) { Text("退出云端账号") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(YuniSpacing.card), verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                Text("我们的清单", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onOpenWishes, modifier = Modifier.fillMaxWidth()) {
                    Text("愿望清单（$wishCount 条）")
                }
                OutlinedButton(onClick = onOpenMeetings, modifier = Modifier.fillMaxWidth()) {
                    Text("见面计划")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(YuniSpacing.card), verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                Text("本地数据库（Room）", style = MaterialTheme.typography.titleMedium)
                LabeledRow("回忆", "$memoryCount 条")
                LabeledRow("日记", "$diaryCount 条")
                LabeledRow("愿望", "$wishCount 条")
                Text(
                    "这三行有数字 = Room 建表、写入、读取都通了",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(YuniSpacing.card), verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                Text("云端与同步", style = MaterialTheme.typography.titleMedium)
                Text(vm.cloudStateText, style = MaterialTheme.typography.bodySmall)
                Text("待上传队列：$pending 条", style = MaterialTheme.typography.bodySmall)
                Text(
                    "上次同步：" + if (lastSync == 0L) "无" else formatTime(lastSync),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(lastResult, style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                    Button(onClick = { vm.syncNow() }) { Text("立即同步") }
                    OutlinedButton(onClick = { vm.scheduleBackgroundSync() }) { Text("排后台任务") }
                }
            }
        }

        // S5.9 本地提醒
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(YuniSpacing.card), verticalArrangement = Arrangement.spacedBy(YuniSpacing.xs)) {
                Text("本地提醒（S5.9）", style = MaterialTheme.typography.titleMedium)
                Text("每天固定时间提醒记录，不发隐私内容、不联网。", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                    OutlinedButton(onClick = { vm.setDailyReminder(21, 0) }) { Text("每天 21:00") }
                    OutlinedButton(onClick = { vm.cancelDailyReminder() }) { Text("关闭提醒") }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(YuniSpacing.card), verticalArrangement = Arrangement.spacedBy(YuniSpacing.xs)) {
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

    if (showAuthDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text(if (authIsRegister) "注册云端账号" else "登录云端账号") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                    OutlinedTextField(
                        value = username, onValueChange = { username = it },
                        label = { Text("用户名") }, singleLine = true
                    )
                    OutlinedTextField(
                        value = password, onValueChange = { password = it },
                        label = { Text("密码") }, singleLine = true
                    )
                    Text(
                        "账号存在这台临时服务器上；退出登录不会删本地日记。",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (authIsRegister) vm.register(username, password) else vm.login(username, password)
                    showAuthDialog = false
                }) { Text(if (authIsRegister) "注册" else "登录") }
            },
            dismissButton = {
                TextButton(onClick = { showAuthDialog = false }) { Text("取消") }
            }
        )
    }

    if (showInviteDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("输入邀请码") },
            text = {
                Column {
                    OutlinedTextField(
                        value = code, onValueChange = { code = it },
                        label = { Text("另一半发来的邀请码") }, singleLine = true
                    )
                    Text("一个邀请码只能核销一次。", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.acceptInvite(code)
                    showInviteDialog = false
                }) { Text("加入空间") }
            },
            dismissButton = {
                TextButton(onClick = { showInviteDialog = false }) { Text("取消") }
            }
        )
    }
}
