package com.mulin.us.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.R
import com.mulin.us.domain.model.TimelineEntry
import com.mulin.us.domain.model.TimelineSource
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.animation.SoftReveal
import com.mulin.us.ui.animation.StarTwinkle
import com.mulin.us.ui.components.IllustrationView
import com.mulin.us.ui.components.StartDatePickerDialog
import com.mulin.us.ui.components.StorybookButton
import com.mulin.us.ui.components.StorybookCard
import com.mulin.us.ui.components.StorybookEmptyState
import com.mulin.us.ui.components.YuniAvatar
import com.mulin.us.ui.components.YuniFeatureIcon
import com.mulin.us.ui.components.YuniTone
import com.mulin.us.ui.components.YuniTones
import com.mulin.us.ui.theme.UsColor
import com.mulin.us.ui.theme.UsDockInset
import com.mulin.us.ui.theme.UsRadius
import com.mulin.us.ui.theme.UsSpacing
import com.mulin.us.ui.theme.UsBrand
import com.mulin.us.ui.theme.UsDayNumber
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * 首页 A（V3）：参考绘本布局。
 * 主图 2.1:1 → 天数核心卡 → 今日互动卡 → 我们的回忆 → 快捷操作。
 * 业务数据仍只来自 ViewModel / TimelineRepository，无第二数据源。
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
    val couple by vm.couple.collectAsState()
    val user by vm.currentUser.collectAsState()
    val recent by vm.recentTimeline.collectAsState()
    var showDatePicker by remember { mutableStateOf(false) }

    val startDate = couple?.startDate
    val days = startDate?.let { ChronoUnit.DAYS.between(it, LocalDate.now()) }?.coerceAtLeast(0)
    val bound = couple?.isBound == true

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = UsSpacing.page),
        verticalArrangement = Arrangement.spacedBy(UsSpacing.module)
    ) {
        Spacer(Modifier.height(4.dp))

        // ---------- 品牌区 ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("予你", style = UsBrand, color = UsColor.Accent)
                Text("Us · 两个人的私密空间", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(UsRadius.pill))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Settings, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
                Text("设置", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            }
        }

        // ---------- 绘本主图 2.1:1 ----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(2.1f)
                .clip(RoundedCornerShape(UsRadius.card))
        ) {
            IllustrationView(
                drawableRes = R.drawable.home_story_illustration,
                contentDescription = "星空下的金发旅行者与玻璃罩玫瑰",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            // 左侧提亮层：保证文案可读，不遮挡右侧主体
            Box(
                Modifier.fillMaxSize().background(
                    Brush.horizontalGradient(
                        listOf(
                            UsColor.Surface.copy(alpha = 0.86f),
                            UsColor.Surface.copy(alpha = 0.46f),
                            Color.Transparent
                        )
                    )
                )
            )
            Column(modifier = Modifier.padding(UsSpacing.inner)) {
                Text(
                    "「 与你在一起，\n    就是最好的时光 」",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 21.sp
                )
                Spacer(Modifier.weight(1f))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YuniAvatar(
                        imagePath = user?.avatarLocalPath,
                        label = (user?.nickname?.take(1) ?: "我").uppercase(),
                        contentDescription = "我的头像"
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("&", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.width(8.dp))
                    YuniAvatar(
                        imagePath = null,
                        label = if (bound) "TA" else "?",
                        soft = !bound,
                        containerColor = if (bound) MaterialTheme.colorScheme.secondaryContainer else null,
                        contentColor = if (bound) MaterialTheme.colorScheme.onSecondaryContainer else null,
                        contentDescription = if (bound) "另一半头像位" else "等待另一半加入"
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (bound) "${user?.nickname ?: "我"} & TA" else user?.nickname ?: "先建立本机档案",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            StarTwinkle(modifier = Modifier.padding(top = 12.dp, end = 12.dp).align(Alignment.TopEnd))
        }

        // ---------- 恋爱天数核心卡 ----------
        StorybookCard(accent = UsColor.Primary, wash = UsColor.PrimaryContainer) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("我们已经在一起", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            if (days == null) "—" else days.toString(),
                            style = UsDayNumber,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("天", style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 14.dp))
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("一起的日子", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .clip(RoundedCornerShape(UsRadius.pill))
                            .background(UsColor.PrimaryContainer)
                            .padding(horizontal = 12.dp, vertical = 5.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(startDate?.toString() ?: "未设置", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        startDate?.dayOfWeek?.getDisplayName(TextStyle.FULL, Locale.CHINESE) ?: "—",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                if (startDate == null) "点这里写下恋爱开始日" else "相恋 · 自 $startDate",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { showDatePicker = true }.padding(vertical = 4.dp)
            )
            Spacer(Modifier.height(12.dp))
            Box(Modifier.height(1.dp).fillMaxWidth().background(UsColor.Divider))
            Spacer(Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                InfoCell("下一个纪念日", nextAnniversary(startDate), Modifier.weight(1f))
                InfoCell("下一次见面", "还没有计划", Modifier.weight(1f))
            }
        }

        // ---------- 今日互动 ----------
        StorybookCard(accent = UsColor.Primary, wash = UsColor.LavenderPale) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("今日互动", style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("每日问答 · 悄悄话", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Icon(Icons.Filled.DateRange, contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(UsRadius.card))
                        .background(UsColor.PrimaryContainer)
                        .padding(horizontal = 16.dp, vertical = 18.dp)
                ) {
                    Text("今天也要一起呀 \u2665", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary)
                }
                Spacer(Modifier.width(12.dp))
                IllustrationView(
                    drawableRes = R.drawable.mailbox_envelope_illustration,
                    contentDescription = "玻璃罩玫瑰装饰",
                    modifier = Modifier.size(84.dp)
                )
            }
            Spacer(Modifier.height(14.dp))
            StorybookButton("进入今日互动", onOpenInteraction, secondary = true)
        }

        // ---------- 我们的回忆 ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("我们的回忆", style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("查看全部 ›", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenTimeline() }.padding(4.dp))
        }
        if (recent.isEmpty()) {
            StorybookEmptyState(
                icon = Icons.Filled.Star,
                title = "这一页还是空白的",
                message = "写下第一条回忆，我们的故事就开始啦。"
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(UsSpacing.cardGap)) {
                recent.take(3).forEachIndexed { index, entry ->
                    SoftReveal(visible = true, delayMillis = index * 60) {
                        RecentRow(entry)
                    }
                }
            }
        }

        // ---------- 快捷操作 ----------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(UsSpacing.cardGap)) {
            QuickAction("写回忆", Icons.Filled.Star, Modifier.weight(1f), onWriteMemory)
            QuickAction("写日记", Icons.Filled.Favorite, Modifier.weight(1f), onWriteDiary)
            QuickAction("写封信", Icons.Filled.Email, Modifier.weight(1f), onOpenMailbox)
        }

        Spacer(Modifier.height(UsDockInset.TailSpacer))
    }

    if (showDatePicker) {
        StartDatePickerDialog(
            initialDate = startDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { iso -> vm.confirmStartDate(iso); showDatePicker = false }
        )
    }
}

