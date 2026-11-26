package com.mulin.us.ui.diary

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.components.YuniTones
import com.mulin.us.ui.theme.YuniDockInset
import java.time.LocalDate
import java.time.YearMonth

/**
 * 轻记最新日记页（MoodDiaryContent）骨架移植，首页与回忆页共用。
 *
 * 顺序按用户要求：日历（最上）→ 写一篇日记/回忆 → 本月心情 → 日记/回忆记录（最下）。
 * 月历格子按当天评分四档着色；没写的日子透明底（轻记 v?：不要灰色块）。
 *
 * @param entries 条目列表，[entryDate] 取日期、[entryRating] 取 1-5 星、[entryPreview] 取摘要
 */
@Composable
fun DiaryPageContent(
    title: String,
    writeLabel: String,
    recordTitle: String,
    emptyHint: String,
    entries: List<DiaryLike>,
    month: YearMonth,
    onMonthChange: (YearMonth) -> Unit,
    onWrite: () -> Unit,
    onEntryClick: (DiaryLike) -> Unit,
    onDelete: (DiaryLike) -> Unit,
    modifier: Modifier = Modifier,
    header: (@Composable () -> Unit)? = null
) {
    var queryDate by remember { mutableStateOf(LocalDate.now()) }

    val ratingByDate = remember(entries) {
        entries.groupBy { it.date }.mapValues { (_, list) -> list.maxOf { it.rating } }
    }
    val monthEntries = entries.filter { YearMonth.from(it.date) == month }
    val avg = if (monthEntries.isEmpty()) "--"
            else "%.1f / 5".format(monthEntries.map { it.rating }.average())
    val streak = currentStreak(ratingByDate.keys)
    val dateColors = remember(month, ratingByDate) {
        ratingByDate.filterKeys { YearMonth.from(it) == month }
            .mapValues { (_, r) -> moodColor(r) }
    }

    val rose = MaterialTheme.colorScheme.primary
    val roseSoft = MaterialTheme.colorScheme.primaryContainer
    val sub = MaterialTheme.colorScheme.onSurfaceVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (header != null) {
            header()
            Spacer(Modifier.height(4.dp))
        }

        // ---------- 1. 日历（最上）----------
        YuniCard {
            CalendarCard(month, dateColors, onMonthChange) { queryDate = it }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                MoodLegend("低", roseSoft)
                MoodLegend("中", rose.copy(alpha = 0.55f))
                MoodLegend("高", rose)
            }
        }

        // ---------- 2. 写一篇日记/回忆（日历下）----------
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(rose)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) { onWrite() }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(writeLabel, fontSize = 15.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onPrimary)
        }

        // ---------- 3. 本月心情（两个下面）----------
        YuniCard {
            Text("本月心情", fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(10.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                SummaryItem("${monthEntries.size} 篇", "篇数", Modifier.weight(1f), rose)
                SummaryItem(avg, "平均评分", Modifier.weight(1f), rose)
                SummaryItem("$streak 天", "连续记录", Modifier.weight(1f), rose)
            }
        }

        // ---------- 4. 记录（最下，按日查询）----------
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(recordTitle, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.weight(1f))
            Text("‹", fontSize = 20.sp, color = sub,
                modifier = Modifier.noRipple { queryDate = queryDate.minusDays(1) }
                    .padding(horizontal = 8.dp))
            Text(
                if (queryDate == LocalDate.now()) "今天" else queryDate.toString(),
                fontSize = 12.sp, color = rose, modifier = Modifier.padding(4.dp)
            )
            Text("›", fontSize = 20.sp, color = sub,
                modifier = Modifier.noRipple { queryDate = queryDate.plusDays(1) }
                    .padding(horizontal = 8.dp))
        }

        Box(modifier = Modifier.animateContentSize(tween(200, easing = FastOutSlowInEasing))) {
            val dayEntries = entries.filter { it.date == queryDate }
            if (dayEntries.isEmpty()) {
                YuniCard {
                    Text(emptyHint, fontSize = 14.sp, color = sub)
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    dayEntries.forEach { e ->
                        YuniCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(e.date.toString(), fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface)
                                    Spacer(Modifier.height(3.dp))
                                    Text(e.preview, fontSize = 11.sp, color = sub, maxLines = 2)
                                }
                                Text(moodLabel(e.rating), fontSize = 12.sp, color = rose)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = { onEntryClick(e) }) { Text("明细") }
                                TextButton(onClick = { onDelete(e) }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(YuniDockInset.TailSpacer))
    }
}

