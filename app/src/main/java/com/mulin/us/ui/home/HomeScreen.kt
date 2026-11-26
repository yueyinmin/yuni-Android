package com.mulin.us.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
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
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.StartDatePickerDialog
import com.mulin.us.ui.components.YuniCard
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
                    Avatar(initial = (user?.nickname?.take(1) ?: "我").uppercase())
                    Text("&", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 12.dp))
                    Avatar(initial = if (bound) "TA" else "?", soft = !bound)
                    Column(modifier = Modifier.padding(start = 12.dp)) {
                        Text(
                            if (bound) "我们已经在一起了" else "等待另一半加入",
                            fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(user?.nickname ?: "先建立本机档案", fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
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
            recent.take(3).forEach { RecentRow(it) }
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
private fun Avatar(initial: String, soft: Boolean = false) {
    Box(
        modifier = Modifier.size(48.dp).clip(CircleShape).background(
            if (soft) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.primaryContainer
        ),
        contentAlignment = Alignment.Center
    ) {
        Text(initial, fontSize = 18.sp, fontWeight = FontWeight.Bold,
            color = if (soft) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.primary)
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
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text("记", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(entry.title, fontSize = 14.sp, fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface)
                Text("${entry.eventAt} · ${entry.summary.take(24)}", fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
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
