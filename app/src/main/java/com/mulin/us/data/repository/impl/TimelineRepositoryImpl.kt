package com.mulin.us.data.repository.impl

import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.TimelineEntry
import com.mulin.us.domain.model.TimelineFilter
import com.mulin.us.domain.model.TimelineSource
import com.mulin.us.domain.repository.DiaryRepository
import com.mulin.us.domain.repository.MeetingPlanRepository
import com.mulin.us.domain.repository.MemoryRepository
import com.mulin.us.domain.repository.TimelineRepository
import com.mulin.us.domain.repository.WishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 时间线聚合实现（工单 V3.1 B2/B5/B6）。
 *
 * 权限过滤在**聚合层**集中做，不靠 UI 隐藏（工单第八章）：
 * - 私密日记：只取 SHARED
 * - 未完成的愿望：排除
 * - 悄悄话：根本不作为一个源接入（工单 B2 默认排除）
 * - 未揭晓问答 / 未解锁未来信：Question/Letter Repository 无本地 Flow，
 *   服务端已控制不下发正文；阶段 C 接入时同样只取 revealed/UNLOCKED
 *
 * 排序一律按 eventAt（日期语义），createdAt/updatedAt 只做展示元数据。
 */
class TimelineRepositoryImpl(
    private val memoryRepository: MemoryRepository,
    private val diaryRepository: DiaryRepository,
    private val meetingRepository: MeetingPlanRepository,
    private val wishRepository: WishRepository
) : TimelineRepository {

    private val all: Flow<List<TimelineEntry>> = combine(
        memoryRepository.observeAll(),
        diaryRepository.observeAll(),
        meetingRepository.observeAll(),
        wishRepository.observeAll()
    ) { memories, diaries, meetings, wishes ->
        buildList {
            // 共同回忆（含补录历史：happenedAt 为 null 时用 createdAt）
            memories.filter { it.deletedAt == null }.forEach { m ->
                add(TimelineEntry(
                    id = "memory-${m.localId}",
                    sourceType = TimelineSource.MEMORY,
                    sourceLocalId = m.localId,
                    eventAt = m.happenedAt?.toLocalDate() ?: m.createdAt.toLocalDate(),
                    createdAt = m.createdAt,
                    updatedAt = m.updatedAt,
                    title = m.title.ifBlank { "那一天" },
                    summary = m.content,
                    badge = m.mood
                ))
            }
            // 已发布共同日记：PRIVATE 一律排除（工单 B2 / 第八章）
            diaries.filter { it.deletedAt == null && it.visibility == DiaryVisibility.SHARED }
                .forEach { d ->
                    add(TimelineEntry(
                        id = "diary-${d.localId}",
                        sourceType = TimelineSource.DIARY,
                        sourceLocalId = d.localId,
                        eventAt = LocalDate.ofEpochDay(d.entryEpochDay),
                        createdAt = d.createdAt,
                        updatedAt = d.updatedAt,
                        title = d.title?.takeIf { it.isNotBlank() } ?: "共同日记",
                        summary = d.content,
                        badge = d.mood
                    ))
                }
            // 见面记录：已保存（未软删）的全部纳入；doneAt 不排除（历史见面也是时间线）
            meetings.filter { it.deletedAt == null }.forEach { mt ->
                add(TimelineEntry(
                    id = "meeting-${mt.localId}",
                    sourceType = TimelineSource.MEETING,
                    sourceLocalId = mt.localId,
                    eventAt = mt.plannedAt.toLocalDate(),
                    createdAt = mt.createdAt,
                    updatedAt = mt.createdAt,
                    title = "见面",
                    summary = listOfNotNull(mt.place, mt.note).joinToString(" · ").ifBlank { "一次见面" },
                    badge = mt.place
                ))
            }
            // 愿望：仅已完成的共同愿望
            wishes.filter { it.deletedAt == null && it.doneAt != null }.forEach { w ->
                add(TimelineEntry(
                    id = "wish-${w.localId}",
                    sourceType = TimelineSource.WISH,
                    sourceLocalId = w.localId,
                    eventAt = (w.doneAt ?: w.createdAt).toLocalDate(),
                    createdAt = w.createdAt,
                    updatedAt = w.updatedAt,
                    title = w.title,
                    summary = w.note ?: "愿望已完成",
                    badge = "已完成"
                ))
            }
        }.sortedWith(compareByDescending<TimelineEntry> { it.eventAt }
            .thenByDescending { it.createdAt })
    }

    override fun observeAll(): Flow<List<TimelineEntry>> = all

    override fun observePage(offset: Int, limit: Int): Flow<List<TimelineEntry>> = all.map { list ->
        if (offset <= 0) list.take(limit) else list.drop(offset).take(limit)
    }

    override fun observeFiltered(
        year: Int?,
        month: Int?,
        filter: TimelineFilter
    ): Flow<List<TimelineEntry>> = all.map { list ->
        list.filter { e ->
            val dateOk = (year == null || e.eventAt.year == year) &&
                (month == null || e.eventAt.monthValue == month)
            val typeOk = filter == TimelineFilter.ALL || e.sourceType.filterGroup == filter
            dateOk && typeOk
        }
    }

    override fun observeRecent(limit: Int): Flow<List<TimelineEntry>> = all.map { it.take(limit) }

    override fun observeCountBySource(): Flow<Map<TimelineSource, Int>> = all.map { list ->
        list.groupingBy { it.sourceType }.eachCount()
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()
}
