package com.mulin.us.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.ProfileDialog
import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.components.YuniGroupDivider
import com.mulin.us.ui.components.YuniGroupHeader
import com.mulin.us.ui.components.YuniSettingsRow
import com.mulin.us.ui.components.YuniTone
import com.mulin.us.ui.components.YuniTones
import com.mulin.us.ui.theme.YuniDockInset
import com.mulin.us.ui.components.formatTime

/** 独立设置页（工单 D6/F）：同步与存储、隐私与安全、账号管理收口于此 */
@Composable
fun SettingsScreen(vm: YuniViewModel, onBack: () -> Unit) {
    val pending by vm.pendingCount.collectAsState()
    val lastSync by vm.lastSyncAt.collectAsState()
    val user by vm.currentUser.collectAsState()
    var showProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回",
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .padding(8.dp))
            Text("设置", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(Modifier.height(16.dp))

        YuniGroupHeader("同步与存储")
        YuniCard {
            YuniSettingsRow("立即同步", Icons.Filled.Refresh, YuniTones.Green,
                trailing = if (pending > 0) "待上传 $pending 条" else "已是最新",
                onClick = { vm.syncNow() }, showArrow = false)
            YuniGroupDivider()
            YuniSettingsRow("后台定时同步", Icons.Filled.Refresh, YuniTones.Blue,
                trailing = "已开启", onClick = { vm.scheduleBackgroundSync() }, showArrow = false)
            YuniGroupDivider()
            YuniSettingsRow("上次同步", Icons.Filled.Email, YuniTones.Gray,
                trailing = if (lastSync == 0L) "无" else formatTime(lastSync),
                onClick = {}, showArrow = false, enabled = false)
        }

        YuniGroupHeader("隐私与安全")
        YuniCard {
            YuniSettingsRow("私密日记", Icons.Filled.Lock, YuniTones.Green,
                trailing = "仅本机", onClick = {}, showArrow = false, enabled = false)
            YuniGroupDivider()
            YuniSettingsRow("Token 加密", Icons.Filled.Lock, YuniTones.Purple,
                trailing = "Keystore", onClick = {}, showArrow = false, enabled = false)
        }

        YuniGroupHeader("账号管理")
        YuniCard {
            YuniSettingsRow("本机档案", Icons.Filled.Person, YuniTones.Orange,
                trailing = user?.nickname ?: "未建立", onClick = { showProfileDialog = true })
            YuniGroupDivider()
            YuniSettingsRow("更换头像", Icons.Filled.Person, YuniTones.Pink,
                trailing = if (user?.avatarLocalPath != null) "已设置" else "从相册选图",
                onClick = { showProfileDialog = true }, showArrow = false)
            YuniGroupDivider()
            YuniSettingsRow("退出云端账号", Icons.Filled.Email, YuniTones.Gray,
                onClick = { vm.logout() }, showArrow = false)
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
}
