package com.mulin.us.ui.timeline

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.mulin.us.domain.model.TimelineEntry
import com.mulin.us.domain.model.TimelineFilter
import com.mulin.us.domain.model.TimelineSource
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.components.PaperCard
import com.mulin.us.ui.components.StorybookEmptyState
import com.mulin.us.ui.components.YuniCard
import com.mulin.us.ui.theme.UsSpacing
import com.mulin.us.ui.theme.YuniDockInset
import com.mulin.us.ui.theme.YuniMotion
import java.time.LocalDate
import java.time.YearMonth

/**
 * 回忆页 = 完整时间线（工单 V3.1 阶段 C）。
 *
 * 竖向时间轴 + 混合卡片：重要回忆大图卡、纯文字紧凑卡、纪念日事件卡。
 * 顶部：年份切换 / 月份定位 / 类型筛选（全部/照片/日记/见面/纪念日/互动/其他）。
 * 数据全部来自 TimelineRepository（与首页最近记录同源）。
 */
@Composable
fun TimelineScreen(
    vm: YuniViewModel,
    onOpenDiary: () -> Unit,
    onOpenMemories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val all by vm.timeline.collectAsState()
    // Tab 切换时 Navigation 会保存目的地状态；用 rememberSaveable 才能恢复筛选条件
    var year by rememberSaveable { mutableStateOf<Int?>(null) }
    var month by rememberSaveable { mutableStateOf<Int?>(null) }
    var filter by rememberSaveable { mutableStateOf(TimelineFilter.ALL) }
    var showMoreFilters by rememberSaveable { mutableStateOf(false) }

    val primaryFilters = listOf(
        TimelineFilter.ALL,
        TimelineFilter.PHOTO,
        TimelineFilter.DIARY,
        TimelineFilter.MEETING
    )
    val moreFilters = listOf(
        TimelineFilter.ANNIVERSARY,
        TimelineFilter.INTERACTION,
        TimelineFilter.OTHER
    )
    val activeMoreFilter = filter in moreFilters

    val filtered = all.filter { e ->
        (year == null || e.eventAt.year == year) &&
            (month == null || e.eventAt.monthValue == month) &&
            (filter == TimelineFilter.ALL || e.sourceType.filterGroup == filter)
    }

    // 按年月分组（时间轴分组头）
    val grouped = filtered.groupBy { YearMonth.from(it.eventAt) }
        .toSortedMap(compareByDescending { it })

    fun shiftMonth(delta: Long) {
        val base = YearMonth.of(year ?: LocalDate.now().year, month ?: LocalDate.now().monthValue)
        val target = base.plusMonths(delta)
        year = target.year
        month = target.monthValue
    }

    Column(modifier = modifier.fillMaxSize()) {
        // ---------- 顶部导航：年份 / 常用筛选 / 月份 ----------
        Column(modifier = Modifier.padding(horizontal = UsSpacing.page, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("回忆", fontSize = 22.sp, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground, modifier = Modifier.weight(1f))
                TextButtonLike("全部") {
                    year = null; month = null; filter = TimelineFilter.ALL; showMoreFilters = false
                }
                Spacer(Modifier.width(8.dp))
                TextButtonLike(if (year == null) "年份" else "$year") {
                    year = if (year == null) LocalDate.now().year else null
                    if (year != null) month = null
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                primaryFilters.forEach { f ->
                    FilterChip(f, selected = filter == f) {
                        filter = f
                        showMoreFilters = false
                    }
                }
                FilterChip(
                    label = if (activeMoreFilter) filterLabel(filter) else "更多",
                    selected = showMoreFilters || activeMoreFilter
                ) {
                    if (activeMoreFilter) {
                        filter = TimelineFilter.ALL
                        showMoreFilters = false
                    } else {
                        showMoreFilters = !showMoreFilters
                    }
                }
            }
            if (showMoreFilters) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    // 已选中的扩展项在「更多」按钮上已经显示，不再重复画一遍
                    moreFilters.filter { it != filter }.forEach { f ->
                        FilterChip(f, selected = filter == f) { filter = f }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButtonLike("‹") { shiftMonth(-1) }
                Spacer(Modifier.width(20.dp))
                TextButtonLike(if (month == null) "全部月份" else "${month}月") { month = null }
                Spacer(Modifier.width(20.dp))
                TextButtonLike("›") { shiftMonth(1) }
            }
        }

        // ---------- 时间轴列表 ----------
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = UsSpacing.page),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (grouped.isEmpty()) {
                item {
                    PaperCard(tonal = true) {
                        StorybookEmptyState(
                            icon = Icons.Filled.Star,
                            title = "还没有共同记录",
                            message = "写下第一条回忆，这里就会慢慢热闹起来。"
                        )
                    }
                }
            }
            grouped.forEach { (ym, entries) ->
                item(key = "header-$ym") {
                    Row(verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 8.dp)) {
                        Box(Modifier.size(8.dp).clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary))
                        Spacer(Modifier.width(8.dp))
                        Text("${ym.year} 年 ${ym.monthValue} 月", fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                items(entries, key = { it.id }) { entry ->
                    TimelineRow(entry)
                }
            }
            item { Spacer(Modifier.height(YuniDockInset.TailSpacer)) }
        }
    }
}

@Composable
private fun TimelineRow(entry: TimelineEntry) {
    val rose = MaterialTheme.colorScheme.primary
    val sub = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = Modifier.fillMaxWidth()) {
        // 时间轴竖线 + 圆点
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(rose))
            Box(Modifier.width(2.dp).height(80.dp).background(rose.copy(alpha = 0.2f)))
        }
        Spacer(Modifier.width(10.dp))
        YuniCard(modifier = Modifier.weight(1f)) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(entry.eventAt.toString(), fontSize = 12.sp, color = sub,
                        modifier = Modifier.weight(1f))
                    entry.badge?.let {
                        Text(it, fontSize = 11.sp, color = rose)
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(entry.title, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(3.dp))
                Text(entry.summary, fontSize = 13.sp, color = sub, maxLines = 3)
                // 大图卡片：有图片才显示（阶段 B 暂无图片，留扩展位）
                entry.imagePath?.let { path ->
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = java.io.File(path), contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxWidth().height(160.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterChip(filter: TimelineFilter, selected: Boolean, onClick: () -> Unit) {
    FilterChip(label = filterLabel(filter), selected = selected, onClick = onClick)
}

private fun filterLabel(filter: TimelineFilter): String = when (filter) {
    TimelineFilter.ALL -> "全部"
    TimelineFilter.PHOTO -> "照片"
    TimelineFilter.DIARY -> "日记"
    TimelineFilter.MEETING -> "见面"
    TimelineFilter.ANNIVERSARY -> "纪念日"
    TimelineFilter.INTERACTION -> "互动"
    TimelineFilter.OTHER -> "其他"
}

@Composable
private fun FilterChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(YuniMotion.Fast), label = "filterBg"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(YuniMotion.Fast), label = "filterFg"
    )
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(label, fontSize = 11.sp, color = fg)
    }
}

@Composable
private fun TextButtonLike(text: String, onClick: () -> Unit) {
    Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.clickable(onClick = onClick).padding(4.dp))
}
