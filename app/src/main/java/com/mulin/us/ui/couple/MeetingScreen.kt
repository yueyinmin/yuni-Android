package com.mulin.us.ui.couple

import com.mulin.us.ui.components.YuniCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.Modifier
import com.mulin.us.domain.model.MeetingPlan
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.SubPageScaffold
import com.mulin.us.ui.components.SyncBadge
import com.mulin.us.ui.theme.YuniSpacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 见面计划（6.5/6.6）：列表 + 倒计时天数 + 地点备注 + 删除。
 * 走 MeetingPlanRepository，新增/删除都会请求同步。
 */
@Composable
fun MeetingScreen(vm: YuniViewModel, onBack: () -> Unit, modifier: Modifier = Modifier) {
    val meetings by vm.meetings.collectAsState()
    var showAdd by remember { mutableStateOf(false) }

    SubPageScaffold(title = "见面计划", onBack = onBack) { padding ->
        Column(modifier = modifier.fillMaxSize().padding(padding).padding(YuniSpacing.page)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("见面计划", style = MaterialTheme.typography.headlineSmall)
                    Text("共 ${meetings.size} 条", style = MaterialTheme.typography.bodySmall)
                }
                Button(onClick = { showAdd = true }) { Text("加一条") }
            }

            LazyColumn(modifier = Modifier.fillMaxSize().padding(top = YuniSpacing.md)) {
                items(meetings, key = { it.localId }) { m ->
                    MeetingCard(m, vm)
                }
            }
        }
    }

    if (showAdd) AddMeetingDialog(vm) { showAdd = false }
}

@Composable
private fun MeetingCard(m: MeetingPlan, vm: YuniViewModel) {
    val date = Instant.ofEpochMilli(m.plannedAt).atZone(ZoneId.systemDefault()).toLocalDate()
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    YuniCard(modifier = Modifier.padding(bottom = YuniSpacing.sm)) {
        Column(modifier = Modifier.padding(YuniSpacing.lg), verticalArrangement = Arrangement.spacedBy(YuniSpacing.xs)) {
            Text(
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                if (m.doneAt != null) "已见面" else if (days > 0) "还有 $days 天" else if (days == 0L) "就是今天" else "已过去 ${-days} 天",
                style = MaterialTheme.typography.bodyMedium
            )
            if (!m.place.isNullOrBlank()) Text("地点：${m.place}", style = MaterialTheme.typography.bodySmall)
            if (!m.note.isNullOrBlank()) Text(m.note, style = MaterialTheme.typography.bodySmall)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SyncBadge(m.syncStatus)
                TextButton(onClick = { vm.deleteMeeting(m.localId) }) { Text("删除") }
            }
        }
    }
}

@Composable
private fun AddMeetingDialog(vm: YuniViewModel, onClose: () -> Unit) {
    var dateText by remember { mutableStateOf(LocalDate.now().plusDays(7).toString()) }
    var place by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text("下次见面") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(YuniSpacing.sm)) {
                OutlinedTextField(value = dateText, onValueChange = { dateText = it }, label = { Text("日期 yyyy-MM-dd") }, singleLine = true)
                OutlinedTextField(value = place, onValueChange = { place = it }, label = { Text("地点") }, singleLine = true)
                OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("备注") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val date = runCatching { LocalDate.parse(dateText) }.getOrNull()
                if (date != null) {
                    vm.addMeeting(date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                        place.ifBlank { null }, note.ifBlank { null })
                    onClose()
                }
            }) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onClose) { Text("取消") } }
    )
}
