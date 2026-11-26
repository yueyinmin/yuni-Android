package com.mulin.us.domain.model

import java.time.LocalDate

/**
 * 时间线统一展示模型（工单 V3.1 B6）。
 *
 * 设计约束：
 * - 这是**只读投影**，不是新的业务表，也不复制原始数据长期落库；
 * - 每条只保留 [sourceType] + [sourceLocalId] 关联回原业务记录；
 * - [eventAt] 是事件发生时间（排序依据），[createdAt]/[updatedAt] 是记录元数据；
 * - 补录历史记录时 eventAt 在 past，createdAt 是现在 —— 排序只看 eventAt。
 */
data class TimelineEntry(
    val id: String,
    val sourceType: TimelineSource,
    val sourceLocalId: Long,
    /** 事件发生时间（日期语义，避免时区偏移） */
    val eventAt: LocalDate,
    /** 记录创建时间（毫秒） */
    val createdAt: Long,
    /** 最后修改时间（毫秒） */
    val updatedAt: Long,
    val title: String,
    val summary: String,
    /** 大图卡片的本地图片路径；紧凑文字卡为 null */
    val imagePath: String? = null,
    /** 卡片附属信息：地点 / 评分 / 完成状态等 */
    val badge: String? = null
)

/** 时间线来源类型。筛选栏直接对应这些枚举（工单 B4） */
enum class TimelineSource {
    MEMORY,             // 共同回忆
    DIARY,              // 已发布共同日记
    MEETING,            // 见面记录
    ANNIVERSARY,        // 纪念日（阶段 D 由 startDate 计算）
    QUESTION,           // 双方提交并已揭晓的问答
    WISH,               // 已完成的共同愿望
    LETTER;             // 已解锁且允许共同展示的未来信

    /** 顶部筛选分组：全部/照片/日记/见面/纪念日/互动/其他 */
    val filterGroup: TimelineFilter get() = when (this) {
        MEMORY -> TimelineFilter.PHOTO
        DIARY -> TimelineFilter.DIARY
        MEETING -> TimelineFilter.MEETING
        ANNIVERSARY -> TimelineFilter.ANNIVERSARY
        QUESTION, LETTER -> TimelineFilter.INTERACTION
        WISH -> TimelineFilter.OTHER
    }
}

enum class TimelineFilter {
    ALL, PHOTO, DIARY, MEETING, ANNIVERSARY, INTERACTION, OTHER
}
