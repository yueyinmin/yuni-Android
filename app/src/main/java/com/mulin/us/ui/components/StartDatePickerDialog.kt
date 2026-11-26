package com.mulin.us.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.LocalDate

/**
 * 恋爱开始日期选择弹窗（工单 V3.1 C3.3/C3.4）。
 *
 * 年/月/日三列滚轮复用 YuniWheelDatePicker（轻记交互真值）。
 * 规则：可以选历史任意日期；未来日期确定按钮置灰并明确提示；
 * 首次打开默认今天但**不自动保存**，用户必须主动确认。
 */
@Composable
fun StartDatePickerDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val today = LocalDate.now()
    var picked by remember {
        mutableStateOf(initialDate ?: today)
    }
    var year by remember { mutableStateOf(picked.year) }
    var month by remember { mutableStateOf(picked.monthValue) }
    var day by remember { mutableStateOf(picked.dayOfMonth) }

    // 组合日期并校验（滚轮只回传三个数字，合法性在这里做 —— 轻记 WheelDatePicker 的分工）
    val candidate = runCatching { LocalDate.of(year, month, day) }.getOrNull()
    val valid = candidate != null && !candidate.isAfter(today)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialDate == null) "选择恋爱开始日" else "修改恋爱开始日") },
        text = {
            Column {
                if (initialDate != null) {
                    Text("修改后相恋天数、年度纪念日和倒计时都会更新。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                YuniWheelDatePicker(
                    initialDate = initialDate ?: today,
                    onSelected = { y, m, d ->
                        year = y; month = m; day = d
                    }
                )
                if (!valid) {
                    Text(
                        if (candidate == null) "这个日期不存在（比如 2 月 30 日）" else "恋爱开始日不能晚于今天",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = valid, onClick = {
                candidate?.let { onConfirm(it.toString()) }
            }) { Text("确认") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("取消") }
        }
    )
}
