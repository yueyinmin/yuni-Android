package com.mulin.us.ui.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.mulin.us.ui.YuniViewModel
import com.mulin.us.ui.diary.DiaryEditSheet
import com.mulin.us.ui.diary.DiaryLike
import com.mulin.us.ui.diary.DiaryPageContent
import java.time.LocalDate
import java.time.YearMonth

/**
 * 首页（Tab 1）—— 轻记最新日记页布局：
 * 日历（最上）→ 写一篇日记 → 本月心情 → 日记记录（最下）。
 */
@Composable
fun HomeScreen(vm: YuniViewModel, modifier: Modifier = Modifier) {
    val diaries by vm.diaries.collectAsState()
    var month by remember { mutableStateOf(YearMonth.now()) }
    var showEdit by remember { mutableStateOf(false) }

    val entries = diaries.map { d ->
        DiaryLike(
            id = d.localId,
            date = LocalDate.ofEpochDay(d.entryEpochDay),
            rating = d.mood?.takeWhile { it.isDigit() }?.toIntOrNull() ?: 3,
            preview = d.content,
            content = d.content,
            raw = d
        )
    }

    DiaryPageContent(
        title = "日记",
        writeLabel = "写一篇日记",
        recordTitle = "日记记录",
        emptyHint = "这天没有日记。点上面的「写一篇日记」，记下心情怎么样。",
        entries = entries,
        month = month,
        onMonthChange = { month = it },
        onWrite = { showEdit = true },
        onEntryClick = { },
        onDelete = { like ->
            (like.raw as? com.mulin.us.domain.model.Diary)?.let { vm.deleteDiary(it.localId) }
        },
        modifier = modifier.fillMaxSize()
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
}
