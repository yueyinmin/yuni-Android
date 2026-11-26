package com.mulin.us.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/** 统一绘本弹窗：标题、内容槽、确认/取消，全部走主题 Token */
@Composable
fun StorybookDialog(
    title: String,
    onDismiss: () -> Unit,
    confirmText: String = "确定",
    dismissText: String? = "取消",
    enabled: Boolean = true,
    content: @Composable () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, style = MaterialTheme.typography.titleMedium) },
        text = content,
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = enabled) { Text(confirmText) }
        },
        dismissButton = dismissText?.let {
            { TextButton(onClick = onDismiss) { Text(it) } }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = MaterialTheme.shapes.large
    )
}