@Composable
private fun InfoCell(label: String, value: String, modifier: Modifier) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RecentRow(entry: TimelineEntry) {
    StorybookCard(
        accent = entry.sourceType.tone().icon,
        wash = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            YuniFeatureIcon(entry.sourceType.icon(), entry.sourceType.tone())
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.sourceType.label(), fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.background(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                            RoundedCornerShape(6.dp)
                        ).padding(horizontal = 6.dp, vertical = 2.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(entry.eventAt.toString(), fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(6.dp))
                Text(entry.title, style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(entry.summary.take(40), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
            }
        }
    }
}

private fun TimelineSource.icon() = when (this) {
    TimelineSource.MEMORY, TimelineSource.WISH -> Icons.Filled.Star
    TimelineSource.DIARY -> Icons.Filled.Person
    TimelineSource.MEETING -> Icons.Filled.DateRange
    TimelineSource.ANNIVERSARY, TimelineSource.QUESTION -> Icons.Filled.Favorite
    TimelineSource.LETTER -> Icons.Filled.Email
}

private fun TimelineSource.tone(): YuniTone = when (this) {
    TimelineSource.MEMORY -> YuniTones.Pink
    TimelineSource.DIARY -> YuniTones.Green
    TimelineSource.MEETING -> YuniTones.Purple
    TimelineSource.ANNIVERSARY -> YuniTones.Pink
    TimelineSource.QUESTION -> YuniTones.Blue
    TimelineSource.WISH -> YuniTones.Orange
    TimelineSource.LETTER -> YuniTones.Teal
}

private fun TimelineSource.label() = when (this) {
    TimelineSource.MEMORY -> "回忆"
    TimelineSource.DIARY -> "日记"
    TimelineSource.MEETING -> "见面"
    TimelineSource.ANNIVERSARY -> "纪念日"
    TimelineSource.QUESTION -> "问答"
    TimelineSource.WISH -> "愿望"
    TimelineSource.LETTER -> "信"
}

@Composable
private fun QuickAction(
    label: String, icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier, onClick: () -> Unit
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(UsRadius.card))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface)
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
