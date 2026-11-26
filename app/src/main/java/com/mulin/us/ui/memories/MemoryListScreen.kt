package com.mulin.us.ui.memories

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.mulin.us.domain.model.Diary
import com.mulin.us.domain.model.Memory
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.diary.DiaryEditSheet
import com.mulin.us.ui.diary.DiaryLike
import com.mulin.us.ui.diary.DiaryPageContent
import com.mulin.us.ui.theme.YuniMotion
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId

/**
 * 回忆（Tab 2）—— 轻记日记页胶囊切换：
 * 「心情日记 | 共同回忆」，右边「共同回忆」点击切换。
 * 两个页面同布局（日历→写一条→本月心情→记录），共用 DiaryPageContent。
 */
@Composable
fun MemoryListScreen(
    vm: YuniViewModel,
    onOpenDiary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var shared by remember { mutableStateOf(false) }
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showEdit by remember { mutableStateOf(false) }

    if (!shared) {
        val diaries by vm.diaries.collectAsState()
        val entries = diaries.map { d ->
            DiaryLike(
                id = d.localId,
                date = LocalDate.ofEpochDay(d.entryEpochDay),
                rating = d.mood?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 3,
                preview = d.content, content = d.content, raw = d
            )
        }
        DiaryPageContent(
            title = "心情日记", writeLabel = "写一篇日记", recordTitle = "日记记录",
            emptyHint = "这天没有日记。点上面的「写一篇日记」，记下心情怎么样。",
            entries = entries, month = month, onMonthChange = { month = it },
            onWrite = { showEdit = true }, onEntryClick = {},
            onDelete = { like -> (like.raw as? Diary)?.let { vm.deleteDiary(it.localId) } },
            modifier = modifier,
            header = { DiaryTabHeader(shared) { shared = it } }
        )
        if (showEdit) {
            DiaryEditSheet(
                title = "写一篇日记",
                onDismiss = { showEdit = false },
                onSave = { date, rating, content, photos ->
                    vm.writeDiaryWithMood(date, rating, content, photos)
                    showEdit = false
                }
            )
        }
    } else {
        val memories by vm.memories.collectAsState()
        val entries = memories.map { m ->
            DiaryLike(
                id = m.localId,
                date = m.happenedAt?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalDate() }
                    ?: LocalDate.ofEpochDay(m.createdAt / 86_400_000L),
                rating = m.mood?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 3,
                preview = m.content, content = m.content, raw = m
            )
        }
        DiaryPageContent(
            title = "共同回忆", writeLabel = "写一篇回忆", recordTitle = "我的回忆",
            emptyHint = "这天没有回忆。点上面的「写一篇回忆」，记下这一天。",
            entries = entries, month = month, onMonthChange = { month = it },
            onWrite = { showEdit = true }, onEntryClick = {},
            onDelete = { like -> (like.raw as? Memory)?.let { vm.deleteMemory(it.localId) } },
            modifier = modifier,
            header = { DiaryTabHeader(shared) { shared = it } }
        )
        if (showEdit) {
            DiaryEditSheet(
                title = "写一篇回忆",
                onDismiss = { showEdit = false },
                onSave = { date, rating, content, photos ->
                    vm.writeMemoryWithMood(date, rating, content, photos)
                    showEdit = false
                }
            )
        }
    }
}

@Composable
private fun DiaryTabHeader(shared: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clip(RoundedCornerShape(999.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        TabPill("心情日记", selected = !shared, modifier = Modifier.weight(1f)) { onChange(false) }
        TabPill("共同回忆", selected = shared, modifier = Modifier.weight(1f)) { onChange(true) }
    }
}

@Composable
private fun TabPill(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val bg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        animationSpec = tween(YuniMotion.Fast), label = "tabPill"
    )
    val fg by animateColorAsState(
        if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = tween(YuniMotion.Fast), label = "tabText"
    )
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal, color = fg)
    }
}
