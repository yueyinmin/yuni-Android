package com.mulin.us.ui.components

import com.mulin.us.domain.model.SyncStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 界面用的纯格式化工具与状态文案。
 *
 * 为什么从各页面抽出来：
 * 这些是"多处都要用、内容必须一致"的东西。原先 formatTime 写在 HomeScreen 里、
 * shortLabel 写在 MemoryScreen 里，别的页面要用就得反向依赖某个页面 —— 那是错的依赖方向。
 * 放进 components 之后，谁都能用，且改一处全局一致。
 */

/** 时间戳 → "MM-dd HH:mm:ss"；0 或负数显示 "-"（表示从未发生） */
fun formatTime(millis: Long): String {
    if (millis <= 0) return "-"
    val fmt = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())
    return fmt.format(Date(millis))
}

/** 日期戳 → "yyyy年M月d日"，用于日记、回忆这类按天展示的内容 */
fun formatEpochDay(epochDay: Long): String {
    return try {
        val date = java.time.LocalDate.ofEpochDay(epochDay)
        "${date.year}年${date.monthValue}月${date.dayOfMonth}日"
    } catch (e: Exception) {
        "-"
    }
}

/**
 * 同步状态的中文短标。
 *
 * 六个状态必须各有各的说法，不能合并成"已同步/未同步"两档 ——
 * 用户看到"仅本机"和"待上传"要知道区别：前者永远不会上传，后者等一下就好。
 */
fun SyncStatus.shortLabel(): String = when (this) {
    SyncStatus.LOCAL_ONLY -> "仅本机"
    SyncStatus.PENDING_UPLOAD -> "待上传"
    SyncStatus.SYNCED -> "已同步"
    SyncStatus.PENDING_DELETE -> "待删除"
    SyncStatus.CONFLICT -> "冲突"
    SyncStatus.FAILED -> "失败"
}
