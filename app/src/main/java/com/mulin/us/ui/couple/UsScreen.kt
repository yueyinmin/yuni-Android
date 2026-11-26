package com.mulin.us.ui.couple

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
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
import com.mulin.us.ui.theme.YuniDockInset
import com.mulin.us.ui.components.YuniGroupDivider
import com.mulin.us.ui.components.YuniGroupHeader
import com.mulin.us.ui.components.YuniSettingsRow
import com.mulin.us.ui.components.YuniTone
import com.mulin.us.ui.components.YuniTones
import com.mulin.us.ui.theme.YuniSpacing

/**
 * 我们（Tab 5）—— 按轻记「我的」页（MineScreen）版式重排。
 *
 * 轻记结构：标题 + 右上设置图标 → 资料卡（头像+昵称+箭头） → 分组标题 + 白卡列表行。
 * **不做轻记的"使用情况统计"**：予你没有任何统计模块，全部选项都来自真实功能，
 * 每一行都有真实跳转或动作（回忆/日记/愿望/见面/问答/悄悄话/未来信/同步/提醒）。
 */
@Composable
fun UsScreen(
    vm: YuniViewModel,
    onOpenWishes: () -> Unit,
    onOpenMeetings: () -> Unit,
    onOpenMemories: () -> Unit,
    onOpenDiary: () -> Unit,
    onOpenInteraction: () -> Unit,
    onOpenMailbox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by vm.currentUser.collectAsState()
    val couple by vm.couple.collectAsState()
    val inviteCode by vm.lastInviteCode.collectAsState()
    val toast by vm.toast.collectAsState()
    val context = LocalContext.current
    val pending by vm.pendingCount.collectAsState()

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var authIsRegister by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

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

        // 标题 + 右上设置（轻记 MineScreen 结构）
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("我们", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = "设置",
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .size(22.dp)
                    .clickable { showSettingsDialog = true }
            )
        }
        Spacer(Modifier.height(16.dp))

        // ---------- 资料卡：头像 58dp 带边框 + 昵称 + 副信息 + 箭头 ----------
        YuniAppearItem(index = 0) {
            YuniCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showProfileDialog = true }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (user?.nickname?.take(1) ?: "予").uppercase(),
                            fontSize = 22.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(user?.nickname ?: "还没建立档案",
                            fontSize = 17.sp, fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            if (loggedIn) "云端账号已登录" else "本机档案 · 未登录云端",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            if (bound) "已和另一半绑定空间" else "等待另一半加入",
                            fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // 操作结果提示
        if (!toast.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            YuniCard {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(toast!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.dismissToast() }) { Text("知道了") }
                }
            }
        }

        // ---------- 记录 ----------
        YuniAppearItem(index = 1) {
            YuniGroupHeader("记录")
            YuniCard {
                YuniSettingsRow("共同回忆", Icons.Filled.DateRange, YuniTones.Teal,
                    onClick = onOpenMemories)
                YuniGroupDivider()
                YuniSettingsRow("日记本", Icons.Filled.Person, YuniTones.Green,
                    trailing = "私密与共同", onClick = onOpenDiary)
                YuniGroupDivider()
                YuniSettingsRow("愿望清单", Icons.Filled.Star, YuniTones.Orange,
                    onClick = onOpenWishes)
                YuniGroupDivider()
                YuniSettingsRow("见面计划", Icons.Filled.DateRange, YuniTones.Purple,
                    onClick = onOpenMeetings)
            }
        }

        // ---------- 互动 ----------
        YuniAppearItem(index = 2) {
            YuniGroupHeader("互动")
            YuniCard {
                YuniSettingsRow("每日问答", Icons.Filled.Favorite, YuniTones.Pink,
                    trailing = "揭晓", onClick = onOpenInteraction)
                YuniGroupDivider()
                YuniSettingsRow("悄悄话", Icons.Filled.Email, YuniTones.Blue,
                    onClick = onOpenInteraction)
                YuniGroupDivider()
                YuniSettingsRow("未来信与胶囊", Icons.Filled.DateRange, YuniTones.Orange,
                    onClick = onOpenMailbox)
            }
        }

        // ---------- 双人空间 ----------
        YuniAppearItem(index = 3) {
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
                YuniSettingsRow("共同空间", Icons.Filled.Favorite, YuniTones.Pink,
                    trailing = couple?.id?.take(10) ?: "未创建",
                    onClick = { vm.createSpaceAndInvite(null) }, showArrow = false)
                YuniGroupDivider()
                YuniSettingsRow("输入邀请码", Icons.Filled.Add, YuniTones.Orange,
                    trailing = if (bound) "已绑定" else "",
                    onClick = { showInviteDialog = true }, showArrow = !bound)
                if (loggedIn) {
                    YuniGroupDivider()
                    YuniSettingsRow("退出云端账号", Icons.Filled.ExitToApp, YuniTones.Gray,
                        onClick = { vm.logout() }, showArrow = false)
                }
            }

            // 邀请码持久卡片（未绑定时才显示）
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

        // ---------- 同步 ----------
        YuniAppearItem(index = 4) {
            YuniGroupHeader("同步")
            YuniCard {
                YuniSettingsRow("立即同步", Icons.Filled.Refresh, YuniTones.Green,
                    trailing = if (pending > 0) "待上传 $pending 条" else "已是最新",
                    onClick = { vm.syncNow() }, showArrow = false)
                YuniGroupDivider()
                YuniSettingsRow("排入后台同步", Icons.Filled.Settings, YuniTones.Blue,
                    onClick = { vm.scheduleBackgroundSync() }, showArrow = false)
            }
        }

        // ---------- 隐私（轻记放最底，不放使用情况）----------
        YuniAppearItem(index = 5) {
            YuniGroupHeader("隐私")
            YuniCard {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Lock, contentDescription = null,
                        tint = YuniTones.Green.icon, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("私密日记只存本机 · Token 加密落盘 · 退出不删本地日记",
                        fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Spacer(Modifier.height(YuniDockInset.TailSpacer))
    }

    // ---------- 弹窗 ----------
    if (showProfileDialog) {
        var nickname by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = { Text("本机档案") },
            text = {
                Column {
                    Text("没配云端也能用：档案只存这台手机。", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = nickname, onValueChange = { nickname = it },
                        label = { Text("昵称") }, singleLine = true)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.createLocalProfile(nickname)
                    showProfileDialog = false
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showProfileDialog = false }) { Text("取消") } }
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
                    OutlinedTextField(value = username, onValueChange = { username = it },
                        label = { Text("用户名") }, singleLine = true)
                    OutlinedTextField(value = password, onValueChange = { password = it },
                        label = { Text("密码") }, singleLine = true)
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
            dismissButton = { TextButton(onClick = { showAuthDialog = false }) { Text("取消") } }
        )
    }

    if (showInviteDialog) {
        var code by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showInviteDialog = false },
            title = { Text("输入邀请码") },
            text = {
                Column {
                    OutlinedTextField(value = code, onValueChange = { code = it },
                        label = { Text("另一半发来的邀请码") }, singleLine = true)
                    Text("一个邀请码只能核销一次。", style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    vm.acceptInvite(code)
                    showInviteDialog = false
                }) { Text("加入空间") }
            },
            dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("取消") } }
        )
    }

    // 设置面板：只放予你真实有的设置项
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("设置") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                    Text("相恋开始日", style = MaterialTheme.typography.titleMedium)
                    Text("设定后首页显示真实天数。", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = {
                        vm.setRelationshipStart(java.time.LocalDate.now().toEpochDay())
                        vm.showToast("已从今天开始记录")
                        showSettingsDialog = false
                    }) { Text("设为今天") }

                    Text("每日提醒", style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                        TextButton(onClick = { vm.setDailyReminder(21, 0); showSettingsDialog = false }) {
                            Text("每天 21:00")
                        }
                        TextButton(onClick = { vm.cancelDailyReminder(); showSettingsDialog = false }) {
                            Text("关闭提醒")
                        }
                    }

                    Text("隐私", style = MaterialTheme.typography.titleMedium)
                    Text("私密日记只存本机，永不进入上传队列；Token 用 Android Keystore 加密落盘。",
                        style = MaterialTheme.typography.bodySmall)
                }
            },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("完成") } }
        )
    }
}
