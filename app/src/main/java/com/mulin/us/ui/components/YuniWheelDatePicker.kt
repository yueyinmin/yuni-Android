package com.mulin.us.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

/**
 * 轻记滚轮日期选择器（W3.1 工单 C3.2）——源码复制适配，独立存在于 Us-Android。
 *
 * 来源（只读参考，未修改轻记工程）：
 * - `ui/components/ExerciseWheelPickers.kt:68-139`  NumberWheelColumn
 * - `ui/components/WheelTimePicker.kt:39-87`       WheelDatePicker
 *
 * 保留的交互（轻记 v3.3.1 修正）：
 * - 前后各补 2 个空格子，中心项 = firstVisibleItemIndex（不是 +2，否则首尾偏格）
 * - 松手吸附 + 回调
 * - 三列独立滚动，只回传年/月/日三个数字，合法性交给调用方
 * - 首次组合跳过回传（不自动保存）
 *
 * 适配：主题色 → MaterialTheme；年范围扩到近 80 年（可补录真实恋爱开始日）。
 */
@Composable
fun YuniWheelDatePicker(
    initialDate: LocalDate,
    modifier: Modifier = Modifier,
    yearRange: IntRange = IntRange(LocalDate.now().year - 80, LocalDate.now().year),
    onSelected: (year: Int, month: Int, day: Int) -> Unit
) {
    var year = initialDate.year.coerceIn(yearRange.first, yearRange.last)
    var month = initialDate.monthValue
    var day = initialDate.dayOfMonth

    // 用 remember 包状态（复制轻记时使用 mutableStateOf，这里保持同等语义）
    val yState = remember { androidx.compose.runtime.mutableIntStateOf(year) }
    val mState = remember { androidx.compose.runtime.mutableIntStateOf(month) }
    val dState = remember { androidx.compose.runtime.mutableIntStateOf(day) }
    val ready = remember { androidx.compose.runtime.mutableStateOf(false) }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        DateWheelColumn("年", yearRange.map { "${it}年" },
            yState.value - yearRange.first, Modifier.weight(1f)) { i ->
            yState.value = yearRange.first + i
        }
        DateWheelColumn("月", (1..12).map { "${it}月" },
            mState.value - 1, Modifier.weight(1f)) { mState.value = it + 1 }
        DateWheelColumn("日", (1..31).map { "${it}日" },
            dState.value - 1, Modifier.weight(1f)) { dState.value = it + 1 }
    }

    LaunchedEffect(yState.value, mState.value, dState.value) {
        if (!ready.value) {
            ready.value = true
            return@LaunchedEffect
        }
        onSelected(yState.value, mState.value, dState.value)
    }
}

@Composable
private fun DateWheelColumn(
    title: String,
    labels: List<String>,
    initialIndex: Int,
    modifier: Modifier,
    onSelected: (Int) -> Unit
) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        NumberWheelColumn(labels, initialIndex, Modifier.fillMaxWidth(), onSelected)
    }
}

/**
 * 单列滚轮（轻记 NumberWheelColumn 真值移植）。
 * 前后补 2 空格 → 首尾项也能滚到中心；中心项 = firstVisibleItemIndex。
 */
@Composable
private fun NumberWheelColumn(
    labels: List<String>,
    initialIndex: Int,
    modifier: Modifier = Modifier,
    onSelected: (Int) -> Unit
) {
    val itemHeight = 40.dp
    val pad = 2
    val lastIndex = (labels.size - 1).coerceAtLeast(0)
    val state = rememberLazyListState(
        initialFirstVisibleItemIndex = initialIndex.coerceIn(0, lastIndex)
    )
    val centerIndex by remember {
        derivedStateOf { state.firstVisibleItemIndex.coerceIn(0, lastIndex) }
    }

    LaunchedEffect(state.isScrollInProgress) {
        if (!state.isScrollInProgress) {
            val target = state.firstVisibleItemIndex.coerceIn(0, lastIndex)
            state.animateScrollToItem(target)
            onSelected(target)
        }
    }

    Box(
        modifier = modifier
            .height(itemHeight * (pad * 2 + 1))
            .clip(RoundedCornerShape(12.dp))
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .height(itemHeight)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer)
        )
        LazyColumn(
            state = state,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            items(count = pad) { Spacer(Modifier.fillMaxWidth().height(itemHeight)) }
            items(count = labels.size) { index ->
                val isCenter = index == centerIndex
                Box(Modifier.fillMaxWidth().height(itemHeight), contentAlignment = Alignment.Center) {
                    Text(
                        labels[index],
                        fontSize = if (isCenter) 17.sp else 14.sp,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        color = if (isCenter) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center, maxLines = 1
                    )
                }
            }
            items(count = pad) { Spacer(Modifier.fillMaxWidth().height(itemHeight)) }
        }
    }
}
