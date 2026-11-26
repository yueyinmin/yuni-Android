package com.mulin.us.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.domain.model.LocalUser

/**
 * 本机档案弹窗：昵称 + 头像。
 *
 * 头像点击后直接拉系统图库，选完由 ViewModel 压缩存本机；
 * 这里不假设云端一定可用，离线也能换。
 */
@Composable
fun ProfileDialog(
    user: LocalUser?,
    onDismiss: () -> Unit,
    onSaveNickname: (String) -> Unit,
    onPickAvatar: (Uri) -> Unit
) {
    var nickname by remember(user?.nickname) { mutableStateOf(user?.nickname ?: "") }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPickAvatar(uri)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("本机档案") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                YuniAvatar(
                    imagePath = user?.avatarLocalPath,
                    label = (nickname.take(1).ifBlank { "我" }).uppercase(),
                    size = 64.dp,
                    showEditHint = true,
                    contentDescription = "点击更换头像",
                    modifier = Modifier.clickable { pickImage.launch("image/*") }
                )
                Spacer(Modifier.height(6.dp))
                Text("点头像从相册选图，只存本机", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = nickname,
                    onValueChange = { nickname = it },
                    label = { Text("昵称") },
                    singleLine = true,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveNickname(nickname)
                onDismiss()
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } }
    )
}
