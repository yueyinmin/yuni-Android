package com.mulin.us.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mulin.us.R
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

/** 首页天数计算：所有日期展示共用同一个 today，避免跨零点不一致。 */
internal fun homeDaysTogether(startDate: LocalDate?, today: LocalDate): Long? =
    startDate?.let { ChronoUnit.DAYS.between(it, today) }?.coerceAtLeast(0)

/**
 * 首页：只做页面组合与状态映射。
 * 业务数据仍来自 YuniViewModel / Repository；本文件不直接访问 DAO 或网络。
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
    val todayQuestion by vm.todayQuestion.collectAsStateWithLifecycle()

    var showDatePicker by remember { mutableStateOf(false) }
    var selectedMemory by remember { mutableStateOf<MemoryPreviewUiModel?>(null) }
    var today by remember { mutableStateOf(LocalDate.now()) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                today = LocalDate.now()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) { vm.loadTodayQuestion() }

    val startDate = couple?.startDate
    val days = homeDaysTogether(startDate, today)
    val bound = couple?.isBound == true
    val todayText = today.toString()
    val todayWeekdayText = today.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.CHINESE)
    val nickname = user?.nickname ?: "先建立本机档案"
    val initial = user?.nickname?.take(1)?.uppercase() ?: "你"

    val interactionText = when {
        todayQuestion?.myAnswer != null -> "今日互动已完成 ♥"
        todayQuestion != null -> todayQuestion!!.content
        else -> "今天也要一起呀 ♥"
    }

    val memory = recent.firstOrNull()?.let { entry ->
        MemoryPreviewUiModel(
            id = entry.id,
            dateText = entry.eventAt.toString(),
            title = entry.title,
            summary = entry.summary,
            coverUrl = entry.imagePath
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        // 首页专属暖粉水彩底，不影响其他四个 Tab
        Image(
            painter = painterResource(R.drawable.bg_home_soft),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = UsSpacing.page),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            HomeHeaderBar(myInitial = initial, onSettingsClick = onOpenSettings)

            HeroCoupleBanner(
                quote = "与你在一起，\n就是最好的时光",
                myAvatarPath = null,
                myInitial = initial,
                myNickname = nickname,
                partnerBound = bound,
                onInvitePartner = onOpenSettings
            )

            AnniversaryCounterCard(
                days = days,
                startDate = startDate?.toString(),
                todayDateText = todayText,
                todayWeekdayText = todayWeekdayText,
                onEditDate = { showDatePicker = true }
            )

            DailyInteractionCard(
                dailyNote = interactionText,
                onOpenInteraction = onOpenInteraction
            )

            MemoriesPreviewCard(
                memory = memory,
                onOpenMemory = { selectedMemory = it },
                onOpenAll = onOpenTimeline,
                onAddMemory = onWriteMemory
            )

            // Dock 62dp + 底部 22dp + 视觉间距 16dp + 系统导航栏 Insets
            Spacer(Modifier.height(100.dp).navigationBarsPadding())
        }
    }

    if (showDatePicker) {
        StartDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { iso -> vm.confirmStartDate(iso); showDatePicker = false }
        )
    }

    selectedMemory?.let { item ->
        AlertDialog(
            onDismissRequest = { selectedMemory = null },
            title = { Text(item.title) },
            text = {
                Column {
                    Text(item.dateText)
                    Text(item.summary.ifBlank { "还没有写下描述" })
                }
            },
            confirmButton = { TextButton(onClick = { selectedMemory = null }) { Text("关闭") } }
        )
    }
}
