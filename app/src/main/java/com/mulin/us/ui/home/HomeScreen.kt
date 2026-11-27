package com.mulin.us.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.StartDatePickerDialog
import com.mulin.us.ui.home.components.AnniversaryCounterCard
import com.mulin.us.ui.home.components.DailyInteractionCard
import com.mulin.us.ui.home.components.HeroCoupleBanner
import com.mulin.us.ui.home.components.HomeHeaderBar
import com.mulin.us.ui.home.components.MemoryPreviewUiModel
import com.mulin.us.ui.home.components.MemoriesPreviewCard
import com.mulin.us.ui.theme.UsSpacing
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 首页 V3.1：素材包落地版。
 * 业务数据仍只来自 YuniViewModel / TimelineRepository；本文件只做状态映射与组合。
 */
@Composable
fun HomeScreen(
    vm: YuniViewModel,
    onWriteDiary: () -> Unit,
    onWriteMemory: () -> Unit,
    onOpenInteraction: () -> Unit,
    onOpenMailbox: () -> Unit,
    onOpenTimeline: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    val couple by vm.couple.collectAsStateWithLifecycle()
    val user by vm.currentUser.collectAsStateWithLifecycle()
    val recent by vm.recentTimeline.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var dailyNote by rememberSaveable { mutableStateOf("今天也要一起呀 ♥") }

    val startDate = couple?.startDate
    val days = startDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) }?.coerceAtLeast(0)
    val bound = couple?.isBound == true
    val weekday = startDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.CHINESE) ?: "—"
    val anniversary = nextAnniversary(startDate)
    val remaining = runCatching {
        if (anniversary == "—" || anniversary == "先设置开始日") null
        else ChronoUnit.DAYS.between(LocalDate.now(), LocalDate.parse(anniversary)).coerceAtLeast(0)
    }.getOrNull()

    val memory = recent.firstOrNull()?.let { entry ->
        MemoryPreviewUiModel(
            id = entry.id,
            dateText = entry.eventAt.toString(),
            title = entry.title,
            daysRemaining = remaining ?: 0,
            coverUrl = entry.imagePath
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UsSpacing.page),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        HomeHeaderBar(
            myInitial = "M",
            onSettingsClick = onOpenSettings
        )

        HeroCoupleBanner(
            quote = "与你在一起，\n就是最好的时光",
            myAvatarPath = user?.avatarLocalPath,
            myInitial = "M",
            myNickname = user?.nickname ?: "先建立本机档案",
            partnerBound = bound,
            onInvitePartner = onOpenSettings
        )

        AnniversaryCounterCard(
            days = days,
            startDate = startDate?.toString(),
            weekday = weekday,
            onEditDate = { showDatePicker = true }
        )

        DailyInteractionCard(
            dailyNote = dailyNote,
            onOpenInteraction = onOpenInteraction,
            onEditNote = { showNoteDialog = true }
        )

        MemoriesPreviewCard(
            memory = memory,
            onOpenAll = onOpenTimeline,
            onAddMemory = onWriteMemory
        )

        Spacer(Modifier.height(96.dp))
    }

    if (showDatePicker) {
        StartDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { iso -> vm.confirmStartDate(iso); showDatePicker = false }
        )
    }

    if (showNoteDialog) {
        var draft by remember { mutableStateOf(dailyNote) }
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("今天想说点什么") },
            text = {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it },
                    label = { Text("今日状态") },
                    maxLines = 3
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    dailyNote = draft.ifBlank { "今天也要一起呀 ♥" }
                    showNoteDialog = false
                }) { Text("保存") }
            },
            dismissButton = { TextButton(onClick = { showNoteDialog = false }) { Text("取消") } }
        )
    }
}

/** 下一个纪念日：从开始日推算最近的周年（闰年 2/29 用 runCatching 兜底） */
private fun nextAnniversary(start: LocalDate?): String {
    if (start == null) return "先设置开始日"
    val today = LocalDate.now()
    var year = today.year
    var candidate = runCatching { LocalDate.of(year, start.monthValue, start.dayOfMonth) }.getOrNull()
        ?: return "—"
    if (!candidate.isAfter(today)) {
        year += 1
        candidate = runCatching { LocalDate.of(year, start.monthValue, start.dayOfMonth) }.getOrNull()
            ?: return "—"
    }
    return candidate.toString()
}
