package com.mulin.us.data.repository.impl

import com.mulin.us.domain.model.DiaryVisibility
import com.mulin.us.domain.model.TimelineEntry
import com.mulin.us.domain.model.TimelineFilter
import com.mulin.us.domain.model.TimelineSource
import com.mulin.us.domain.repository.CoupleRepository
import com.mulin.us.domain.repository.DiaryRepository
import com.mulin.us.domain.repository.LetterRepository
import com.mulin.us.domain.repository.MeetingPlanRepository
import com.mulin.us.domain.repository.MemoryPhotoRepository
import com.mulin.us.domain.repository.MemoryRepository
import com.mulin.us.domain.repository.QuestionRepository
import com.mulin.us.domain.repository.TimelineRepository
import com.mulin.us.domain.repository.WishRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * 时间线聚合实现（工单 V3.1 B2/B5/B6 + 最终验收第四节 4 项收口）。
 *
 * 接入 8 类来源：回忆 / 共同日记 / 见面 / 完成愿望 / 已揭晓问答 / 已解锁未来信 /
 * 纪念日（由 startDate 派生） / 回忆照片（作为 imagePath 附属到回忆条目）。
 *
 * 权限过滤在**聚合层**集中做，不靠 UI 隐藏（工单第八章）：
 * - 私密日记：只取 SHARED
 * - 未完成的愿望：排除
 * - 未揭晓问答 / 未解锁未来信：Repository 的 Flow 在 SQL 层过滤；这里再断言一次
 * - 悄悄话：根本不作为一个源接入（工单 B2 默认排除）
 *
 * 排序一律按 eventAt（日期语义），createdAt/updatedAt 只做展示元数据。
 */
class TimelineRepositoryImpl(
    private val memoryRepository: MemoryRepository,
    private val diaryRepository: DiaryRepository,
    private val meetingRepository: MeetingPlanRepository,
    private val wishRepository: WishRepository,
    private val questionRepository: QuestionRepository,
    private val letterRepository: LetterRepository,
    private val coupleRepository: CoupleRepository,
    private val memoryPhotoRepository: MemoryPhotoRepository
) : TimelineRepository {

    /** 四类本地业务源：先聚成基础条目，照片随后再贴上去 */
    private val baseEntries: Flow<List<TimelineEntry>> = combine(
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
            // 见面记录：已保存（未软删）的全部纳入；历史见面也是时间线
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
        }
    }

    /** 问答：只接已揭晓且确实拿到对方答案的；正文只做摘要，不泄露隐私细节 */
    private val questionEntries: Flow<List<TimelineEntry>> =
        questionRepository.observeRevealed().map { list ->
            list.filter { it.revealed && !it.partnerAnswer.isNullOrBlank() }.map { q ->
                TimelineEntry(
                    id = "question-${q.localId}-${q.questionEpochDay}",
                    sourceType = TimelineSource.QUESTION,
                    sourceLocalId = q.localId,
                    eventAt = LocalDate.ofEpochDay(q.questionEpochDay),
                    createdAt = q.createdAt,
                    updatedAt = q.updatedAt,
                    title = "每日问答 · ${q.content.take(20)}",
                    summary = listOfNotNull(
                        q.myAnswer?.takeIf { it.isNotBlank() }?.let { "我：$it" },
                        q.partnerAnswer?.let { "TA：$it" }
                    ).joinToString(" / ").take(80),
                    badge = "已揭晓"
                )
            }
        }

    /** 未来信：只接 UNLOCKED，且绝不把信件正文带进时间线 */
    private val letterEntries: Flow<List<TimelineEntry>> =
        letterRepository.observeUnlocked().map { list ->
            list.filter { it.status.name == "UNLOCKED" }.map { letter ->
                TimelineEntry(
                    id = "letter-${letter.localId}-${letter.cloudId.orEmpty()}",
                    sourceType = TimelineSource.LETTER,
                    sourceLocalId = letter.localId,
                    eventAt = letter.unlockAt.toLocalDate(),
                    createdAt = letter.createdAt,
                    updatedAt = letter.createdAt,
                    title = letter.title.ifBlank { "未来的信" },
                    summary = "这封信已经解锁，去信箱读完整内容。",
                    badge = "已解锁"
                )
            }
        }

    /** 纪念日：从权威 startDate 派生，不新建业务表 */
    private val anniversaryEntries: Flow<List<TimelineEntry>> =
        coupleRepository.couple.map { couple ->
            val start = couple?.startDate ?: return@map emptyList()
            val today = LocalDate.now()
            val dates = mutableListOf<Pair<LocalDate, Int>>()

            // 历史周年：最多回溯 10 年，避免老账号一次生成上百条
            val fromYear = maxOf(start.year + 1, today.year - 9)
            for (year in fromYear..today.year) {
                val date = anniversaryIn(start, year) ?: continue
                if (date.isAfter(start) && !date.isAfter(today)) dates += date to (year - start.year)
            }
            // 下一次周年（未到日期时给一条"即将到来"，首页最近记录会过滤未来日期）
            val nextYear = today.year + 1
            anniversaryIn(start, nextYear)?.let { dates += it to (nextYear - start.year) }

            dates.distinctBy { it.first }.map { (date, years) ->
                val upcoming = date.isAfter(today)
                TimelineEntry(
                    id = "anniversary-$date",
                    sourceType = TimelineSource.ANNIVERSARY,
                    sourceLocalId = date.toEpochDay(),
                    eventAt = date,
                    createdAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    updatedAt = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    title = if (years == 1) "一周年纪念日" else "$years 周年纪念日",
                    summary = if (upcoming) "距离下一次纪念日还有 ${java.time.temporal.ChronoUnit.DAYS.between(today, date)} 天"
                    else "相恋第 $years 年的这一天",
                    badge = if (upcoming) "即将到来" else "纪念日"
                )
            }
        }

    /** 统一聚合 + 照片大图位：N 条回忆只查一次照片表 */
    private val all: Flow<List<TimelineEntry>> = combine(
        combine(baseEntries, questionEntries, letterEntries, anniversaryEntries) { base, questions, letters, anniversaries ->
            buildList {
                addAll(base)
                addAll(questions)
                addAll(letters)
                addAll(anniversaries)
            }.sortedWith(compareByDescending<TimelineEntry> { it.eventAt }
                .thenByDescending { it.createdAt })
        },
        memoryPhotoRepository.observeAll()
    ) { entries, photos ->
        val photoByMemory = photos.groupBy { it.memoryLocalId }
            .mapValues { (_, list) -> list.firstOrNull { File(it.localPath).exists() }?.localPath }
        entries.map { entry ->
            if (entry.sourceType == TimelineSource.MEMORY) {
                entry.copy(imagePath = photoByMemory[entry.sourceLocalId])
            } else entry
        }
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

    /** 首页「最近发生的事」只看已经发生的，避免未来纪念日排在最前 */
    override fun observeRecent(limit: Int): Flow<List<TimelineEntry>> = all.map { list ->
        val today = LocalDate.now()
        list.filter { !it.eventAt.isAfter(today) }.take(limit)
    }

    override fun observeCountBySource(): Flow<Map<TimelineSource, Int>> = all.map { list ->
        list.groupingBy { it.sourceType }.eachCount()
    }

    private fun Long.toLocalDate(): LocalDate =
        Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDate()

    /** 闰年 2/29 在非闰年没有对应日期，直接跳过该年 */
    private fun anniversaryIn(start: LocalDate, year: Int): LocalDate? =
        runCatching { LocalDate.of(year, start.monthValue, start.dayOfMonth) }.getOrNull()
}
