package com.mulin.us.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.domain.model.TimelineEntry
import com.mulin.us.domain.model.TimelineSource
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.StartDatePickerDialog
import com.mulin.us.ui.components.YuniAvatar
import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.components.YuniFeatureIcon
import com.mulin.us.ui.components.YuniTone
import com.mulin.us.ui.components.YuniTones
import com.mulin.us.ui.theme.YuniDockInset
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 首页 A：温暖陪伴型（工单 V3.1 C1~C7）。
 *
 * 信息层级：品牌 + 关系卡 → 今日互动 → 最近发生的事 → 快捷操作。
 * 明确移除 ECS 跳板/云端调试文本/待上传列表/同步按钮（工单 C7）。
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
            .padding(horizontal = 16.dp)
            .statusBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        // ---------- 第一层：品牌 + 关系卡 ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("予你", fontSize = 26.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground)
                Text("Us · 两个人的私密空间", fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("设置", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenSettings() }.padding(4.dp))
        }

        YuniCard {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    YuniAvatar(
                        imagePath = user?.avatarLocalPath,
                        label = (user?.nickname?.take(1) ?: "我").uppercase(),
                        contentDescription = "我的头像"
                    )
                    Text("&", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp))
                    // 另一半加入后这里就是独立头像位；当前 ECS 跳板未同步头像，先用 TA 占位。
                    YuniAvatar(
                        imagePath = null,
                        label = if (bound) "TA" else "?",
                        soft = !bound,
                        containerColor = if (bound) MaterialTheme.colorScheme.secondaryContainer else null,
                        contentColor = if (bound) MaterialTheme.colorScheme.onSecondaryContainer else null,
                        contentDescription = if (bound) "另一半头像位" else "等待另一半加入"
                    )
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            if (bound) "我们已经在一起了" else "等待另一半加入",
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            if (bound) "${user?.nickname ?: "我"} & TA" else user?.nickname ?: "先建立本机档案",
                            fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        if (days == null) "—" else days.toString(),
                        fontSize = 44.sp, fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(6.dp))
                    Text("天", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp))
                }
                Text(
                    if (startDate == null) "点这里写下恋爱开始日" else "相恋 · 自 $startDate",
                    fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showDatePicker = true }.padding(vertical = 4.dp)
                )

                Spacer(Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth()) {
                    InfoCell("下一个纪念日", nextAnniversary(startDate), Modifier.weight(1f))
                    InfoCell("下一次见面", "还没有计划", Modifier.weight(1f))
                }
            }
        }

        // ---------- 第二层：今日互动 ----------
        YuniCard {
            Row(modifier = Modifier.clickable { onOpenInteraction() },
                verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Favorite, contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("今日互动", fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface)
                    Text("每日问答 · 悄悄话", fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text("›", fontSize = 20.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        // ---------- 第三层：最近发生的事 ----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("最近发生的事", fontSize = 17.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("查看全部 ›", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable { onOpenTimeline() }.padding(4.dp))
        }
        if (recent.isEmpty()) {
            YuniCard {
                Text("还没有共同记录。写下第一条回忆吧。", fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                recent.take(3).forEach { RecentRow(it) }
            }
        }

        // ---------- 第四层：快捷操作 ----------
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickAction("写回忆", Icons.Filled.Star, Modifier.weight(1f), onWriteMemory)
            QuickAction("写日记", Icons.Filled.Favorite, Modifier.weight(1f), onWriteDiary)
            QuickAction("写封信", Icons.Filled.Email, Modifier.weight(1f), onOpenMailbox)
        }

        Spacer(Modifier.height(YuniDockInset.TailSpacer))
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
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(4.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun RecentRow(entry: TimelineEntry) {
    YuniCard {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
                Text(entry.title, fontSize = 15.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface, maxLines = 1)
                Spacer(Modifier.height(4.dp))
                Text(entry.summary.take(40), fontSize = 12.sp,
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
        modifier = modifier.clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(6.dp))
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface)
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