/** 列表条目的统一抽象：日记与回忆都塞进这个结构 */
data class DiaryLike(
    val id: Long,
    val date: LocalDate,
    val rating: Int,
    val preview: String,
    val content: String,
    val raw: Any? = null
)

@Composable
private fun CalendarCard(
    month: YearMonth,
    dateColors: Map<LocalDate, Color>,
    onMonthChange: (YearMonth) -> Unit,
    onSelectDate: (LocalDate) -> Unit
) {
    val rose = MaterialTheme.colorScheme.primary
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    Text("${month.year} 年 ${month.monthValue} 月", fontSize = 16.sp,
        fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("上个月", fontSize = 12.sp, color = rose,
            modifier = Modifier.noRipple { onMonthChange(month.minusMonths(1)) }
                .padding(horizontal = 8.dp, vertical = 4.dp))
        Text("下个月", fontSize = 12.sp, color = rose,
            modifier = Modifier.noRipple { onMonthChange(month.plusMonths(1)) }
                .padding(horizontal = 8.dp, vertical = 4.dp))
    }
    Spacer(Modifier.height(6.dp))
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("一", "二", "三", "四", "五", "六", "日").forEach {
            Text(it, fontSize = 11.sp, color = sub, textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f))
        }
    }
    val first = month.atDay(1)
    val leading = (first.dayOfWeek.value - 1 + 7) % 7
    val days = month.lengthOfMonth()
    val total = ((leading + days + 6) / 7) * 7
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        for (row in 0 until total / 7) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val idx = row * 7 + col
                    val day = idx - leading + 1
                    val date = if (day in 1..days) month.atDay(day) else null
                    val bg = if (date != null) dateColors[date] else null
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bg ?: Color.Transparent)
                            .then(if (date != null) Modifier.noRipple { onSelectDate(date) } else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (date != null) Text(day.toString(), fontSize = 12.sp,
                            color = if (bg != null) MaterialTheme.colorScheme.onPrimary else sub)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(value: String, label: String, modifier: Modifier, color: Color) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = color)
        Spacer(Modifier.height(2.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MoodLegend(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(horizontal = 6.dp)) {
        Box(Modifier.size(9.dp).clip(CircleShape).background(color))
        Spacer(Modifier.size(4.dp))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Modifier.noRipple(onClick: () -> Unit): Modifier = this.clickable(
    interactionSource = remember { MutableInteractionSource() },
    indication = null,
    onClick = onClick
)

/** 评分四档颜色：低=浅粉，中=中粉，高=主色 */
private fun moodColor(rating: Int): Color {
    // 返回的是占位色，真正颜色在 Composable 里按主题取；这里用固定近似值即可
    return when {
        rating <= 0 -> Color.Transparent
        rating <= 2 -> Color(0xFFF2E4E8)
        rating == 3 -> Color(0xFFD9A3AF)
        else -> Color(0xFFB4556A)
    }
}

private fun moodLabel(rating: Int): String = when {
    rating <= 0 -> "未评分"
    rating <= 2 -> "有点糟"
    rating == 3 -> "一般"
    rating == 4 -> "不错"
    else -> "很好"
}

private fun currentStreak(dates: Set<LocalDate>): Int {
    var day = LocalDate.now()
    if (day !in dates) day = day.minusDays(1)
    var count = 0
    while (day in dates) { count++; day = day.minusDays(1) }
    return count
}
