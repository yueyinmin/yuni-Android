package com.mulin.us.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mulin.us.ui.theme.UsRadius
import com.mulin.us.ui.theme.UsSpacing
import java.time.LocalDate

/**
 * 恋爱开始日期选择弹窗（V3 P0 重构）。
 *
 * V2.2 问题：继承 UsShapes.extraLarge=999.dp，AlertDialog 变成巨大椭圆，
 * 标题/按钮被裁。V3 使用显式 24dp 圆角矩形、24dp 左右边距、
 * wrapContent 高度且不超过屏幕 80%，三列等宽滚轮，底部按钮始终可见。
 */
@Composable
fun StartDatePickerDialog(
    initialDate: LocalDate?,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    val today = LocalDate.now()
    var year by remember { mutableStateOf((initialDate ?: today).year) }
    var month by remember { mutableStateOf((initialDate ?: today).monthValue) }
    var day by remember { mutableStateOf((initialDate ?: today).dayOfMonth) }

    val candidate = runCatching { LocalDate.of(year, month, day) }.getOrNull()
    val valid = validStartDate(candidate, today)
    val maxHeight = LocalConfiguration.current.screenHeightDp.dp * 0.8f

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .padding(horizontal = UsSpacing.page)
            .heightIn(max = maxHeight),
        properties = DialogProperties(usePlatformDefaultWidth = false),
        shape = RoundedCornerShape(UsRadius.dialog),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    if (initialDate == null) "选择恋爱开始日" else "修改恋爱开始日",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (initialDate != null) {
                    Text(
                        "修改后相恋天数、年度纪念日和倒计时都会更新。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(UsSpacing.md)) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StorybookButton(
                    text = "取消",
                    onClick = onDismiss,
                    secondary = true,
                    modifier = Modifier.weight(1f)
                )
                StorybookButton(
                    text = "确认",
                    onClick = { candidate?.let { date -> onConfirm(date.toString()) } },
                    enabled = valid,
                    modifier = Modifier.weight(1f)
                )
            }
        },
        dismissButton = null
    )
}

/** 可单测的日期合法性：不存在日期、未来日期均拒绝；闰年日期由 LocalDate 负责校验 */
internal fun validStartDate(candidate: LocalDate?, today: LocalDate = LocalDate.now()): Boolean =
    candidate != null && !candidate.isAfter(today)
