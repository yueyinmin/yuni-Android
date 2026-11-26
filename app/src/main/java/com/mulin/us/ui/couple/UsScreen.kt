package com.mulin.us.ui.couple

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.ProfileDialog
import com.mulin.us.ui.components.StorybookCard
import com.mulin.us.ui.components.StartDatePickerDialog
import com.mulin.us.ui.components.YuniAppearItem
import com.mulin.us.ui.components.YuniAvatar
import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.theme.YuniDockInset
import com.mulin.us.ui.components.YuniGroupDivider
import com.mulin.us.ui.components.YuniGroupHeader
import com.mulin.us.ui.components.YuniSettingsRow
import com.mulin.us.ui.components.YuniTones

/**
 * 我们 B（工单 V3.1 阶段 E）：我们的空间 / 我们的记录 / 互动与信箱 / 邀请与绑定 / 设置入口。
 * 技术状态收口到独立设置页；已绑定时收起邀请码区（D5）。
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
    onOpenSettings: () -> Unit,
    onStartDate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val user by vm.currentUser.collectAsState()
    val couple by vm.couple.collectAsState()
    val inviteCode by vm.lastInviteCode.collectAsState()
    val toast by vm.toast.collectAsState()
    val context = LocalContext.current

    var showProfileDialog by remember { mutableStateOf(false) }
    var showAuthDialog by remember { mutableStateOf(false) }
    var authIsRegister by remember { mutableStateOf(true) }
    var showInviteDialog by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val loggedIn = user?.id?.startsWith("local-") == false
    val bound = couple?.isBound == true
    val startDate = couple?.startDate

    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Text("我们", fontSize = 22.sp, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(16.dp))

        // 顶部账号卡：登录状态与绑定状态独立展示
        YuniAppearItem(index = 0) {
            StorybookCard(
                accent = MaterialTheme.colorScheme.primary,
                wash = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    Modifier.fillMaxWidth().clickable { showProfileDialog = true },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    YuniAvatar(
                        imagePath = user?.avatarLocalPath,
                        label = (user?.nickname?.take(1) ?: "予").uppercase(),
                        size = 58.dp,
                        contentDescription = "本机头像"
                    )
                    Column(Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(user?.nickname ?: "还没建立档案", fontSize = 17.sp,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(4.dp))
                        Text(if (loggedIn) "云端账号：已登录" else "云端账号：未登录",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(2.dp))
                        Text(if (bound) "情侣绑定：已绑定" else "情侣绑定：未绑定",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.size(20.dp))
                }
            }
        }

        if (!toast.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            YuniCard {
                Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(toast!!, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f))
                    TextButton(onClick = { vm.dismissToast() }) { Text("知道了") }
                }
            }
        }

        YuniAppearItem(index = 1) {
            YuniGroupHeader("我们的空间")
            YuniCard {
                YuniSettingsRow("共同资料", Icons.Filled.Person, YuniTones.Blue,
                    trailing = startDate?.toString() ?: "设置开始日", onClick = onStartDate)
                YuniGroupDivider()
                YuniSettingsRow("恋爱开始日", Icons.Filled.DateRange, YuniTones.Pink,
                    trailing = startDate?.toString() ?: "未设置",
                    onClick = { showDatePicker = true })
                YuniGroupDivider()
                YuniSettingsRow("纪念日", Icons.Filled.DateRange, YuniTones.Orange,
                    trailing = "按开始日计算", onClick = onStartDate)
                YuniGroupDivider()
                YuniSettingsRow("见面计划", Icons.Filled.DateRange, YuniTones.Purple,
                    onClick = onOpenMeetings)
            }
        }

        YuniAppearItem(index = 2) {
            YuniGroupHeader("我们的记录")
            YuniCard {
                YuniSettingsRow("共同回忆", Icons.Filled.DateRange, YuniTones.Teal,
                    onClick = onOpenMemories)
                YuniGroupDivider()
                YuniSettingsRow("日记本", Icons.Filled.Person, YuniTones.Green,
                    trailing = "私密 + 共同", onClick = onOpenDiary)
                YuniGroupDivider()
                YuniSettingsRow("愿望清单", Icons.Filled.Star, YuniTones.Orange,
                    onClick = onOpenWishes)
            }
        }

        YuniAppearItem(index = 3) {
            YuniGroupHeader("互动与信箱")
            YuniCard {
                YuniSettingsRow("每日问答", Icons.Filled.Favorite, YuniTones.Pink,
                    trailing = "揭晓", onClick = onOpenInteraction)
                YuniGroupDivider()
                YuniSettingsRow("悄悄话", Icons.Filled.Email, YuniTones.Blue,
                    onClick = onOpenInteraction)
                YuniGroupDivider()
                YuniSettingsRow("未来信与时间胶囊", Icons.Filled.Email, YuniTones.Purple,
                    onClick = onOpenMailbox)
            }
        }

        YuniAppearItem(index = 4) {
            YuniGroupHeader("邀请与绑定")
            YuniCard {
                if (!bound) {
                    YuniSettingsRow("注册 / 登录云端", Icons.Filled.Email, YuniTones.Blue,
                        trailing = if (loggedIn) user?.nickname ?: "" else "未登录",
                        onClick = { authIsRegister = true; showAuthDialog = true },
                        showArrow = !loggedIn)
                    YuniGroupDivider()
                    YuniSettingsRow("建立共同空间", Icons.Filled.Favorite, YuniTones.Pink,
                        trailing = couple?.id?.take(10) ?: "未创建",
                        onClick = { vm.createSpaceAndInvite(null) }, showArrow = false)
                    YuniGroupDivider()
                    YuniSettingsRow("输入邀请码", Icons.Filled.Add, YuniTones.Orange,
                        onClick = { showInviteDialog = true })
                } else {
                    YuniSettingsRow("另一半", Icons.Filled.Favorite, YuniTones.Pink,
                        trailing = couple?.memberBId?.take(10) ?: "已绑定",
                        onClick = {}, showArrow = false, enabled = false)
                    YuniGroupDivider()
                    YuniSettingsRow("复制我的邀请码", Icons.Filled.Add, YuniTones.Gray,
                        onClick = {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("予你邀请码", inviteCode ?: ""))
                            vm.showToast("邀请码已复制")
                        }, showArrow = false)
                }
                if (loggedIn) {
                    YuniGroupDivider()
                    YuniSettingsRow("退出云端账号", Icons.Filled.ExitToApp, YuniTones.Gray,
                        onClick = { vm.logout() }, showArrow = false)
                }
            }

            if (!bound && !inviteCode.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                YuniCard {
                    Column(
                        Modifier.fillMaxWidth().clickable {
                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            cm.setPrimaryClip(ClipData.newPlainText("予你邀请码", inviteCode))
                            vm.showToast("邀请码 $inviteCode 已复制，发给另一半")
                        }.padding(16.dp),
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

        YuniAppearItem(index = 5) {
            YuniGroupHeader("设置")
            YuniCard {
                YuniSettingsRow("设置", Icons.Filled.Settings, YuniTones.Gray,
                    trailing = "同步与存储 · 隐私与安全", onClick = onOpenSettings)
            }
        }

        Spacer(Modifier.height(YuniDockInset.TailSpacer))
    }

    if (showProfileDialog) {
        ProfileDialog(
            user = user,
            onDismiss = { showProfileDialog = false },
            onSaveNickname = { vm.saveNickname(it) },
            onPickAvatar = { vm.saveAvatar(it) }
        )
    }
    if (showAuthDialog) {
        var username by remember { mutableStateOf("") }
        var password by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAuthDialog = false },
            title = { Text(if (authIsRegister) "注册云端账号" else "登录云端账号") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = username, onValueChange = { username = it },
                        label = { Text("用户名") }, singleLine = true)
                    OutlinedTextField(value = password, onValueChange = { password = it },
                        label = { Text("密码") }, singleLine = true)
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
            onDismissRequest = { showInviteDialog = false }, title = { Text("输入邀请码") },
            text = {
                OutlinedTextField(value = code, onValueChange = { code = it },
                    label = { Text("另一半发来的邀请码") }, singleLine = true)
            },
            confirmButton = {
                TextButton(onClick = { vm.acceptInvite(code); showInviteDialog = false }) { Text("加入空间") }
            },
            dismissButton = { TextButton(onClick = { showInviteDialog = false }) { Text("取消") } }
        )
    }
    if (showDatePicker) {
        StartDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { iso -> vm.confirmStartDate(iso); showDatePicker = false }
        )
    }
}
