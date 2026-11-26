package com.mulin.us.ui.couple

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.YuniAppearItem
import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.components.YuniGroupDivider
import com.mulin.us.ui.components.YuniGroupHeader
import com.mulin.us.ui.components.YuniSettingsRow
import com.mulin.us.ui.components.YuniTone
import com.mulin.us.ui.components.YuniTones
import com.mulin.us.ui.components.formatTime
import com.mulin.us.ui.theme.YuniSpacing

/**
 * 我们（Tab 5）—— 轻记「我的」页式分组列表。
 *
 * 14:55 按轻记源码真值重构视觉：分组标题（卡片外 13sp 灰字）+ 白卡列表行
 * （38dp 彩色图标底 + 14sp 主文 + 右侧状态 + 箭头）。功能一行不少，
 * 三个 dialog 原样保留。
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

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var authIsRegister by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }

    val loggedIn = user?.id?.startsWith("local-") == false
    val bound = !couple?.memberBId.isNullOrBlank()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text(
            "我们",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))

        // ---------- 资料卡（轻记头像卡：浅胭脂圆 + 首字 + 昵称 + 副信息）----------
        YuniAppearItem(index = 0) {
        YuniCard {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showProfileDialog = true }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(58.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (user?.nickname?.take(1) ?: "予").uppercase(),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                    Text(
                        text = user?.nickname ?: "还没建立档案",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = if (loggedIn) "云端账号已登录" else "本机档案 · 未登录云端",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = if (bound) "已双人绑定" else "等待另一半",
                    fontSize = 12.sp,
                    color = if (bound) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        }

        // 操作结果提示
        if (!toast.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            YuniCard {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(toast!!, fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.dismissToast() }) { Text("知道了") }
                }
            }
        }

        // ---------- 双人空间 ----------
        YuniAppearItem(index = 1) {
        YuniGroupHeader("双人空间")
        YuniCard {
            YuniSettingsRow(
                title = if (loggedIn) "云端账号" else "注册 / 登录云端",
                icon = Icons.Filled.Email, tone = YuniTones.Blue,
                trailing = if (loggedIn) user?.nickname ?: "" else "未登录",
                onClick = { authIsRegister = true; showAuthDialog = true },
                showArrow = !loggedIn
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "共同空间",
                icon = Icons.Filled.Favorite, tone = YuniTones.Pink,
                trailing = couple?.id?.take(10) ?: "未创建",
                onClick = { vm.createSpaceAndInvite(null) },
                showArrow = false
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "输入邀请码",
                icon = Icons.Filled.Add, tone = YuniTones.Orange,
                trailing = if (bound) "已绑定" else "",
                onClick = { showInviteDialog = true },
                showArrow = !bound
            )
            if (loggedIn) {
                YuniGroupDivider()
                YuniSettingsRow(
                    title = "退出云端账号",
                    icon = Icons.Filled.ExitToApp, tone = YuniTones.Gray,
                    onClick = { vm.logout() }, showArrow = false
                )
            }
        }

        // 邀请码持久卡片（点此复制）—— 必须抄给对方，保持显眼
        if (!inviteCode.isNullOrBlank() && !bound) {
            Spacer(Modifier.height(8.dp))
            YuniCard {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("予你邀请码", inviteCode))
                            vm.showToast("邀请码 $inviteCode 已复制，发给另一半")
                        }
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("你的邀请码 · 点此复制", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(6.dp))
                    Text(inviteCode!!, fontSize = 28.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(4.dp))
                    Text("发给另一半，在「输入邀请码」里粘贴", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        }

        // ---------- 我们的清单 ----------
        YuniAppearItem(index = 2) {
        YuniGroupHeader("我们的清单")
        YuniCard {
            YuniSettingsRow(
                title = "愿望清单", icon = Icons.Filled.Star, tone = YuniTones.Orange,
                trailing = "$wishCount 条", onClick = onOpenWishes
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "见面计划", icon = Icons.Filled.DateRange, tone = YuniTones.Purple,
                onClick = onOpenMeetings
            )
        }

        }

        // ---------- 本机数据 ----------
        YuniAppearItem(index = 3) {
        YuniGroupHeader("本机数据")
        YuniCard {
            YuniSettingsRow(
                title = "共同回忆", icon = Icons.Filled.DateRange, tone = YuniTones.Teal,
                trailing = "$memoryCount 条", onClick = {}, showArrow = false
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "日记", icon = Icons.Filled.Person, tone = YuniTones.Green,
                trailing = "$diaryCount 条", onClick = {}, showArrow = false
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "愿望", icon = Icons.Filled.Star, tone = YuniTones.Orange,
                trailing = "$wishCount 条", onClick = {}, showArrow = false
            )
        }

        }

        // ---------- 同步与工具 ----------
        YuniAppearItem(index = 4) {
        YuniGroupHeader("同步与工具")
        YuniCard {
            YuniSettingsRow(
                title = "立即同步", icon = Icons.Filled.Refresh, tone = YuniTones.Green,
                trailing = if (pending > 0) "待上传 $pending 条" else "已是最新",
                onClick = { vm.syncNow() }, showArrow = false
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "排入后台同步", icon = Icons.Filled.Settings, tone = YuniTones.Blue,
                onClick = { vm.scheduleBackgroundSync() }, showArrow = false
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "每天提醒", icon = Icons.Filled.Notifications, tone = YuniTones.Purple,
                trailing = "21:00", onClick = { vm.setDailyReminder(21, 0) }, showArrow = false
            )
            YuniGroupDivider()
            YuniSettingsRow(
                title = "关闭提醒", icon = Icons.Filled.Clear, tone = YuniTones.Gray,
                onClick = { vm.cancelDailyReminder() }, showArrow = false
            )
        }

        }

        // 同步详情 + 安全说明
        YuniAppearItem(index = 5) {
        YuniGroupHeader("状态与隐私")
        YuniCard {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(vm.cloudStateText, fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "上次同步：" + if (lastSync == 0L) "无" else formatTime(lastSync),
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null,
                        tint = YuniTones.Green.icon, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("私密日记只存本机 · Token 加密落盘", fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        }

        Spacer(Modifier.height(24.dp))
    }

    if (showProfileDialog) {
        var nickname by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("本机档案") },
            text = {
                Column {
                    Text("没配云端也能用：档案只存这台手机。",
                        style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = nickname, onValueChange = { nickname = it },
                        label = { Text("昵称") }, singleLine = true
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
                    Text("账号存在这台临时服务器上；退出登录不会删本地日记。",
                        style = MaterialTheme.typography.bodySmall)
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
