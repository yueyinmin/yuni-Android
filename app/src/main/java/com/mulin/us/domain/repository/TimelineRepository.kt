package com.mulin.us.domain.repository

import com.mulin.us.domain.model.TimelineEntry
import com.mulin.us.domain.model.TimelineFilter
import com.mulin.us.domain.model.TimelineSource
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

/**
 * 时间线聚合门面（工单 V3.1 B6）。
 *
 * 唯一数据来源：多业务 Repository 的 Flow 聚合，**不建时间线主表**。
 * 首页"最近发生的事"和回忆页完整时间线都必须走这里，保证新增/编辑/删除两处一致。
 */
interface TimelineRepository {
    /** 完整时间线（按 eventAt 倒序），已按权限过滤 */
    fun observeAll(): Flow<List<TimelineEntry>>

    /**
     * 分页读取。
     * @param offset 跳过条数 @param limit 本页条数
     * 注意：分页是在内存聚合后切片，源数据变更会重新发整流（阶段 B 的合理实现）。
     */
    fun observePage(offset: Int, limit: Int): Flow<List<TimelineEntry>>

    /** 按年份 + 月份 + 类型筛选；null = 不过滤 */
    fun observeFiltered(
        year: Int? = null,
        month: Int? = null,
        filter: TimelineFilter = TimelineFilter.ALL
    ): Flow<List<TimelineEntry>>

    /** 首页用：最近 N 条（默认 3） */
    fun observeRecent(limit: Int = 3): Flow<List<TimelineEntry>>

    /** 某来源的条目数（自检用） */
    fun observeCountBySource(): Flow<Map<TimelineSource, Int>>
}
